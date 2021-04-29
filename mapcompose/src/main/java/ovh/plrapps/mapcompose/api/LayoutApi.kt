@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.demo.ui.state.MapState
import ovh.plrapps.mapcompose.utils.AngleDegree

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
 * The [scroll] property defines the position of the top-left corner of the visible viewport.
 * This is a low-level concept (the value is in scaled pixels). To scroll to a known position,
 * prefer the [scrollToAndCenter] API.
 */
var MapState.scroll: Offset
    get() = Offset(zoomPanRotateState.scrollX, zoomPanRotateState.scrollY)
    set(value) {
        zoomPanRotateState.setScroll(value.x, value.y)
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
 * Enable the rotation by user gestures.
 */
fun MapState.enableRotation() {
    zoomPanRotateState.isRotationEnabled = true
}

/**
 * Discard rotation gestures. The map can still be programmatically rotated using APIs such as
 * [rotateTo] or [rotation].
 */
fun MapState.disableRotation() {
    zoomPanRotateState.isRotationEnabled = false
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
 * The scroll offset ratio allows to scroll past the default scroll limits. They are expressed in
 * percentage of the layout dimensions.
 * Values must be in [0f..1f] range, or an [IllegalArgumentException] is thrown.
 * Setting a scroll offset ratio is useful when rotation is enabled, so that edges of the map are
 * reachable.
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
 * Rotates to the specified [angle] in decimal degrees, animating the rotation if the composable is
 * ready for animation.
 */
fun MapState.rotateTo(
    angle: AngleDegree,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        if (isReadyForAnimation) {
            zoomPanRotateState.smoothRotateTo(angle, animationSpec)
        } else {
            setRotation(angle)
        }
    }
}

/**
 * Scrolls and center on a position, animating the scroll position and the scale. If the composable
 * isn't ready for animation, the new values are just set.
 *
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 * @param destScale The destination scale
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
fun MapState.scrollToAndCenter(
    x: Double,
    y: Double,
    destScale: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        val destScrollX = (x * fullWidth * destScale - layoutSize.width / 2).toFloat()
        val destScrollY = (y * fullHeight * destScale - layoutSize.height / 2).toFloat()

        if (isReadyForAnimation) {
            smoothScrollAndScale(
                destScrollX,
                destScrollY,
                destScale,
                animationSpec
            )
        } else {
            setScroll(destScrollX, destScrollY)
            setScale(destScale)
        }
    }
}

/**
 * Center on a position, animating the scroll position. If the composable isn't ready for animation,
 * the new values are just set.
 *
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
fun MapState.scrollToAndCenter(
    x: Double,
    y: Double,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        val destScrollX = (x * fullWidth * scale - layoutSize.width / 2).toFloat()
        val destScrollY = (y * fullHeight * scale - layoutSize.height / 2).toFloat()

        if (isReadyForAnimation) {
            smoothScrollAndScale(
                destScrollX,
                destScrollY,
                scale,
                animationSpec
            )
        } else {
            setScroll(destScrollX, destScrollY)
        }
    }
}

