@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import ovh.plrapps.mapcompose.core.ColorFilterProvider
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
 * Sets the background color visible before tiles are loaded or when the canvas outside of the
 * map area is in view.
 */
fun MapState.setMapBackground(color: Color) {
    mapBackground = color
}

/**
 * Controls whether Bitmap filtering is enabled when drawing tiles. This is enabled by default.
 * Disabling it is useful to achieve nearest-neighbor scaling, for cases when the art style of the
 * displayed image benefits from it.
 * @see [android.graphics.Paint.setFilterBitmap]
 */
fun MapState.setBitmapFilteringEnabled(enabled: Boolean) {
    isBitmapFilteringEnabled = enabled
}
