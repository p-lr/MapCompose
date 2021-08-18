package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import ovh.plrapps.mapcompose.api.moveMarkerBy
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.MarkerData
import ovh.plrapps.mapcompose.ui.state.MarkerState
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import ovh.plrapps.mapcompose.utils.rotateX
import ovh.plrapps.mapcompose.utils.rotateY
import ovh.plrapps.mapcompose.utils.toRad

@Composable
internal fun MarkerComposer(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    markerState: MarkerState,
    mapState: MapState
) {
    MarkerLayout(
        modifier = modifier,
        zoomPRState = zoomPRState,
    ) {
        for (data in markerState.markers.values) {
            Box(
                Modifier
                    .layoutId(data)
                    .clip(CircleShape)
                    .clickable {
                        markerState.onMarkerClick(data)
                    }
                    .then(
                        if (data.isDraggable) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consumeAllChanges()
                                    val interceptor = data.dragInterceptor
                                    if (interceptor != null) {
                                        invokeDragInterceptor(data, zoomPRState, dragAmount)
                                    } else {
                                        mapState.moveMarkerBy(data.id, dragAmount)
                                    }
                                }
                            }
                        } else Modifier
                    )
            ) {
                data.c()
            }
        }
        for (data in markerState.callouts.values) {
            Box(
                Modifier
                    .layoutId(data.markerData)
                    .clickable {
                        /* This click listener will be invoked only if the child composable doesn't
                         * consume clicks */
                        markerState.onCalloutClick(data.markerData)
                    },
            ) {
                data.markerData.c()
            }
        }
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

    val deltaX = (data.x + dx / (zoomPRState.fullWidth * zoomPRState.scale)).coerceIn(0.0, 1.0) - data.x
    val deltaY = (data.y + dy / (zoomPRState.fullHeight * zoomPRState.scale)).coerceIn(0.0, 1.0) - data.y

    with(data) {
        dragInterceptor?.invoke(
            id, x, y,
            deltaX,
            deltaY
        )
    }
}