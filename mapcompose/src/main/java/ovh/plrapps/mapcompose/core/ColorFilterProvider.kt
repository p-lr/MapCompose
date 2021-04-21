package ovh.plrapps.mapcompose.core

import androidx.compose.ui.graphics.ColorFilter


/**
 * Provides a [ColorFilter] for a tile coordinate.
 */
fun interface ColorFilterProvider {
    /* Must not be a blocking call - should return immediately */
    fun getColorFilter(row: Int, col: Int, zoomLvl: Int): ColorFilter?
}