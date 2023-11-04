@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ovh.plrapps.mapcompose.ui.layout.Fill
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.layout.MinimumScaleMode
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.VisibleAreaPadding
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import ovh.plrapps.mapcompose.utils.*
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The scale of the map. By convention, the scale at full dimension is 1f.
 */
var MapState.scale: Float
    get() = zoomPanRotateState.scale
    set(value) {
        zoomPanRotateState.setScale(value)
    }

/**
 * The [rotation] property is the angle (in decimal degrees) of rotation,
 * using the center of the view as the pivot point.
 */
var MapState.rotation: AngleDegree
    get() = zoomPanRotateState.rotation
    set(value) {
        zoomPanRotateState.setRotation(value)
    }

/**
 * Get the current [scroll] - the position of the top-left corner of the visible viewport.
 * This is a low-level concept (returned value is in scaled pixels).
 */
val MapState.scroll: Offset
    get() = Offset(zoomPanRotateState.scrollX, zoomPanRotateState.scrollY)

/**
 * Set the [scroll] - the position of the top-left corner of the visible viewport. This is a
 * suspending call because it's required to wait the first composition. Otherwise, it's invoked
 * immediately.
 * This is a low-level concept (input value is expected to be in scaled pixels). To scroll to a
 * known position, prefer the [snapScrollTo] API.
 */
suspend fun MapState.setScroll(offset: Offset) {
    with(zoomPanRotateState) {
        awaitLayout()

        setScroll(offset.x, offset.y)
    }
}

fun MapState.referentialSnapshotFlow(): Flow<ReferentialSnapshot> = snapshotFlow {
    ReferentialSnapshot(zoomPanRotateState.scale, scroll, zoomPanRotateState.rotation)
}

data class ReferentialSnapshot(val scale: Float, val scroll: Offset, val rotation: AngleDegree)

/**
 * Get notified whenever the state ([scale] and/or [scroll] and/or [rotation]) changes.
 *
 * @param cb An extension function with [MapState] as receiver type
 */
fun MapState.setStateChangeListener(cb: MapState.() -> Unit) {
    stateChangeListener = cb
}

/**
 * Removes the state change listener.
 */
fun MapState.removeStateChangeListener() {
    stateChangeListener = null
}

/**
 * On double-tap, and if the scale is already at its maximum value, circle-back to the minimum scale.
 */
var MapState.shouldLoopScale
    get() = zoomPanRotateState.shouldLoopScale
    set(value) {
        zoomPanRotateState.shouldLoopScale = value
    }

/**
 * Sets the padding of the visible area of the map viewport in [Dp], for the purpose of camera moves.
 * For example, if you have some UI obscuring the map on the left, you can set the appropriate
 * left padding. Then, when you use the scrollTo methods, the map will take that into account, by
 * centering on the visible portion of the viewport.
 */
fun MapState.setVisibleAreaPadding(
    left: Dp = 0.dp,
    right: Dp = 0.dp,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp
) {
    setVisibleAreaPadding(
        left = dpToPx(left.value).roundToInt(),
        right = dpToPx(right.value).roundToInt(),
        top = dpToPx(top.value).roundToInt(),
        bottom = dpToPx(bottom.value).roundToInt()
    )
}

/**
 * Variant of [MapState.setVisibleAreaPadding] using ratios. This is a suspending call because it
 * awaits for the first layout.
 *
 * @param leftRatio The left padding will be equal to this ratio multiplied by the layout width.
 * @param rightRatio The right padding will be equal to this ratio multiplied by the layout width.
 * @param topRatio The top padding will be equal to this ratio multiplied by the layout height.
 * @param bottomRatio The bottom padding will be equal to this ratio multiplied by the layout height.
 */
suspend fun MapState.setVisibleAreaPadding(
    leftRatio: Float = 0f,
    rightRatio: Float = 0f,
    topRatio: Float = 0f,
    bottomRatio: Float = 0f
) {
    with(zoomPanRotateState) {
        awaitLayout()
        val layoutSize = zoomPanRotateState.layoutSize
        setVisibleAreaPadding(
            left = (leftRatio * layoutSize.width).roundToInt(),
            right = (rightRatio * layoutSize.width).roundToInt(),
            top = (topRatio * layoutSize.height).roundToInt(),
            bottom = (bottomRatio * layoutSize.height).roundToInt()
        )
    }
}

/**
 * Variant of [MapState.setVisibleAreaPadding] using pixels.
 */
fun MapState.setVisibleAreaPadding(left: Int = 0, right: Int = 0, top: Int = 0, bottom: Int = 0) {
    zoomPanRotateState.visibleAreaPadding = VisibleAreaPadding(left, top, right, bottom)
}

/**
 * Set the minimum scale mode. See [MinimumScaleMode].
 * The minimum scale can be manually defined using [Forced], or can be inferred using [Fill], or
 * [Fit] (the default).
 * Note: When enabling map rotation, it's advised to use the [Fill] mode.
 */
var MapState.minimumScaleMode: MinimumScaleMode
    get() = zoomPanRotateState.minimumScaleMode
    set(value) {
        zoomPanRotateState.minimumScaleMode = value
    }

/**
 * Get the current minimum scale. The minimum scale changes on [minimumScaleMode] change.
 * Do note that the initial value is always 0f. However, the value is updated after the first layout
 * pass. To be reactive on minimum scale changes, use [MapState.minScaleSnapshotFlow] api.
 */
val MapState.minScale: Float
    get() = zoomPanRotateState.minScale

/**
 * Get the minimum scale changes.
 */
fun MapState.minScaleSnapshotFlow(): Flow<Float> {
    return snapshotFlow {
        zoomPanRotateState.minScale
    }
}

/**
 * The default maximum scale is 2f.
 * When changed, and if the current scale is greater than the new [maxScale], the current scale is
 * changed to be equal to [maxScale].
 */
var MapState.maxScale: Float
    get() = zoomPanRotateState.maxScale
    set(value) {
        zoomPanRotateState.maxScale = value
    }

/**
 * The scroll offset ratio allows to scroll past the default scroll limits. They are expressed in
 * percentage of the layout dimensions.
 * Setting a scroll offset ratio is useful when rotation is enabled, so that edges of the map are
 * reachable.
 * The recommended value to try it out is 0.5f
 * Values must be in [0f..1f] range, or an [IllegalArgumentException] is thrown.
 *
 * @param xRatio The horizontal scroll offset ratio. The scroll offset will be equal to this ratio
 * multiplied by the layout width.
 * @param yRatio The vertical scroll offset ratio. The scroll offset will be equal to this ratio
 * multiplied by the layout height.
 */
fun MapState.setScrollOffsetRatio(xRatio: Float, yRatio: Float) {
    zoomPanRotateState.scrollOffsetRatio = Offset(xRatio, yRatio)
}

/**
 * Rotates to the specified [angle] in decimal degrees, animating the rotation.
 */
suspend fun MapState.rotateTo(
    angle: AngleDegree,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    withRetry(maxAnimationsRetries, animationsRetriesInterval) {
        zoomPanRotateState.smoothRotateTo(angle, animationSpec)
    }
}

/**
 * Get the layout dimensions in pixels.
 * Note that layout dimension may change during the lifetime of the application. The returned value
 * is a read-only snapshot.
 */
suspend fun MapState.getLayoutSize(): IntSize {
    return with(zoomPanRotateState) {
        awaitLayout()
        layoutSize
    }
}

/**
 * Get the layout dimensions in pixels, as a [Flow].
 * This api is useful to react on layout changes.
 */
suspend fun MapState.getLayoutSizeFlow(): Flow<IntSize> {
    return with(zoomPanRotateState) {
        awaitLayout()
        snapshotFlow {
            layoutSize
        }
    }
}

/**
 * Scrolls to a position. Defaults to centering on the provided scroll destination.
 *
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 * @param screenOffset Offset of the screen relatively to its dimension. Default is
 * Offset(-0.5f, -0.5f), so moving the screen by half the width left and by half the height top,
 * effectively centering on the scroll destination.
 */
suspend fun MapState.snapScrollTo(
    x: Double,
    y: Double,
    screenOffset: Offset = Offset(-0.5f, -0.5f)
) {
    with(zoomPanRotateState) {
        awaitLayout()
        val offsetX = screenOffset.x * layoutSize.width
        val offsetY = screenOffset.y * layoutSize.height

        val paddingOffset = visibleAreaPadding.getOffsetForScroll(rotation)
        val destScrollX = (x * fullWidth * scale + offsetX - paddingOffset.x).toFloat()
        val destScrollY = (y * fullHeight * scale + offsetY - paddingOffset.y).toFloat()

        setScroll(destScrollX, destScrollY)
    }
}

/**
 * Scrolls to a position, animating the scroll and the scale. Defaults to centering on the provided
 * scroll destination.
 *
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 * @param destScale The destination scale. The default value is the current scale.
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 * @param screenOffset Offset of the screen relatively to its dimension. Default is
 * Offset(-0.5f, -0.5f), so moving the screen by half the width left and by half the height top,
 * effectively centering on the scroll destination.
 */
suspend fun MapState.scrollTo(
    x: Double,
    y: Double,
    destScale: Float = scale,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
    screenOffset: Offset = Offset(-0.5f, -0.5f)
) {
    with(zoomPanRotateState) {
        awaitLayout()
        val offsetX = screenOffset.x * layoutSize.width
        val offsetY = screenOffset.y * layoutSize.height

        val effectiveDstScale = constrainScale(destScale)

        val paddingOffset = visibleAreaPadding.getOffsetForScroll(rotation)
        val destScrollX = (x * fullWidth * effectiveDstScale + offsetX - paddingOffset.x).toFloat()
        val destScrollY = (y * fullHeight * effectiveDstScale + offsetY - paddingOffset.y).toFloat()

        withRetry(maxAnimationsRetries, animationsRetriesInterval) {
            smoothScrollScaleRotate(
                destScrollX,
                destScrollY,
                effectiveDstScale,
                animationSpec
            )
        }
    }
}

/**
 * Scrolls to an area. The target position will be centered on the area, scaled in as much as
 * possible while still keeping the area plus the provided padding completely in view.
 *
 * @param area The [BoundingBox] of the target area to scroll to.
 * @param padding Padding around the area defined as a fraction of the viewport.
 */
suspend fun MapState.snapScrollTo(
    area: BoundingBox,
    padding: Offset = Offset(0f, 0f)
) {
    with(zoomPanRotateState) {
        awaitLayout()
        val (center, scale) = calculateScrollTo(area, padding)
        setScale(scale)
        snapScrollTo(center.x, center.y)
    }
}

/**
 * Scrolls to an area, animating the scroll and the scale. The target position will be centered
 * on the area, scaled in as much as possible while still keeping the area plus the provided
 * padding completely in view.
 *
 * @param area The [BoundingBox] of the target area to scroll to.
 * @param padding Padding around the area defined as a fraction of the viewport.
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapState.scrollTo(
    area: BoundingBox,
    padding: Offset = Offset(0f, 0f),
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
) {
    with(zoomPanRotateState) {
        awaitLayout()
        val (center, scale) = calculateScrollTo(area, padding)
        scrollTo(center.x, center.y, scale, animationSpec)
    }
}

/**
 * Calculates the target scroll position and scale that will be centered on the given [area] and
 * scaled in as much as possible while keeping the [area] plus [padding] in view.
 *
 * @return The scroll position and scale, as a [Pair].
 */
private fun ZoomPanRotateState.calculateScrollTo(
    area: BoundingBox,
    padding: Offset
): Pair<Point, Float> {
    val centerX = (area.xLeft + area.xRight) / 2
    val centerY = (area.yTop + area.yBottom) / 2

    val xAxisScale = fullHeight / fullWidth.toDouble()
    val normalizedArea = area.scaleAxis(1 / xAxisScale)
    val rotatedNormalizedArea =
        normalizedArea.rotate(Point(centerX / xAxisScale, centerY), -rotation.toRad())
    val rotatedArea = rotatedNormalizedArea.scaleAxis(xAxisScale)
    val visibleAreaMargin = visibleAreaPadding.getRotatedMargin(rotation)

    val areaWidth = fullWidth * (rotatedArea.xRight - rotatedArea.xLeft)
    val availableViewportWidth = (layoutSize.width - visibleAreaMargin.x) * (1 - padding.x)
    val horizontalScale = availableViewportWidth / areaWidth

    val areaHeight = fullHeight * (rotatedArea.yBottom - rotatedArea.yTop)
    val availableViewportHeight = (layoutSize.height - visibleAreaMargin.y) * (1 - padding.y)
    val verticalScale = availableViewportHeight / areaHeight

    val targetScale = min(horizontalScale, verticalScale).toFloat()
    val effectiveTargetScale = constrainScale(targetScale)

    return Point(centerX, centerY) to effectiveTargetScale
}

/**
 * The [centroidX] is the x coordinate of the center of the map (which is also the origin of
 * rotation transformation). It changes with the scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a relative coordinate (in [0.0 .. 1.0] range).
 */
val MapState.centroidX: Double
    get() = zoomPanRotateState.centroidX

/**
 * The [centroidY] is the y coordinate of the center of the map (which is also the origin of
 * rotation transformation). It changes with the scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a relative coordinate (in [0.0 .. 1.0] range).
 */
val MapState.centroidY: Double
    get() = zoomPanRotateState.centroidY

/**
 * Get the flow of centroid points. A centroid point contains the normalized coordinates of the
 * center of the map.
 * Useful for asynchronous processing using flow operators. Like every snapshot flow, it should be
 * collected from the main thread.
 *
 * Example:
 * ```
 * mapState.centroidSnapshotFlow().map { point ->
 *   withContext(Dispatchers.Default) {
 *     // some heavy computing
 *   }
 * }.launchIn(scope)  // scope is using Dispatchers.Main
 * ```
 */
fun MapState.centroidSnapshotFlow(): Flow<Point> {
    return snapshotFlow {
        Point(zoomPanRotateState.centroidX, zoomPanRotateState.centroidY)
    }
}

/**
 * A convenience property. It corresponds to the size used when creating the [MapState].
 */
val MapState.fullSize: IntSize
    get() = IntSize(zoomPanRotateState.fullWidth, zoomPanRotateState.fullHeight)

/**
 * Stops all currently running animations. If other animations are scheduled to run (inside running
 * coroutines), you might have to cancel those coroutines as well.
 */
suspend fun MapState.stopAnimations() {
    zoomPanRotateState.stopAnimations()
}

/**
 * Returns the visible area expressed in normalized coordinates. This does not account for rotation.
 * When the map isn't rotated, the obtained [BoundingBox] represents the same area as the one
 * obtained with the [visibleArea] API.
 */
suspend fun MapState.visibleBoundingBox(): BoundingBox {
    return with(zoomPanRotateState) {
        awaitLayout()

        BoundingBox(
            xLeft = centroidX - layoutSize.width / (2 * fullWidth * scale),
            yTop = centroidY - layoutSize.height / (2 * fullHeight * scale),
            xRight = centroidX + layoutSize.width / (2 * fullWidth * scale),
            yBottom = centroidY + layoutSize.height / (2 * fullHeight * scale)
        )
    }
}

data class BoundingBox(val xLeft: Double, val yTop: Double, val xRight: Double, val yBottom: Double)

/**
 * Returns the visible area expressed in normalized coordinates. This *does* account for rotation.
 *
 * @return The [VisibleArea], as follows:
 *    p1         p2
 *      ---------
 *      |       |
 *      |       |
 *      |       |
 *      ---------
 *    p4         p3
 */
suspend fun MapState.visibleArea(padding: IntOffset = IntOffset.Zero): VisibleArea {
    return with(zoomPanRotateState) {
        awaitLayout()

        val xLeft = centroidX - (layoutSize.width + padding.x * 2) / (2 * fullWidth * scale)
        val yTop = centroidY - (layoutSize.height + padding.y * 2) / (2 * fullHeight * scale)
        val xRight = centroidX + (layoutSize.width + padding.x * 2) / (2 * fullWidth * scale)
        val yBottom = centroidY + (layoutSize.height + padding.y * 2) / (2 * fullHeight * scale)

        val xAxisScale = fullHeight / fullWidth.toDouble()
        val scaledCenterX = centroidX / xAxisScale

        val p1x = rotateCenteredX(
            xLeft / xAxisScale, yTop, scaledCenterX, centroidY, -rotation.toRad()
        ) * xAxisScale
        val p1y = rotateCenteredY(
            xLeft / xAxisScale, yTop, scaledCenterX, centroidY, -rotation.toRad()
        )

        val p2x = rotateCenteredX(
            xRight / xAxisScale, yTop, scaledCenterX, centroidY, -rotation.toRad()
        ) * xAxisScale
        val p2y = rotateCenteredY(
            xRight / xAxisScale, yTop, scaledCenterX, centroidY, -rotation.toRad()
        )

        val p3x = rotateCenteredX(
            xRight / xAxisScale, yBottom, scaledCenterX, centroidY, -rotation.toRad()
        ) * xAxisScale
        val p3y = rotateCenteredY(
            xRight / xAxisScale, yBottom, scaledCenterX, centroidY, -rotation.toRad()
        )

        val p4x = rotateCenteredX(
            xLeft / xAxisScale, yBottom, scaledCenterX, centroidY, -rotation.toRad()
        ) * xAxisScale
        val p4y = rotateCenteredY(
            xLeft / xAxisScale, yBottom, scaledCenterX, centroidY, -rotation.toRad()
        )

        visibleAreaMutex.withLock {
            val area = visibleArea
            if (area == null) {
                visibleArea = VisibleArea(p1x, p1y, p2x, p2y, p3x, p3y, p4x, p4y)
            } else {
                area._p1x = p1x
                area._p1y = p1y
                area._p2x = p2x
                area._p2y = p2y
                area._p3x = p3x
                area._p3y = p3y
                area._p4x = p4x
                area._p4y = p4y
            }
            visibleArea as VisibleArea
        }
    }
}

data class VisibleArea(
    internal var _p1x: Double,
    internal var _p1y: Double,
    internal var _p2x: Double,
    internal var _p2y: Double,
    internal var _p3x: Double,
    internal var _p3y: Double,
    internal var _p4x: Double,
    internal var _p4y: Double,
) {
    val p1x: Double
        get() = _p1x
    val p1y: Double
        get() = _p1y
    val p2x: Double
        get() = _p2x
    val p2y: Double
        get() = _p2y
    val p3x: Double
        get() = _p3x
    val p3y: Double
        get() = _p3y
    val p4x: Double
        get() = _p4x
    val p4y: Double
        get() = _p4y
}

/* Internally, we're working on a single VisibleArea instance, and we must ensure mutual exclusion
 * when creating the instance. */
internal val visibleAreaMutex = Mutex()
internal var visibleArea: VisibleArea? = null

/**
 * The [MapState] is considered idle when its [centroidX] and [centroidY] haven't changed for at
 * least [thresholdMillis] which is 400ms by default.
 */
fun MapState.idleStateFlow(thresholdMillis: Long = 400): StateFlow<Boolean> {
    val stateFlow = MutableStateFlow(false)

    scope.launch {
        snapshotFlow {
            "$centroidX,$centroidY"
        }.map {
            stateFlow.value = false
        }.collectLatest {
            delay(thresholdMillis)
            stateFlow.value = true
        }
    }

    return stateFlow.asStateFlow()
}


