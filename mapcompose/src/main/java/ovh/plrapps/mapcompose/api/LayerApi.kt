package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.Layer
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState



fun MapState.setTileStreamProvider(tileStreamProvider: TileStreamProvider) {
    setLayers(listOf(Layer(mainLayerId, tileStreamProvider)))
}

const val mainLayerId: String = "mainLayer"

