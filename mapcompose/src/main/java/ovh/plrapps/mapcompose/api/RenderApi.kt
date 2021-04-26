package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.ColorFilterProvider
import androidx.compose.ui.graphics.ColorFilter
import ovh.plrapps.mapcompose.demo.ui.state.MapState

/**
 * Controls the fade-in effect of tiles. Provided speed should be in the range [0.01f, 1.0f].
 * Values below 0.04f aren't recommended (can cause blinks), the default is 0.07f.
 * A [speed] of 1f effectively disables the fade-in effect.
 */
fun MapState.setFadeInSpeed(speed: Float) {
    tileCanvasState.alphaTick = speed
}

/**
 * Applies a [ColorFilter] for each tile. A different [ColorFilter] can be applied depending on the
 * coordinate of tiles.
 * This change triggers a re-composition (effects are immediately visible).
 */
fun MapState.setColorFilterProvider(provider: ColorFilterProvider) {
    tileCanvasState.colorFilterProvider = provider
}