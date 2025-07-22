package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import ovh.plrapps.mapcompose.api.moveMarkerBy
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.MarkerRenderState
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import ovh.plrapps.mapcompose.utils.rotateX
import ovh.plrapps.mapcompose.utils.rotateY
import ovh.plrapps.mapcompose.utils.toRad

@Composable
internal fun MarkerComposer(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    markerRenderState: MarkerRenderState,
    mapState: MapState
) {
    MarkerLayout(
        modifier = modifier,
        zoomPRState = zoomPRState,
    ) {
        for (data in markerRenderState.markers.value) {
            /* Optimize re-compositions */
            key(data.uuid) {
                Box(
                    Modifier
                        .layoutId(data)
                        .then(
                            if (data.isDraggable) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            val listener = data.dragStartListener
                                            if (listener != null) {
                                                invokeDragStartListener(data, zoomPRState, it)
                                            }
                                        },
                                        onDragEnd = {
                                            data.dragEndListener?.onDragEnd(data.id, data.x, data.y)
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val interceptor = data.dragInterceptor
                                        if (interceptor != null) {
                                            invokeDragInterceptor(
                                                data,
                                                zoomPRState,
                                                dragAmount,
                                                change.position
                                            )
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
        }
        for (data in markerRenderState.callouts.values) {
            /* Optimize re-compositions */
            key(data.markerData.uuid) {
                Box(
                    Modifier
                        .layoutId(data.markerData)
                        .then(
                            if (data.markerData.isClickable) {
                                /**
                                 * As of 2022/04, using Modifier.clickable causes a huge performance
                                 * drop when the number of callouts exceeds a few dozens.
                                 * Using pointerInput, we loose the ripple effect.
                                 */
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            markerRenderState.onCalloutClick(data.markerData)
                                        }
                                    )
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

private fun invokeDragStartListener(
    data: MarkerData,
    zoomPRState: ZoomPanRotateState,
    position: Offset
) {
    /* Compute the pointer offset */
    val origin = Offset(- data.measuredWidth * data.relativeOffset.x, - data.measuredHeight * data.relativeOffset.y)
    val pointerOffset = position - origin
    val angle = -zoomPRState.rotation.toRad()
    val pointerOffsetRotated = Offset(
        rotateX(pointerOffset.x.toDouble(), pointerOffset.y.toDouble(), angle).toFloat(),
        rotateY(pointerOffset.x.toDouble(), pointerOffset.y.toDouble(), angle).toFloat()
    )

    val px = data.x + pointerOffsetRotated.x.toDouble() / (zoomPRState.fullWidth * zoomPRState.scale)
    val py = data.y + pointerOffsetRotated.y.toDouble() / (zoomPRState.fullHeight * zoomPRState.scale)

    data.dragStartListener?.onDragStart(
        id = data.id,
        x = data.x,
        y = data.y,
        px = if (data.isConstrainedInBounds) px.coerceIn(0.0, 1.0) else px,
        py = if (data.isConstrainedInBounds) py.coerceIn(0.0, 1.0) else py
    )
}

private fun invokeDragInterceptor(
    data: MarkerData,
    zoomPRState: ZoomPanRotateState,
    deltaPx: Offset,
    position: Offset
) {
    /* Compute the displacement */
    val angle = -zoomPRState.rotation.toRad()
    val dx = rotateX(deltaPx.x.toDouble(), deltaPx.y.toDouble(), angle)
    val dy = rotateY(deltaPx.x.toDouble(), deltaPx.y.toDouble(), angle)

    val deltaX = dx / (zoomPRState.fullWidth * zoomPRState.scale)
    val deltaY = dy / (zoomPRState.fullHeight * zoomPRState.scale)

    /* Compute the pointer offset */
    val origin = Offset(- data.measuredWidth * data.relativeOffset.x, - data.measuredHeight * data.relativeOffset.y)
    val pointerOffset = position - origin
    val pointerOffsetRotated = Offset(
        rotateX(pointerOffset.x.toDouble(), pointerOffset.y.toDouble(), angle).toFloat(),
        rotateY(pointerOffset.x.toDouble(), pointerOffset.y.toDouble(), angle).toFloat()
    )

    val px = data.x + pointerOffsetRotated.x.toDouble() / (zoomPRState.fullWidth * zoomPRState.scale)
    val py = data.y + pointerOffsetRotated.y.toDouble() / (zoomPRState.fullHeight * zoomPRState.scale)

    data.dragInterceptor?.onMove(
        id = data.id,
        x = data.x,
        y = data.y,
        dx = deltaX,
        dy = deltaY,
        px = if (data.isConstrainedInBounds) px.coerceIn(0.0, 1.0) else px,
        py = if (data.isConstrainedInBounds) py.coerceIn(0.0, 1.0) else py
    )
}