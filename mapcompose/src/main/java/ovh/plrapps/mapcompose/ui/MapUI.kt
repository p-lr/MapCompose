package ovh.plrapps.mapcompose.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import ovh.plrapps.mapcompose.ui.layout.ZoomPanRotate
import ovh.plrapps.mapcompose.ui.markers.MarkerComposer
import ovh.plrapps.mapcompose.ui.paths.PathComposer
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.view.TileCanvas

@Composable
fun MapUI(
    modifier: Modifier = Modifier,
    state: MapState,
    content: @Composable () -> Unit = {}
) {
    val zoomPRState = state.zoomPanRotateState
    val markerState = state.markerRenderState
    val pathState = state.pathState

    key(state) {
        ZoomPanRotate(
            modifier = modifier
                .clipToBounds()
                .background(state.mapBackground),
            gestureListener = zoomPRState,
            layoutSizeChangeListener = zoomPRState,
        ) {
            TileCanvas(
                modifier = Modifier,
                zoomPRState = zoomPRState,
                visibleTilesResolver = state.visibleTilesResolver,
                tileSize = state.tileSize,
                alphaTick = state.tileCanvasState.alphaTick,
                colorFilterProvider = state.tileCanvasState.colorFilterProvider,
                tilesToRender = state.tileCanvasState.tilesToRender,
                isFilteringBitmap = state.isFilteringBitmap,
            )

            MarkerComposer(
                modifier = Modifier.zIndex(1f),
                zoomPRState = zoomPRState,
                markerRenderState = markerState,
                mapState = state
            )

            PathComposer(
                modifier = Modifier,
                zoomPRState = zoomPRState,
                pathState = pathState
            )

            content()
        }
    }
}