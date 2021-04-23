package ovh.plrapps.mapcompose.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layoutId
import ovh.plrapps.mapcompose.ui.layout.ZoomPanRotate
import ovh.plrapps.mapcompose.ui.markers.MarkerLayout
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.view.TileCanvas

@Composable
fun MapUI(
    modifier: Modifier = Modifier,
    state: MapState
) {
    val zoomPRState = state.zoomPanRotateState
    val markerState = state.markerState

    ZoomPanRotate(
        modifier = modifier.clipToBounds(),
        gestureListener = zoomPRState,
        layoutSizeChangeListener = zoomPRState,
        paddingX = zoomPRState.paddingX,
        paddingY = zoomPRState.paddingY,
    ) {
        TileCanvas(
            modifier = Modifier,
            zoomPRState = zoomPRState,
            visibleTilesResolver = state.visibleTilesResolver,
            tileSize = state.tileSize,
            alphaTick = state.tileCanvasState.alphaTick,
            colorFilterProvider = state.tileCanvasState.colorFilterProvider,
            tilesToRender = state.tileCanvasState.tilesToRender
        )

        MarkerLayout(
            modifier = Modifier,
            zoomPRState = zoomPRState,
        ) {
            for (data in markerState.markers.values) {
                Surface(Modifier.layoutId(data)) {
                    data.c()
                }
            }
        }

        for (c in state.childComposables.values) {
            c()
        }
    }
}