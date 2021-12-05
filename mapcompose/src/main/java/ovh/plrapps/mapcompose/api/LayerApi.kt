@file:Suppress("unused")

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

/**
 * Add a layer. By default, the layer is added on top of the layer stack (see [AboveAll]).
 * Optionally, the layer can be added at the bottom of the stack, or above / below an existing layer.
 */
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
    refresh()
}

/**
 * Moves a layer up in the layer stack, making it drawn on top of the layer that was previously
 * above it.
 */
fun MapState.moveLayerUp(layerId: String) {
    val layers = tileCanvasState.layerFlow.value.toMutableList()

    val index = layers.indexOfFirst {
        it.id == layerId
    }

    if (index > 0 && index < layers.lastIndex) {
        Collections.swap(layers, index + 1, index)
        tileCanvasState.setLayers(layers)
        refresh()
    }
}

/**
 * Moves a layer down in the layer stack, making it drawn below the layer that was previously
 * below it.
 */
fun MapState.moveLayerDown(layerId: String) {
    val layers = tileCanvasState.layerFlow.value.toMutableList()

    val index = layers.indexOfFirst {
        it.id == layerId
    }

    /* The primary layer should remain first */
    if (index > 1) {
        Collections.swap(layers, index - 1, index)
        tileCanvasState.setLayers(layers)
        refresh()
    }
}

/**
 * Reorder layers in the order of the provided list of ids. Layers listed first will be drawn before
 * subsequent layers (so the later will be above).
 * Existing layers not included in the provided list will be removed
 */
fun MapState.reorderLayers(layerIds: List<String>) {
    val layerForId = tileCanvasState.layerFlow.value.filter {
        it.id in layerIds
    }.associateBy { it.id }
    val layers = layerIds.mapNotNull { layerForId[it] }

    tileCanvasState.setLayers(layers)
    refresh()
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


