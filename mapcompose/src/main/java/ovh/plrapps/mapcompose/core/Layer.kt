package ovh.plrapps.mapcompose.core

import java.util.*

internal data class Layer(
    val id: String,
    val tileStreamProvider: TileStreamProvider,
    val alpha: Float = 1f
)

sealed interface LayerPlacement
object AboveAll : LayerPlacement
object BelowAll : LayerPlacement
data class AboveLayer(val layerId: String) : LayerPlacement
data class BelowLayer(val layerId: String) : LayerPlacement

internal fun makeLayerId(): String = UUID.randomUUID().toString()
