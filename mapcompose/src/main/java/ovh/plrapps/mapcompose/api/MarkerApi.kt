@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.Flow
import ovh.plrapps.mapcompose.ui.state.DragInterceptor
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.rotateX
import ovh.plrapps.mapcompose.utils.rotateY
import ovh.plrapps.mapcompose.utils.toRad
import ovh.plrapps.mapcompose.utils.withRetry

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
 * @param zIndex A marker with larger zIndex will be drawn on top of all markers with smaller zIndex.
 * When markers have the same zIndex, the original order in which the parent placed the marker is used.
 * @param clickable Controls whether the marker is clickable. Default is true. If a click listener
 * is registered using [onMarkerClick], that listener will only be invoked for that marker if
 * [clickable] is true.
 */
fun MapState.addMarker(
    id: String,
    x: Double,
    y: Double,
    relativeOffset: Offset = Offset(-0.5f, -1f),
    absoluteOffset: Offset = Offset.Zero,
    zIndex: Float = 0f,
    clickable: Boolean = true,
    c: @Composable () -> Unit
) {
    markerState.addMarker(id, x, y, relativeOffset, absoluteOffset, zIndex, clickable, c)
}

/**
 * Check whether a marker was already added or not.
 */
fun MapState.hasMarker(id: String): Boolean {
    return markerState.markers.keys.contains(id)
}

/**
 * Get info on a marker, if the marker is already added.
 *
 * @return Available [MarkerInfo] if the marker was already added, `null` otherwise.
 */
fun MapState.getMarkerInfo(id: String): MarkerInfo? {
    return markerState.markers[id]?.let {
        MarkerInfo(it.id, it.x, it.y, it.relativeOffset, it.absoluteOffset, it.zIndex)
    }
}

/**
 * Updates the [zIndex] for an existing marker.
 *
 * @param id The id of the marker
 * @param zIndex A marker with larger zIndex will be drawn on top of all markers with smaller zIndex.
 * When markers have the same zIndex, the original order in which the parent placed the marker is used.
 */
fun MapState.updateMarkerZ(
    id: String,
    zIndex: Float
) {
    markerState.markers[id]?.zIndex = zIndex
}

/**
 * Updates the clickable property of an existing marker.
 *
 * @param id The id of the marker
 * @param clickable Controls whether the marker is clickable.
 */
fun MapState.updateMarkerClickable(
    id: String,
    clickable: Boolean
) {
    markerState.markers[id]?.isClickable = clickable
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
 * Beware that this clicked listener will only be invoked if the marker is clickable, and when the
 * click gesture isn't already consumed by some other composable (like a button).
 */
fun MapState.onMarkerClick(cb: (id: String, x: Double, y: Double) -> Unit) {
    markerState.markerClickCb = cb
}

/**
 * Sometimes, some components need to react to marker position change. However, the [MapState] owns
 * the [State] of each marker position. To avoid duplicating state and have the [MapState] as single
 * source of truth, this API creates an "observer" [State] of marker positions.
 */
fun MapState.markerDerivedState(): State<List<MarkerDataSnapshot>> {
    return derivedStateOf {
        markerState.markers.values.map {
            MarkerDataSnapshot(it.id, it.x, it.y)
        }
    }
}

/**
 * Similar to [markerDerivedState], but useful for asynchronous processing, using flow operators.
 */
fun MapState.markerSnapshotFlow(): Flow<List<MarkerDataSnapshot>> {
    return snapshotFlow {
        markerState.markers.values.map {
            MarkerDataSnapshot(it.id, it.x, it.y)
        }
    }
}

data class MarkerDataSnapshot(val id: String, val x: Double, val y: Double)

/**
 * Register a callback which will be invoked when a callout is tapped.
 * Beware that this click listener will only be invoked if the callout is clickable, and when the
 * click gesture isn't already consumed by some other composable (like a button).
 */
fun MapState.onCalloutClick(cb: (id: String, x: Double, y: Double) -> Unit) {
    markerState.calloutClickCb = cb
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
suspend fun MapState.centerOnMarker(
    id: String,
    destScale: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        markerState.markers[id]?.also {
            awaitLayout()
            val destScaleCst = constrainScale(destScale)
            val destScrollX = (it.x * fullWidth * destScaleCst - layoutSize.width / 2).toFloat()
            val destScrollY = (it.y * fullHeight * destScaleCst - layoutSize.height / 2).toFloat()

            withRetry(maxAnimationsRetries, animationsRetriesInterval) {
                smoothScrollAndScale(
                    destScrollX,
                    destScrollY,
                    destScale,
                    animationSpec
                )
            }
        }
    }
}

/**
 * Center on a marker, animating the scroll.
 *
 * @param id The id of the marker
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapState.centerOnMarker(
    id: String,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        markerState.markers[id]?.also {
            awaitLayout()
            val destScrollX = (it.x * fullWidth * scale - layoutSize.width / 2).toFloat()
            val destScrollY = (it.y * fullHeight * scale - layoutSize.height / 2).toFloat()

            withRetry(maxAnimationsRetries, animationsRetriesInterval) {
                smoothScrollTo(destScrollX, destScrollY, animationSpec)
            }
        }
    }
}

/**
 * Add a callout to the given position.
 *
 * @param id The id of the callout
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 * @param relativeOffset The x-axis and y-axis positions of the callout will be respectively offset by
 * the width of the marker multiplied by the x value of the offset, and the height of the marker
 * multiplied by the y value of the offset.
 * @param absoluteOffset The x-axis and y-axis positions of a callout will be respectively offset by
 * the x and y values of the offset.
 * @param zIndex A callout with larger zIndex will be drawn on top of all callouts with smaller zIndex.
 * When callouts have the same zIndex, the original order in which the parent placed the callout is used.
 * @param autoDismiss Whether the callout should be dismissed on touch down. Default is true. If set
 * to false, the callout can be programmatically dismissed with [removeCallout].
 * @param clickable Controls whether the callout is clickable. Default is false. If a click listener
 * is registered using [onMarkerClick], that listener will only be invoked for that marker if
 * [clickable] is true.
 */
fun MapState.addCallout(
    id: String,
    x: Double,
    y: Double,
    relativeOffset: Offset = Offset(-0.5f, -1f),
    absoluteOffset: Offset = Offset.Zero,
    zIndex: Float = 0f,
    autoDismiss: Boolean = true,
    clickable: Boolean = false,
    c: @Composable () -> Unit
) {
    markerState.addCallout(id, x, y, relativeOffset, absoluteOffset, zIndex, autoDismiss, clickable, c)
}

/**
 * Updates the clickable property of an existing callout.
 *
 * @param id The id of the marker
 * @param clickable Controls whether the callout is clickable.
 */
fun MapState.updateCalloutClickable(
    id: String,
    clickable: Boolean
) {
    markerState.callouts[id]?.markerData?.isClickable = clickable
}

/**
 * Removes a callout.
 *
 * @param id The id of the callout.
 */
fun MapState.removeCallout(id: String): Boolean {
    return markerState.removeCallout(id)
}

/**
 * Public data on a marker.
 */
data class MarkerInfo(
    val id: String, val x: Double,
    val y: Double,
    val relativeOffset: Offset,
    val absoluteOffset: Offset,
    val zIndex: Float
)