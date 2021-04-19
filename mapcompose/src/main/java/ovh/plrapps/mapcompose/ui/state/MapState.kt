package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ovh.plrapps.mapcompose.core.TileOptionsProvider
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapview.compose.ui.state.ZoomPanRotateState

class MapState(
    levelCount: Int,
    fullWidth: Int,
    fullHeight: Int,
    tileStreamProvider: TileStreamProvider,
    tileSize: Int = 256,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    internal val zoomPanRotateState = ZoomPanRotateState(fullWidth, fullHeight)
    internal val tileCanvasState = TileCanvasState(
        scope,
        tileSize,
        VisibleTilesResolver(levelCount, fullWidth, fullHeight, tileSize),
        tileStreamProvider,
        object : TileOptionsProvider {
        },
        workerCount = Runtime.getRuntime().availableProcessors() - 1,
        highFidelityColors = true
    )

    internal val childComposables = mutableStateMapOf<Int, @Composable () -> Unit>()

    @Suppress("unused")
    fun addComposable(id: Int, c: @Composable () -> Unit) {
        childComposables[id] = c
    }

    @Suppress("unused")
    fun removeComposable(id: Int): Boolean {
        return childComposables.remove(id) != null
    }
}