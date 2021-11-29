package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * A [Tile] is defined by its coordinates in the "pyramid". But a [Tile] is sub-sampled when the
 * scale becomes lower than the scale of the lowest level. To reflect that, there is [subSample]
 * property which is a positive integer (can be 0). If [subSample] is equal to 0, it means that the
 * [bitmap] of the tile is full scale. If [subSample] is 1, the [bitmap] is sub-sampled and its size
 * is half the original bitmap (the one at the lowest level), and so on.
 */
internal data class Tile(val zoom: Int, val row: Int, val col: Int, val subSample: Int, val layerId: String) {
    lateinit var bitmap: Bitmap
    var alpha: Float by mutableStateOf(0f)
}

internal data class TileSpec(val zoom: Int, val row: Int, val col: Int, val subSample: Int = 0)

internal fun Tile.sameSpecAs(zoom: Int, row: Int, col: Int, subSample: Int, layerIds: List<String>): Boolean {
    return this.zoom == zoom && this.row == row && this.col == col && this.subSample == subSample && this.layerId in layerIds
}

internal fun Tile.samePositionAs(tile: Tile): Boolean {
    return zoom == tile.zoom && row == tile.row && col == tile.col
}