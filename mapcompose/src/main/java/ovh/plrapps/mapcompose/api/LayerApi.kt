package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*

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
    val layers = tileCanvasState.layerFlow.value.toMutableList()

    val newLayers = when (aboveLayer) {
        AboveAll -> {
            layers.add(0, layer)
            layers
        }
        is AboveLayer -> {
            val existingLayerIndex = layers.indexOfFirst { it.id == aboveLayer.layerId }
            if (existingLayerIndex != -1 && existingLayerIndex < layers.lastIndex) {
                layers.add(existingLayerIndex + 1, layer)
            }
            layers
        }
        BelowAll -> {
            layers + layer
        }
        is BelowLayer -> {
            val existingLayerIndex = layers.indexOfFirst { it.id == aboveLayer.layerId }
            if (existingLayerIndex != -1) {
                layers.add(existingLayerIndex, layer)
            }
            layers
        }
    }

    tileCanvasState.setLayers(newLayers)
}

fun MapState.moveLayerUp(layerId: String) {
    val layers = tileCanvasState.layerFlow.value.toMutableList()

    val index = layers.indexOfFirst {
        it.id == layerId
    }

    if (index > 0 && index < layers.lastIndex) {
        Collections.swap(layers, index + 1, index)
        tileCanvasState.setLayers(layers)
    }
}

fun MapState.moveLayerDown(layerId: String) {
    val layers = tileCanvasState.layerFlow.value.toMutableList()

    val index = layers.indexOfFirst {
        it.id == layerId
    }

    if (index > 0) {
        Collections.swap(layers, index - 1, index)
        tileCanvasState.setLayers(layers)
    }
}

/**
 * Reorder layers in the order of the provided list of ids.
 * Existing layers not included in the provided list will be removed
 */
fun MapState.reorderLayers(layerIds: List<String>) {
    val layerForId = tileCanvasState.layerFlow.value.filter {
        it.id in layerIds
    }.associateBy { it.id }
    val layers = layerIds.mapNotNull { layerForId[it] }

    tileCanvasState.setLayers(layers)
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


