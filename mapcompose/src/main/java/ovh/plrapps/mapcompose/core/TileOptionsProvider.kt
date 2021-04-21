package ovh.plrapps.mapcompose.core

import android.graphics.ColorFilter

/**
 * Tile rendering options provider. Optional parameter for
 * [ovh.plrapps.mapcompose.MapViewConfiguration].
 */
interface TileOptionsProvider {
    /* Must not be a blocking call - should return immediately */
    fun getColorFilter(row: Int, col: Int, zoomLvl: Int): ColorFilter? = null
}