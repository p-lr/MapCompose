package ovh.plrapps.mapcompose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.zIndex
import ovh.plrapps.mapcompose.api.moveMarkerBy
import ovh.plrapps.mapcompose.ui.layout.ZoomPanRotate
import ovh.plrapps.mapcompose.ui.markers.MarkerLayout
import ovh.plrapps.mapcompose.ui.paths.PathComposer
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.MarkerData
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import ovh.plrapps.mapcompose.ui.view.TileCanvas
import ovh.plrapps.mapcompose.utils.rotateX
import ovh.plrapps.mapcompose.utils.rotateY
import ovh.plrapps.mapcompose.utils.toRad

@Composable
fun MapUI(
    modifier: Modifier = Modifier,
    state: MapState,
    content: @Composable () -> Unit = {}
) {
    val zoomPRState = state.zoomPanRotateState
    val markerState = state.markerState
    val pathState = state.pathState

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
            modifier = Modifier.zIndex(1f),
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
                                    val interceptor = data.dragInterceptor
                                    if (interceptor != null) {
                                        invokeDragInterceptor(data, zoomPRState, dragAmount)
                                    } else {
                                        state.moveMarkerBy(data.id, dragAmount)
                                    }
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

        PathComposer(
            modifier = Modifier,
            zoomPRState = zoomPRState,
            pathState = pathState
        )

        content()
    }
}

private fun invokeDragInterceptor(
    data: MarkerData,
    zoomPRState: ZoomPanRotateState,
    deltaPx: Offset
) {
    val angle = -zoomPRState.rotation.toRad()
    val dx = rotateX(deltaPx.x.toDouble(), deltaPx.y.toDouble(), angle)
    val dy = rotateY(deltaPx.x.toDouble(), deltaPx.y.toDouble(), angle)
    with(data) {
        dragInterceptor?.invoke(
            id, x, y,
            dx / (zoomPRState.fullWidth * zoomPRState.scale),
            dy / (zoomPRState.fullHeight * zoomPRState.scale)
        )
    }
}