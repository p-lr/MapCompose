@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.demo.ui.state.DragInterceptor
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
 * @param dragInterceptor (Optional) Useful to constrain drag movements along a path. When this
 * parameter is set, you're responsible for invoking [moveMarker] with appropriate values (using
 * your own custom logic).
 * The lambda receives 5 parameters:
 * * id: The id of the marker
 * * x, y: The current position in relative coordinates
 * * dx, dy: The virtual displacement expressed in relative coordinates (not in pixels) that would
 * have been applied if there were no drag interceptor
 */
fun MapState.enableMarkerDrag(id: String, dragInterceptor: DragInterceptor? = null) {
    markerState.setDraggable(id, true)
    if (dragInterceptor != null) {
        markerState.markers[id]?.dragInterceptor = dragInterceptor
    }
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
 * Center on a marker, animating the scroll position and the scale if the composable is ready for
 * animation.
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
    with(zoomPanRotateState) {
        markerState.markers[id]?.also {
            val destScrollX = (it.x * fullWidth * destScale - layoutSize.width / 2).toFloat()
            val destScrollY = (it.y * fullHeight * destScale - layoutSize.height / 2).toFloat()

            if (isReadyForAnimation) {
                smoothScrollAndScale(
                    destScrollX,
                    destScrollY,
                    destScale,
                    animationSpec
                )
            } else {
                setScale(destScale)
                setScroll(destScrollX, destScrollY)
            }
        }
    }
}

/**
 * Center on a marker, animating the scroll if the composable is ready for animation.
 *
 * @param id The id of the marker
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
fun MapState.centerOnMarker(
    id: String,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        markerState.markers[id]?.also {
            val destScrollX = (it.x * fullWidth * scale - layoutSize.width / 2).toFloat()
            val destScrollY = (it.y * fullHeight * scale - layoutSize.height / 2).toFloat()

            if (isReadyForAnimation) {
                smoothScrollTo(destScrollX, destScrollY, animationSpec)
            } else {
                setScroll(destScrollX, destScrollY)
            }
        }
    }
}