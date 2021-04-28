@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.demo.ui.state.MapState
import ovh.plrapps.mapcompose.utils.rotateX
import ovh.plrapps.mapcompose.utils.rotateY
import ovh.plrapps.mapcompose.utils.toRad

/**
 * Add a marker to the given position.
 *
 * @param id The id of the marker
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 * @param relativeOffset The x-axis and y-axis positions of the marker will be respectively offset by
 * the width of the marker multiplied by the x value of the offset, and the height of the marker
 * multiplied by the y value of the offset.
 * @param absoluteOffset The x-axis and y-axis positions of a marker will be respectively offset by
 * the x and y values of the offset.
 */
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

/**
 * Remove a marker.
 *
 * @param id The id of the marker
 */
fun MapState.removeMarker(id: String): Boolean {
    return markerState.removeMarker(id)
}

/**
 * Move marker to the given position.
 *
 * @param id The id of the marker
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 */
fun MapState.moveMarker(id: String, x: Double, y: Double) {
    markerState.moveMarkerTo(id, x, y)
}

/**
 * Enable drag gestures on a marker.
 *
 * @param id The id of the marker
 */
fun MapState.enableMarkerDrag(id: String) {
    markerState.setDraggable(id, true)
}

/**
 * Disable drag gestures on a marker.
 *
 * @param id The id of the marker
 */
fun MapState.disableMarkerDrag(id: String) {
    markerState.setDraggable(id, false)
}

/**
 * Register a callback which will be invoked for every marker move (API move and user drag).
 */
fun MapState.onMarkerMove(
    cb: (id: String, x: Double, y: Double, dx: Double, dy: Double) -> Unit
) {
    markerState.markerMoveCb = cb
}

/**
 * Register a callback which will be invoked when a marker is tapped.
 */
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

/**
 * Center on a marker, animating the scroll position and the scale.
 *
 * @param id The id of the marker
 * @param destScale The destination scale
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
fun MapState.centerOnMarker(
    id: String,
    destScale: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    markerState.markers[id]?.also {
        val size = zoomPanRotateState.layoutSize
        val destScrollX = it.x * zoomPanRotateState.fullWidth * destScale - size.width / 2
        val destScrollY = it.y * zoomPanRotateState.fullHeight * destScale - size.height / 2

        zoomPanRotateState.smoothScrollAndScale(
            destScrollX.toFloat(),
            destScrollY.toFloat(),
            destScale,
            animationSpec
        )
    }
}

/**
 * Center on a marker, animating the scroll only.
 *
 * @param id The id of the marker
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
fun MapState.centerOnMarker(
    id: String,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    markerState.markers[id]?.also {
        val size = zoomPanRotateState.layoutSize
        val scale = zoomPanRotateState.scale
        val destScrollX = it.x * zoomPanRotateState.fullWidth * scale - size.width / 2
        val destScrollY = it.y * zoomPanRotateState.fullHeight * scale - size.height / 2

        zoomPanRotateState.smoothScrollTo(
            destScrollX.toFloat(),
            destScrollY.toFloat(),
            animationSpec
        )
    }
}