package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.AboveAll
import ovh.plrapps.mapcompose.core.Layer
import ovh.plrapps.mapcompose.core.LayerPlacement
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * Change of primary layer. When creating the [MapState] instance, a primary layer is automatically
 * created. That primary layer can later be changed using this API, which causes all tiles to be
 * redrawn.
 */
fun MapState.setPrimaryLayer(tileStreamProvider: TileStreamProvider) {
    tileCanvasState.setPrimaryLayer(tileStreamProvider)
    redrawTiles()
}

/**
 * Define layers which are drawn above the primary layer. Layers are drawn in the same order as
 * they're provided in the list. For example, the last layer of [layers] will be drawn last and will
 * appear above all the other layers.
 * This API replaces all existing layers.
 */
fun MapState.setLayers(layers: List<Layer>) {
    tileCanvasState.setLayers(layers)
    refresh()
}

fun MapState.addLayer(layer: Layer, aboveLayer: LayerPlacement = AboveAll) {
    // TODO
}

fun MapState.moveLayerUp(layerId: String) {
    // TODO
}

fun MapState.moveLayerDown(layerId: String) {
    // TODO
}

/**
 * Reorder layers in the order of the provided list of ids.
 * Existing layers not included in the provided list will be removed
 */
fun MapState.reorderLayers(layerIds: List<String>) {
    // TODO
}

/**
 * Remove all layers - keeps the primary layer.
 */
fun MapState.removeLayers() {
    tileCanvasState.setLayers(listOf())
    refresh()
}

/**
 * Remove some layers - keeps the primary layer.
 */
fun MapState.removeLayers(layerIds: List<String>) {
    val remainingLayers = tileCanvasState.layerFlow.value.filterNot {
        it.id in layerIds
    }
    tileCanvasState.setLayers(remainingLayers)
    refresh()
}

fun MapState.setLayerOpacity(layerId: String, opacity: Float) {
    tileCanvasState.layerFlow.value.firstOrNull { it.id == layerId}?.apply {
        alpha.value = opacity.coerceIn(0f..1f)
    }
}


