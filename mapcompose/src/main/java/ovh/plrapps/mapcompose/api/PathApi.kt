package ovh.plrapps.mapcompose.api

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ovh.plrapps.mapcompose.demo.ui.paths.PathData
import ovh.plrapps.mapcompose.demo.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.demo.ui.state.MapState

fun MapState.PathDataBuilder(): PathDataBuilder {
    return PathDataBuilder(zoomPanRotateState.fullWidth, zoomPanRotateState.fullHeight)
}

fun MapState.addPath(id: String, data: PathData) {
    pathState.addPath(id, data)
}

fun MapState.updatePath(
    id: String,
    pathData: PathData? = null,
    visible: Boolean? = null,
    width: Dp? = null,
    color: Color? = null,
    offset: Int? = null
) {
    pathState.updatePath(id, pathData, visible, width, color, offset)
}

fun MapState.removePath(id: String) {
    pathState.removePath(id)
}