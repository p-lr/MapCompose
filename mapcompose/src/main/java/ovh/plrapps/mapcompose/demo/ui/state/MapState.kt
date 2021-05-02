package ovh.plrapps.mapcompose.demo.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.utils.toRad
import ovh.plrapps.mapview.core.throttle

class MapState(
    levelCount: Int,
    fullWidth: Int,
    fullHeight: Int,
    tileStreamProvider: TileStreamProvider,
    tileSize: Int = 256,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ZoomPanRotateStateListener {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    internal val zoomPanRotateState = ZoomPanRotateState(fullWidth, fullHeight, this)
    internal val markerState = MarkerState()
    internal val pathState = PathState()
    internal val visibleTilesResolver =
        VisibleTilesResolver(levelCount, fullWidth, fullHeight, tileSize) {
            zoomPanRotateState.scale
        }
    internal val tileCanvasState = TileCanvasState(
        scope,
        tileSize,
        visibleTilesResolver,
        tileStreamProvider,
        workerCount = Runtime.getRuntime().availableProcessors() - 1,
        highFidelityColors = true
    )

    private val throttledTask: SendChannel<Unit> = scope.throttle(wait = 18) {
        renderVisibleTiles()
    }
    private val viewport = Viewport()
    private var padding: Int = 0
    internal val tileSize by mutableStateOf(tileSize)

    /**
     * Public API to programmatically trigger a redraw of the tiles.
     */
    @Suppress("unused")
    fun redrawTiles() {
        tileCanvasState.clearVisibleTiles().invokeOnCompletion {
            renderVisibleTiles()
        }
    }

    override fun onStateChanged() {
        renderVisibleTilesThrottled()
    }

    private fun renderVisibleTilesThrottled() {
        throttledTask.offer(Unit)
    }

    private fun renderVisibleTiles() {
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