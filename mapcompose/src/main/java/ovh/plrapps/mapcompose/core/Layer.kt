package ovh.plrapps.mapcompose.core

import androidx.compose.runtime.mutableStateOf
import java.util.*

data class Layer(
    val id: String,
    val tileStreamProvider: TileStreamProvider,
    val initialOpacity: Float = 1f
) {
    internal val alpha = mutableStateOf(initialOpacity)
}

sealed interface LayerPlacement
object AboveAll : LayerPlacement
object BelowAll : LayerPlacement
data class AboveLayer(val layerId: String) : LayerPlacement
data class BelowLayer(val layerId: String) : LayerPlacement

private const val mainLayerPrefix = "mainLayer"
internal fun makeMainLayerId(): String = mainLayerPrefix + UUID.randomUUID()
internal fun String.isMainLayer(): Boolean = startsWith(mainLayerPrefix)

