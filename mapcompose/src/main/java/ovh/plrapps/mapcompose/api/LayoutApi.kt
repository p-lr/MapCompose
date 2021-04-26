@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
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
 * [smoothRotateTo].
 */
fun MapState.disableRotation() {
    zoomPanRotateState.isRotationEnabled = false
}

/**
 * Animates the rotation of the map to the specified [angle] in decimal degrees.
 */
fun MapState.smoothRotateTo(
    angle: AngleDegree,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    zoomPanRotateState.smoothRotateTo(angle, animationSpec)
}

