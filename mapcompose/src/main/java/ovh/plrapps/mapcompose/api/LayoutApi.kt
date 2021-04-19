package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.ui.state.MapState

var MapState.shouldLoopScale
    get() = zoomPanRotateState.shouldLoopScale
    set(value) {
        zoomPanRotateState.shouldLoopScale = value
    }

