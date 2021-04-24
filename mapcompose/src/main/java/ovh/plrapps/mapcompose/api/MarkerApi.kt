package ovh.plrapps.mapcompose.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.rotateX
import ovh.plrapps.mapcompose.utils.rotateY
import ovh.plrapps.mapcompose.utils.toRad

fun MapState.addMarker(
    id: String,
    x: Double,
    y: Double,
    relativeOffset: Offset = Offset(-0.5f, -1f),
    absoluteOffset: Offset = Offset.Zero,
    c: @Composable () -> Unit
) {
    markerState.addMarker(id, x, y, relativeOffset, absoluteOffset, c)
}

fun MapState.removeMarker(id: String): Boolean {
    return markerState.removeMarker(id)
}

fun MapState.moveMarker(id: String, x: Double, y: Double) {
    markerState.moveMarkerTo(id, x, y)
}

fun MapState.enableMarkerDrag(id: String) {
    markerState.setDraggable(id, true)
}

fun MapState.disableMarkerDrag(id: String) {
    markerState.setDraggable(id, false)
}

fun MapState.onMarkerMove(
    cb: (id: String, x: Double, y: Double, dx: Double, dy: Double) -> Unit
) {
    markerState.markerMoveCb = cb
}

fun MapState.onMarkerClick(cb: (id: String, x: Double, y: Double) -> Unit) {
    markerState.markerClickCb = cb
}

/**
 * Move a marker, given a displacement in pixels. This is typically useful when programmatically
 * simulating a drag gesture.
 * This API is internally used when enabling drag gestures on a marker using [enableMarkerDrag].
 *
 * @param id The id of the marker
 * @param deltaPx The displacement amount in pixels
 */
fun MapState.moveMarkerBy(id: String, deltaPx: Offset) {
    val angle = -zoomPanRotateState.rotation.toRad()
    val dx = rotateX(deltaPx.x.toDouble(), deltaPx.y.toDouble(), angle)
    val dy = rotateY(deltaPx.x.toDouble(), deltaPx.y.toDouble(), angle)
    markerState.moveMarkerBy(
        id,
        dx / (zoomPanRotateState.fullWidth * zoomPanRotateState.scale),
        dy / (zoomPanRotateState.fullHeight * zoomPanRotateState.scale)
    )
}