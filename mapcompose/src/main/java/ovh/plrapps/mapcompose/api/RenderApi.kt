@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * Reloads all tiles.
 */
fun MapState.reloadTiles() {
    scope.launch {
        tileCanvasState.forgetTiles()
        renderVisibleTilesThrottled()
    }
}

/**
 * Controls the fade-in effect of tiles. Provided speed should be in the range [0.01f, 1.0f].
 * Values below 0.04f aren't recommended (can cause blinks), the default is 0.07f.
 * A [speed] of 1f effectively disables the fade-in effect.
 */
fun MapState.setFadeInSpeed(speed: Float) {
    scope.launch {
        tileCanvasState.alphaTick = speed
    }
}

/**
 * Disables the fade-in effect of tiles.
 */
fun MapState.disableFadeIn() {
    scope.launch {
        tileCanvasState.alphaTick = 1f
    }
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
    setBitmapFilteringEnabled { enabled }
}

/**
 * A version of [setBitmapFilteringEnabled] which allows for dynamic control of bitmap filtering
 * depending on the current [MapState].
 */
fun MapState.setBitmapFilteringEnabled(predicate: (state: MapState) -> Boolean) {
    isFilteringBitmap = { predicate(this) }
}

/**
 * Virtually increase the size of the screen by a padding in pixel amount.
 * With the appropriate value, this can be used to produce a seamless tile loading effect.
 *
 * @param padding in pixels
 */
fun MapState.setPreloadingPadding(padding: Int) {
    scope.launch {
        preloadingPadding = padding.coerceAtLeast(0)
        renderVisibleTilesThrottled()
    }
}

/**
 * The magnifying factor alters the level at which tiles are picked for a given scale. By default,
 * the level immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
 * magnifying factor of 0. The value 1 will result in picking the current level at a given scale,
 * which will be at a relative scale between 1.0 and 2.0
 */
fun MapState.setMagnifyingFactor(factor: Int) {
    scope.launch {
        visibleTilesResolver.magnifyingFactor = factor
        renderVisibleTilesThrottled()
    }
}
