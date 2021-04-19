package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.ui.state.MapComposeState

var MapComposeState.shouldLoopScale
    get() = zoomPanRotateState.shouldLoopScale
    set(value) {
        zoomPanRotateState.shouldLoopScale = value
    }

