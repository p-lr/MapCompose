package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import ovh.plrapps.mapcompose.utils.Point
import ovh.plrapps.mapcompose.utils.rotate
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
            /* Optimize re-compositions */
            key(data.id) {
                Box(
                    Modifier
                        .layoutId(data)
                        .clip(CircleShape)
                        .then(
                            if (data.isClickable) {
                                Modifier.clickable {
                                    markerState.onMarkerClick(data)
                                }
                            } else Modifier
                        )
                        .then(
                            if (data.isDraggable) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consumeAllChanges()
                                        val interceptor = data.dragInterceptor
                                        if (interceptor != null) {
                                            invokeDragInterceptor(data, zoomPRState, Point(dragAmount))
                                        } else {
                                            mapState.moveMarkerBy(data.id, Point(dragAmount))
                                        }
                                    }
                                }
                            } else Modifier
                        )
                ) {
                    data.c()
                }
            }
        }
        for (data in markerState.callouts.values) {
            /* Optimize re-compositions */
            key(data.markerData.id) {
                Box(
                    Modifier
                        .layoutId(data.markerData)
                        .then(
                            if (data.markerData.isClickable) {
                                Modifier.clickable {
                                    markerState.onCalloutClick(data.markerData)
                                }
                            } else Modifier
                        )
                ) {
                    data.markerData.c()
                }
            }
        }
    }
}

private fun invokeDragInterceptor(
    data: MarkerData,
    zoomPRState: ZoomPanRotateState,
    deltaPx: Point
) {
    val angle = -zoomPRState.rotation.toRad()
    val deltaRotated = rotate(deltaPx, angle)

    val scale = Point(zoomPRState.fullWidth * zoomPRState.scale, zoomPRState.fullHeight * zoomPRState.scale)
    val delta = (data.position + (deltaRotated / scale)).coerceIn(0f, 1f) - data.position

    with(data) {
        dragInterceptor?.invoke(id, position, delta)
    }
}