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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.DragInterceptor
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import ovh.plrapps.mapcompose.utils.*
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
 * is registered using [onMarkerClick], that listener will be invoked for that marker if [clickable]
 * is true.
 * @param clipShape Was originally introduced to clip the ripple effect when the library had a click
 * listener for each marker. However the library doesn't work like that anymore.
 * As of 2.4.1, this parameter is made no-op, and will be removed in a future major version.
 * @param isConstrainedInBounds By default, a marker cannot be positioned or moved outside of the
 * map bounds.
 * @param clickableAreaScale The clickable area, which defaults to the bounds of the
 * provided composable, can be expanded or shrinked. For example, using Offset(1.2f, 1f), the
 * clickable are will be expanded by 20% on the X axis relatively to the center.
 * @param clickableAreaCenterOffset The center of the clickable area will be offset by
 * the width of the marker multiplied by the x value of the offset, and the height of the marker
 * multiplied by the y value of the offset.
 */
fun MapState.addMarker(
    id: String,
    x: Double,
    y: Double,
    relativeOffset: Offset = Offset(-0.5f, -1f),
    absoluteOffset: Offset = Offset.Zero,
    zIndex: Float = 0f,
    clickable: Boolean = true,
    clipShape: Shape? = null,
    isConstrainedInBounds: Boolean = true,
    clickableAreaScale: Offset = Offset(1f, 1f),
    clickableAreaCenterOffset: Offset = Offset(0f, 0f),
    c: @Composable () -> Unit
) {
    markerState.addMarker(
        id,
        x,
        y,
        relativeOffset,
        absoluteOffset,
        zIndex,
        clickable,
        isConstrainedInBounds,
        clickableAreaScale,
        clickableAreaCenterOffset,
        RenderingStrategy.Default,
        c
    )
}

/**
 * @see [addMarker]
 * @param renderingStrategy By default, markers are eagerly laid-out, e.g they are laid-out
 * even when not visible. There are two alternative rendering strategies:
 * - [RenderingStrategy.LazyLoading]: removes all non-visible markers, dynamically.
 * - [RenderingStrategy.Clustering]: in addition to lazy loading, clusterize markers when they are
 * close to each other.
 */
@ExperimentalClusteringApi
fun MapState.addMarker(
    id: String,
    x: Double,
    y: Double,
    relativeOffset: Offset = Offset(-0.5f, -1f),
    absoluteOffset: Offset = Offset.Zero,
    zIndex: Float = 0f,
    clickable: Boolean = true,
    clipShape: Shape? = null,
    isConstrainedInBounds: Boolean = true,
    clickableAreaScale: Offset = Offset(1f, 1f),
    clickableAreaCenterOffset: Offset = Offset(0f, 0f),
    renderingStrategy: RenderingStrategy = RenderingStrategy.Default,
    c: @Composable () -> Unit
) {
    markerState.addMarker(
        id,
        x,
        y,
        relativeOffset,
        absoluteOffset,
        zIndex,
        clickable,
        isConstrainedInBounds,
        clickableAreaScale,
        clickableAreaCenterOffset,
        renderingStrategy,
        c
    )
}

/**
 * Add a clusterer which will clusterize all markers added with the same clusterer id.
 * The default behavior on cluster click is a zoom-in to reveal the content of the clicked
 * cluster. This can be changed using [clusterClickBehavior].
 * The style of a cluster is user-defined using [clusterFactory].
 *
 * @param id The id of the clusterer.
 * @param clusteringThreshold When the distance between two markers goes below that threshold, a
 * cluster is formed. Defaults to 50 dp. There's one exception: when the scale reaches max scale,
 * in which case clustering is disabled.
 * @param clusterClickBehavior Defines the behavior when a cluster is clicked.
 * @param clusterFactory Compose code for a cluster. Receives the list of marker ids which are fused
 * to form the cluster.
 */
@ExperimentalClusteringApi
fun MapState.addClusterer(
    id: String,
    clusteringThreshold: Dp = 50.dp,
    clusterClickBehavior: ClusterClickBehavior = Default,
    clusterFactory: (ids: List<String>) -> (@Composable () -> Unit)
) {
    markerState.addClusterer(
        mapState = this,
        id = id,
        clusteringThreshold = clusteringThreshold,
        clusterClickBehavior = clusterClickBehavior.toInternal(),
        clusterFactory = clusterFactory
    )
}

/**
 * Add a lazy loader for markers. The lazy loader removes markers as they go out of the visible area
 * (and adds markers which are visible).
 *
 * @param id The id for the lazy loader
 * @param padding Padding added to the visible area, in dp. Defaults to 0.
 */
@ExperimentalClusteringApi
fun MapState.addLazyLoader(
    id: String,
    padding: Dp = 0.dp
) {
    markerState.addLazyLoader(this, id, padding)
}

/**
 * Remove a clusterer.
 * By default, also removes all markers managed by this clusterer.
 */
@ExperimentalClusteringApi
fun MapState.removeClusterer(
    id: String,
    removeManagedMarkers: Boolean = true
) {
    markerState.removeClusterer(id, removeManagedMarkers)
}

/**
 * Remove a lazy loader.
 * By default, also removes all markers managed by this lazy loader.
 */
@ExperimentalClusteringApi
fun MapState.removeLazyLoader(
    id: String,
    removeManagedMarkers: Boolean = true
) {
    markerState.removeLazyLoader(id, removeManagedMarkers)
}

/**
 * Check whether a marker was already added or not.
 */
fun MapState.hasMarker(id: String): Boolean {
    return markerState.hasMarker(id)
}

/**
 * Get info on a marker, if the marker is already added.
 *
 * @return Available [MarkerInfo] if the marker was already added, `null` otherwise.
 */
fun MapState.getMarkerInfo(id: String): MarkerInfo? {
    return markerState.getMarker(id)?.let {
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
    markerState.getMarker(id)?.zIndex = zIndex
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
    markerState.getMarker(id)?.isClickable = clickable
}

/**
 * Updates the constrained in bounds state of the marker.
 *
 * @param id The id of the marker
 * @param constrainedInBounds Controls whether the marker is constrained inside map bounds
 */
fun MapState.updateMarkerConstrained(
    id: String,
    constrainedInBounds: Boolean
) {
    val markerData = markerState.getMarker(id) ?: return
    markerData.isConstrainedInBounds = constrainedInBounds

    /* If constrained, immediately move the marker to its constrained position */
    if (constrainedInBounds) {
        markerState.moveMarkerTo(id, markerData.x, markerData.y)
    }
}

/**
 * Updates the clickable area of the marker.
 */
fun MapState.updateClickableArea(
    id: String,
    clickableAreaScale: Offset? = null,
    clickableAreaCenterOffset: Offset? = null
) {
    val markerData = markerState.getMarker(id) ?: return
    if (clickableAreaScale != null) {
        markerData.clickableAreaScale = clickableAreaScale
    }
    if (clickableAreaCenterOffset != null) {
        markerData.clickableAreaCenterOffset = clickableAreaCenterOffset
    }
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
 * Remove all markers.
 */
fun MapState.removeAllMarkers() {
    markerState.removeAllMarkers()
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
 * See [DragInterceptor].
 */
fun MapState.enableMarkerDrag(id: String, dragInterceptor: DragInterceptor? = null) {
    markerState.setDraggable(id, true)
    if (dragInterceptor != null) {
        markerState.getMarker(id)?.dragInterceptor = dragInterceptor
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
 * Beware that this click listener will only be invoked if the marker is clickable, and when the
 * click gesture isn't already consumed by some other composable (like a button).
 */
fun MapState.onMarkerClick(cb: (id: String, x: Double, y: Double) -> Unit) {
    markerState.markerClickCb = cb
}

/**
 * Register a callback which will be invoked when a marker is long-pressed.
 * Beware that the provided callback will only be invoked if the marker is clickable, and when the
 * gesture isn't already consumed by some other composable (like a button).
 */
fun MapState.onMarkerLongPress(cb: (id: String, x: Double, y: Double) -> Unit) {
    markerState.markerLongPressCb = cb
}

/**
 * Sometimes, some components need to react to marker position change. However, the [MapState] owns
 * the [State] of each marker position. To avoid duplicating state and have the [MapState] as single
 * source of truth, this API creates an "observer" [State] of marker positions.
 * Note that this api only accounts for regular markers (e.g not managed by a clusterer).
 */
fun MapState.markerDerivedState(): State<List<MarkerDataSnapshot>> {
    return derivedStateOf {
        markerState.getRenderedMarkers().map {
            MarkerDataSnapshot(it.id, it.x, it.y)
        }
    }
}

/**
 * Similar to [markerDerivedState], but useful for asynchronous processing, using flow operators.
 * Like every snapshot flow, it should be collected from the main thread.
 * Note that this api only accounts for regular markers (e.g not managed by a clusterer).
 */
fun MapState.markerSnapshotFlow(): Flow<List<MarkerDataSnapshot>> {
    return snapshotFlow {
        markerState.getRenderedMarkers().map {
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
    markerRenderState.calloutClickCb = cb
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
        markerState.getMarker(id)?.also {
            awaitLayout()
            val paddingOffset = visibleAreaPadding.getOffsetForScroll(rotation)
            val destScrollX = (it.x * fullWidth * scale - layoutSize.width / 2 - paddingOffset.x).toFloat()
            val destScrollY = (it.y * fullHeight * scale - layoutSize.height / 2 - paddingOffset.y).toFloat()

            withRetry(maxAnimationsRetries, animationsRetriesInterval) {
                smoothScrollTo(destScrollX, destScrollY, animationSpec)
            }
        }
    }
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
        markerState.getMarker(id)?.also {
            awaitLayout()
            val destScaleCst = constrainScale(destScale)
            val paddingOffset = visibleAreaPadding.getOffsetForScroll(rotation)
            val destScrollX = (it.x * fullWidth * destScaleCst - layoutSize.width / 2 - paddingOffset.x).toFloat()
            val destScrollY = (it.y * fullHeight * destScaleCst - layoutSize.height / 2 - paddingOffset.y).toFloat()

            withRetry(maxAnimationsRetries, animationsRetriesInterval) {
                smoothScrollScaleRotate(
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
 * Center on a marker, animating the scroll position, the scale, and the rotation.
 *
 * @param id The id of the marker
 * @param destScale The destination scale
 * @param destAngle The destination angle in decimal degrees
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapState.centerOnMarker(
    id: String,
    destScale: Float,
    destAngle: AngleDegree,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        markerState.getMarker(id)?.also {
            awaitLayout()
            val destScaleCst = constrainScale(destScale)
            val paddingOffset = visibleAreaPadding.getOffsetForScroll(rotation)
            val destScrollX = (it.x * fullWidth * destScaleCst - layoutSize.width / 2 - paddingOffset.x).toFloat()
            val destScrollY = (it.y * fullHeight * destScaleCst - layoutSize.height / 2 - paddingOffset.y).toFloat()

            withRetry(maxAnimationsRetries, animationsRetriesInterval) {
                smoothScrollScaleRotate(
                    destScrollX,
                    destScrollY,
                    destScale,
                    destAngle,
                    animationSpec
                )
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
 * @param isConstrainedInBounds By default, a callout cannot be positioned outside of the map
 * bounds.
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
    isConstrainedInBounds: Boolean = true,
    c: @Composable () -> Unit
) {
    markerRenderState.addCallout(
        id,
        x,
        y,
        relativeOffset,
        absoluteOffset,
        zIndex,
        autoDismiss,
        clickable,
        isConstrainedInBounds,
        c
    )
}

/**
 * Check whether a callout was already added or not.
 */
fun MapState.hasCallout(id: String): Boolean {
    return markerRenderState.hasCallout(id)
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
    markerRenderState.callouts[id]?.markerData?.isClickable = clickable
}

/**
 * Moves a callout.
 *
 * @param id The id of the callout.
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 */
fun MapState.moveCallout(id: String, x: Double, y: Double) {
    markerRenderState.moveCallout(id, x, y)
}

/**
 * Removes a callout.
 *
 * @param id The id of the callout.
 */
fun MapState.removeCallout(id: String): Boolean {
    return markerRenderState.removeCallout(id)
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