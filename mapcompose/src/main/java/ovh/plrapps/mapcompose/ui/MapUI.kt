package ovh.plrapps.mapcompose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.ui.layout.ZoomPanRotate
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.view.TileCanvas

@Composable
fun MapUI(
    modifier: Modifier = Modifier,
    state: MapState
) {
    val zoomPRState = state.zoomPanRotateState

    ZoomPanRotate(
        modifier = modifier,
        gestureListener = zoomPRState,
        layoutSizeChangeListener = zoomPRState,
        paddingX = zoomPRState.paddingX,
        paddingY = zoomPRState.paddingY,
    ) {
        TileCanvas(
            modifier = modifier,
            zoomPRState = zoomPRState,
            visibleTilesResolver = state.visibleTilesResolver,
            tileSize = state.tileSize,
            alphaTick = state.tileCanvasState.alphaTick,
            tilesToRender = state.tileCanvasState.tilesToRender
        )

        for (c in state.childComposables.values) {
            c()
        }
    }
}