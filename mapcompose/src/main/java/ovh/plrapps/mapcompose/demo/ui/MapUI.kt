package ovh.plrapps.mapcompose.demo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import ovh.plrapps.mapcompose.api.moveMarkerBy
import ovh.plrapps.mapcompose.demo.ui.layout.ZoomPanRotate
import ovh.plrapps.mapcompose.demo.ui.markers.MarkerLayout
import ovh.plrapps.mapcompose.demo.ui.state.MapState
import ovh.plrapps.mapcompose.demo.ui.view.TileCanvas

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
        padding = zoomPRState.padding,
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
                Surface(Modifier
                    .layoutId(data)
                    .clip(CircleShape)
                    .clickable(
                        onClick = { markerState.onMarkerClick(data) },
                    )
                    .then(
                        if (data.isDraggable) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consumeAllChanges()
                                    state.moveMarkerBy(data.id, dragAmount)
                                }
                            }
                        } else Modifier
                    ),
                    color = Color.Transparent
                ) {
                    data.c()
                }
            }
        }

        for (c in state.childComposables.values) {
            c()
        }
    }
}