package ovh.plrapps.mapcompose.ui.state

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.core.*
import java.util.concurrent.Executors
import kotlin.math.pow

/**
 * This class contains all the logic related to [Tile] management.
 * It defers [Tile] loading to the [TileCollector].
 * All internal data manipulation are thread-confined to a single background thread. This is
 * guarantied by the [scope] and its custom dispatcher.
 * Ultimately, it exposes the list of tiles to render ([tilesToRender]) which is backed by a
 * [MutableState]. A composable using [tilesToRender] will be automatically recomposed when this
 * list changes.
 *
 * @author P.Laurence on 04/06/2019
 */
internal class TileCanvasState(
    parentScope: CoroutineScope, tileSize: Int,
    private val visibleTilesResolver: VisibleTilesResolver,
    workerCount: Int, highFidelityColors: Boolean
) {

    /* This view-model uses a background thread for its computations */
    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(
        parentScope.coroutineContext + singleThreadDispatcher
    )
    internal var tilesToRender: List<Tile> by mutableStateOf(listOf())

    private val _layerFlow = MutableStateFlow<List<Layer>>(listOf())
    internal val layerFlow = _layerFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bitmapPool = BitmapPool(Dispatchers.Default.limitedParallelism(1))
    private val visibleTileLocationsChannel = Channel<TileSpec>(capacity = Channel.RENDEZVOUS)
    private val tilesOutput = Channel<Tile>(capacity = Channel.RENDEZVOUS)
    private val visibleStateFlow = MutableStateFlow<VisibleState?>(null)
    internal var alphaTick = 0.07f
        set(value) {
            field = value.coerceIn(0.01f, 1f)
        }
    internal var colorFilterProvider: ColorFilterProvider? by mutableStateOf(null)

    private val bitmapConfig = if (highFidelityColors) {
        BitmapConfiguration(Bitmap.Config.ARGB_8888, 4)
    } else {
        BitmapConfiguration(Bitmap.Config.RGB_565, 2)
    }

    private val lastVisible: VisibleTiles?
        get() = visibleStateFlow.value?.visibleTiles

    /**
     * So long as this debounced channel is offered a message, the lambda isn't called.
     */
    private val idleDebounced = scope.debounce<Unit>(400) {
        visibleStateFlow.value?.also { (visibleTiles, layerIds, opacities) ->
            evictTiles(visibleTiles, layerIds, opacities, aggressiveAttempt = true)
            renderTiles(visibleTiles, layerIds)
        }
    }

    private val renderTask = scope.throttle(wait = 34) {
        /* Evict, then render */
        val (lastVisible, ids, opacities) = visibleStateFlow.value ?: return@throttle
        evictTiles(lastVisible, ids, opacities)

        renderTiles(lastVisible, ids)
    }

    private fun renderTiles(visibleTiles: VisibleTiles, layerIds: List<String>) {
        /* Right before sending tiles to the view, reorder them so that tiles from current level are
         * above others. */
        val tilesToRenderCopy = tilesCollected.sortedBy {
            val priority =
                if (it.zoom == visibleTiles.level && it.subSample == visibleTiles.subSample) 100 else 0
            priority + if (layerIds == it.layerIds) 1 else 0
        }

        tilesToRender = tilesToRenderCopy
    }

    private val tilesCollected = mutableListOf<Tile>()

    private val tileCollector: TileCollector

    init {
        /* Collect visible tiles and send specs to the TileCollector */
        scope.launch {
            collectNewTiles()
        }

        /* Launch the TileCollector */
        tileCollector = TileCollector(workerCount.coerceAtLeast(1), bitmapConfig, tileSize)
        scope.launch {
            _layerFlow.collectLatest { layers ->
                tileCollector.collectTiles(
                    tileSpecs = visibleTileLocationsChannel,
                    tilesOutput = tilesOutput,
                    layers = layers,
                    bitmapPool = bitmapPool
                )
            }
        }

        /* Launch a coroutine to consume the produced tiles */
        scope.launch {
            consumeTiles(tilesOutput)
        }
    }

    fun setLayers(layers: List<Layer>) {
        /* If there's nothing in common with current layers, the canvas will be cleared */
        val clear = layers.intersect(_layerFlow.value.toSet()).isEmpty()
        _layerFlow.value = layers
        if (clear) {
            evictAll()
        }
    }

    /**
     * Forgets visible state and previously collected tiles.
     * To clear the canvas, call [forgetTiles], then [renderThrottled].
     */
    suspend fun forgetTiles() {
        scope.launch {
            visibleStateFlow.value = null
            tilesCollected.clear()
        }.join()
    }

    fun shutdown() {
        singleThreadDispatcher.close()
        tileCollector.shutdownNow()
    }

    suspend fun setViewport(viewport: Viewport) {
        /* Thread-confine the tileResolver to the main thread */
        val visibleTiles = withContext(Dispatchers.Main) {
            visibleTilesResolver.getVisibleTiles(viewport)
        }

        withContext(scope.coroutineContext) {
            setVisibleTiles(visibleTiles)
        }
    }

    private fun setVisibleTiles(visibleTiles: VisibleTiles) {
        /* Feed the tile processing machinery */
        val layerIds = _layerFlow.value.map { it.id }
        val opacities = _layerFlow.value.map { it.alpha }
        val visibleTilesForLayers = VisibleState(visibleTiles, layerIds, opacities)
        visibleStateFlow.value = visibleTilesForLayers

        renderThrottled()
    }

    /**
     * Consumes incoming visible tiles from [visibleStateFlow] and sends [TileSpec] instances to the
     * [TileCollector].
     *
     * Leverage built-in back pressure, as this function will suspend when the tile collector is busy
     * to the point it can't handshake the [visibleTileLocationsChannel] channel.
     *
     * Using [Flow.collectLatest], we cancel any ongoing previous tile list processing. It's
     * particularly useful when the [TileCollector] is too slow, so when a new [VisibleTiles] element
     * is received from [visibleStateFlow], no new [TileSpec] elements from the previous [VisibleTiles]
     * element are sent to the [TileCollector]. When the [TileCollector] is ready to resume processing,
     * the latest [VisibleTiles] element is processed right away.
     */
    private suspend fun collectNewTiles() {
        visibleStateFlow.collectLatest { visibleState ->
            val visibleTiles = visibleState?.visibleTiles
            if (visibleTiles != null) {
                for (e in visibleTiles.tileMatrix) {
                    val row = e.key
                    val colRange = e.value
                    for (col in colRange) {
                        val alreadyProcessed = tilesCollected.any { tile ->
                            tile.sameSpecAs(
                                visibleTiles.level,
                                row,
                                col,
                                visibleTiles.subSample,
                                visibleState.layerIds,
                                visibleState.opacities
                            )
                        }

                        /* Only emit specs which haven't already been processed by the collector
                         * Doing this now results in less object allocations than filtering the flow
                         * afterwards */
                        if (!alreadyProcessed) {
                            visibleTileLocationsChannel.send(
                                TileSpec(
                                    visibleTiles.level,
                                    row,
                                    col,
                                    visibleTiles.subSample
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * For each [Tile] received, add it to the list of collected tiles if it's visible. Otherwise,
     * recycle the tile.
     */
    private suspend fun consumeTiles(tileChannel: ReceiveChannel<Tile>) {
        for (tile in tileChannel) {
            val lastVisible = lastVisible
            if (
                (lastVisible == null || lastVisible.contains(tile))
                && !tilesCollected.contains(tile)
                && tile.layerIds == visibleStateFlow.value?.layerIds
            ) {
                tile.prepare()
                tilesCollected.add(tile)
                renderThrottled()
            } else {
                tile.recycle()
            }
            fullEvictionDebounced()
        }
    }

    private fun fullEvictionDebounced() {
        idleDebounced.trySend(Unit)
    }

    /**
     * The the alpha needs to be set to [alphaTick], to produce a fade-in effect. If [alphaTick] is
     * 1f, the alpha won't be updated and there won't be any fade-in effect.
     */
    private fun Tile.prepare() {
        alpha = alphaTick
    }

    private fun VisibleTiles.contains(tile: Tile): Boolean {
        if (level != tile.zoom) return false
        val colRange = tileMatrix[tile.row] ?: return false
        return subSample == tile.subSample && tile.col in colRange
    }

    private fun VisibleTiles.intersects(tile: Tile): Boolean {
        return if (level == tile.zoom) {
            val colRange = tileMatrix[tile.row] ?: return false
            tile.col in colRange
        } else {
            val curMinRow = tileMatrix.keys.minOrNull() ?: return false
            val curMaxRow = tileMatrix.keys.maxOrNull() ?: return false
            val curMinCol = tileMatrix.entries.firstOrNull()?.value?.first ?: return false
            val curMaxCol = tileMatrix.entries.firstOrNull()?.value?.last ?: return false

            if (tile.zoom > level) { // User is zooming out
                val dLevel = tile.zoom - level
                val minRowAtLvl = curMinRow.minAtGreaterLevel(dLevel)
                val maxRowAtLvl = curMaxRow.maxAtGreaterLevel(dLevel)

                val minColAtLvl = curMinCol.minAtGreaterLevel(dLevel)
                val maxColAtLvl = curMaxCol.maxAtGreaterLevel(dLevel)
                return tile.row in minRowAtLvl..maxRowAtLvl && tile.col in minColAtLvl..maxColAtLvl
            } else { // User is zooming in
                val dLevel = level - tile.zoom
                val minRowAtLvl = tile.row.minAtGreaterLevel(dLevel)
                val maxRowAtLvl = tile.row.maxAtGreaterLevel(dLevel)

                val minColAtLvl = tile.col.minAtGreaterLevel(dLevel)
                val maxColAtLvl = tile.col.maxAtGreaterLevel(dLevel)
                return curMinCol <= maxColAtLvl && minColAtLvl <= curMaxCol && curMinRow <= maxRowAtLvl &&
                        minRowAtLvl <= curMaxRow
            }
        }
    }

    /**
     * Each time we get a new [VisibleTiles], remove all [Tile] from [tilesCollected] which aren't
     * visible or that aren't needed anymore and put their bitmap into the pool.
     */
    private fun evictTiles(
        visibleTiles: VisibleTiles,
        layerIds: List<String>,
        opacities: List<Float>,
        aggressiveAttempt: Boolean = false
    ) {
        val currentLevel = visibleTiles.level
        val currentSubSample = visibleTiles.subSample

        /* Always perform partial eviction */
        partialEviction(visibleTiles, layerIds, opacities)

        /* Only perform aggressive eviction when tile collector is idle */
        if (aggressiveAttempt && tileCollector.isIdle) {
            aggressiveEviction(currentLevel, currentSubSample, layerIds)
        }
    }

    /**
     * Evict:
     * * tiles of levels different than the current one, that aren't visible,
     * * tiles that aren't visible at current level, and tiles from current level which aren't made
     * of current layers
     */
    private fun partialEviction(
        visibleTiles: VisibleTiles,
        layerIds: List<String>,
        opacities: List<Float>
    ) {
        val currentLevel = visibleTiles.level
        val currentSubSample = visibleTiles.subSample

        val iterator = tilesCollected.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()

            if (tile.zoom != currentLevel && !visibleTiles.intersects(tile)) {
                iterator.remove()
                tile.recycle()
                continue
            }

            if (
                tile.zoom == currentLevel
                && tile.subSample == currentSubSample
                && (!visibleTiles.contains(tile) || !shouldKeepTile(tile, layerIds, opacities))
            ) {
                iterator.remove()
                tile.recycle()
            }
        }
    }

    private fun shouldKeepTile(
        tile: Tile,
        layerIds: List<String>,
        opacities: List<Float>
    ): Boolean {
        if (layerIds.isEmpty()) return false
        return if (tile.layerIds != layerIds) {
            layerIds.containsAll(tile.layerIds) || tile.layerIds.containsAll(layerIds)
        } else {
            tile.opacities == opacities
        }
    }

    /**
     * Removes tiles of other levels, even if they are visible (although they should be drawn beneath
     * currently visible tiles).
     * Only triggered after the [idleDebounced] fires.
     */
    private fun aggressiveEviction(
        currentLevel: Int,
        currentSubSample: Int,
        layerIds: List<String>
    ) {
        val iterator = tilesCollected.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()

            /* Remove tiles at the same level but from other layers */
            if (
                tile.zoom == currentLevel
                && tile.subSample == currentSubSample
                && tile.layerIds != layerIds
            ) {
                iterator.remove()
                tile.recycle()
            }

            /* Remove other tiles at different level and sub-sample */
            if ((tile.zoom != currentLevel && tile.subSample == 0)
                || (tile.zoom == 0 && tile.subSample != currentSubSample)
            ) {
                iterator.remove()
                tile.recycle()
            }
        }
    }

    private fun evictAll() = scope.launch {
        val iterator = tilesCollected.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()
            iterator.remove()
            tile.recycle()
        }
    }

    /**
     * Post a new value to the observable. The view should update its UI.
     */
    private fun renderThrottled() {
        renderTask.trySend(Unit)
    }

    /**
     * After a [Tile] is no longer visible, recycle its Bitmap and Paint if possible, for later use.
     */
    private fun Tile.recycle() {
        val b = bitmap ?: return
        if (b.isMutable) {
            bitmapPool.put(b)
        }
        alpha = 0f
    }

    private fun Int.minAtGreaterLevel(n: Int): Int {
        return this * 2.0.pow(n).toInt()
    }

    private fun Int.maxAtGreaterLevel(n: Int): Int {
        return (this + 1) * 2.0.pow(n).toInt() - 1
    }

    private data class VisibleState(
        val visibleTiles: VisibleTiles,
        val layerIds: List<String>,
        val opacities: List<Float>
    )
}
