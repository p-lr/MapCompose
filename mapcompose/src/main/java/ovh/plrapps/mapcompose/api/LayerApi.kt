package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.Layer
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
 */
fun MapState.setLayers(layers: List<Layer>) {
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


