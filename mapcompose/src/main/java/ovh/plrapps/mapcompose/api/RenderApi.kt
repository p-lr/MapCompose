@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.ColorFilterProvider
import androidx.compose.ui.graphics.ColorFilter
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * Controls the fade-in effect of tiles. Provided speed should be in the range [0.01f, 1.0f].
 * Values below 0.04f aren't recommended (can cause blinks), the default is 0.07f.
 * A [speed] of 1f effectively disables the fade-in effect.
 */
fun MapState.setFadeInSpeed(speed: Float) {
    tileCanvasState.alphaTick = speed
}

/**
 * Disables the fade-in effect of tiles.
 */
fun MapState.disableFadeIn() {
    tileCanvasState.alphaTick = 1f
}

/**
 * Applies a [ColorFilter] for each tile. A different [ColorFilter] can be applied depending on the
 * coordinate of tiles.
 * This change triggers a re-composition (effects are immediately visible).
 */
fun MapState.setColorFilterProvider(provider: ColorFilterProvider) {
    tileCanvasState.colorFilterProvider = provider
}

/**
 * Enables a workaround preventing tiles from being drawn not completely touching each other, making
 * the map background visible in-between tiles. The workaround incurs a slight performance cost.
 */
fun MapState.setTileBleedWorkaroundEnabled(enabled: Boolean) {
    isTileBleedWorkaroundEnabled = enabled
}
