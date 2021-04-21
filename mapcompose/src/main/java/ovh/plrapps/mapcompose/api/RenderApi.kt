package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * Controls the fade-in effect of tiles. Provided speed should be in the range [0.01f, 1.0f].
 * Values below 0.04f aren't recommended (can cause blinks), the default is 0.07f.
 * A [speed] of 1f effectively disables the fade-in effect.
 */
fun MapState.setFadeInSpeed(speed: Float) {
    tileCanvasState.alphaTick = speed
}