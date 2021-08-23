package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.utils.toRad
import ovh.plrapps.mapcompose.core.throttle

/**
 * The state of the map. All public APIs are extensions functions or extension properties of this
 * class.
 *
 * @param levelCount The number of levels in the pyramid.
 * @param fullWidth The width in pixels of the map at scale 1f.
 * @param fullHeight The height in pixels of the map at scale 1f.
 * @param tileSize The size in pixels of tiles, which are expected to be squared. Defaults to 256.
 * @param workerCount The thread count used to fetch tiles. Defaults to the number of cores minus
 * one, which works well for tiles in the file system or in a local database. However, that number
 * should be increased to 16 or more for remote tiles (HTTP requests).
 */
class MapState(
    levelCount: Int,
    fullWidth: Int,
    fullHeight: Int,
    tileStreamProvider: TileStreamProvider,
    tileSize: Int = 256,
    workerCount: Int = Runtime.getRuntime().availableProcessors() - 1,
    magnifyingFactor: Int = 0
) : ZoomPanRotateStateListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal val zoomPanRotateState = ZoomPanRotateState(fullWidth, fullHeight, this)
    internal val markerState = MarkerState()
    internal val pathState = PathState()
    internal val visibleTilesResolver =
        VisibleTilesResolver(levelCount, fullWidth, fullHeight, tileSize, magnifyingFactor) {
            zoomPanRotateState.scale
        }
    internal val tileCanvasState = TileCanvasState(
        scope,
        tileSize,
        visibleTilesResolver,
        tileStreamProvider,
        workerCount,
        highFidelityColors = true
    )

    private val throttledTask: SendChannel<Unit> = scope.throttle(wait = 18) {
        renderVisibleTiles()
    }
    private val viewport = Viewport()
    private var padding: Int = 0
    internal val tileSize by mutableStateOf(tileSize)
    internal var stateChangeListener: (MapState.() -> Unit)? = null
    internal var touchDownCb: (() -> Unit)? = null

    /**
     * Cancels all internal tasks.
     * After this call, this [MapState] is unusable.
     */
    fun shutdown() {
        scope.cancel()
        tileCanvasState.shutdown()
    }

    /**
     * Public API to programmatically trigger a redraw of the tiles.
     */
    @Suppress("unused")
    fun redrawTiles() {
        tileCanvasState.clearVisibleTiles().invokeOnCompletion {
            scope.launch {
                renderVisibleTiles()
            }
        }
    }

    override fun onStateChanged() {
        renderVisibleTilesThrottled()
        stateChangeListener?.invoke(this)
    }

    override fun onTouchDown() {
        markerState.removeAllAutoDismissCallouts()
        touchDownCb?.invoke()
    }

    private fun renderVisibleTilesThrottled() {
        throttledTask.trySend(Unit)
    }

    private suspend fun renderVisibleTiles() {
        val viewport = updateViewport()
        tileCanvasState.setViewport(viewport)
    }

    private fun updateViewport(): Viewport {
        val padding = padding
        return viewport.apply {
            left = zoomPanRotateState.scrollX.toInt() - padding - zoomPanRotateState.padding.x
            top = zoomPanRotateState.scrollY.toInt() - padding - zoomPanRotateState.padding.y
            right = left + zoomPanRotateState.layoutSize.width + padding * 2
            bottom = top + zoomPanRotateState.layoutSize.height + padding * 2
            angleRad = zoomPanRotateState.rotation.toRad()
        }
    }
}