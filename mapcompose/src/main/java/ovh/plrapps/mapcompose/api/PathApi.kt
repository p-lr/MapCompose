@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.ui.paths.model.Cap
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * Adds a path, optionally setting some properties.
 *
 * @param id The unique identifier of the path
 * @param pathData Obtained from [PathDataBuilder.build]
 * @param width The width of the path, in [Dp]. Defaults to 4.dp
 * @param color The color of the path. Defaults to Color(0xFF448AFF)
 * @param offset The number of points to skip from the beginning of the path. Defaults to 0.
 * @param count The number of points to draw after [offset]. Defaults to the number of points added
 * to built [pathData].
 * @param cap The cap style at the start and end of the path. Defaults to [Cap.Round].
 * @param simplify By default, the path is simplified depending on the scale to improve performance.
 * Higher values increase the simplification effect, while a value of 0f effectively disables path
 * simplification. Sensible values a typically in the range [0.5f..2f]. Default value is 1f.
 */
fun MapState.addPath(
    id: String,
    pathData: PathData,
    width: Dp? = null,
    color: Color? = null,
    offset: Int? = null,
    count: Int? = null,
    cap: Cap = Cap.Round,
    simplify: Float? = null
) {
    pathState.addPath(id, pathData, width, color, offset, count, cap, simplify)
}

/**
 * Adds a path, optionally setting some properties.
 *
 * @param id The unique identifier of the path
 * @param width The width of the path, in [Dp]. Defaults to 4.dp
 * @param color The color of the path. Defaults to Color(0xFF448AFF)
 * @param offset The number of points to skip from the beginning of the path. Defaults to 0.
 * @param count The number of points to draw after [offset]. Defaults to the number of points
 * provided inside the [builder] block.
 * @param cap The cap style at the start and end of the path. Defaults to [Cap.Round].
 * @param simplify By default, the path is simplified depending on the scale to improve performance.
 * Higher values increase the simplification effect, while a value of 0f effectively disables path
 * simplification. Sensible values a typically in the range [0.5f..2f]. Default value is 1f.
 * @param builder The builder block from with to add individual points or list of points.
 *
 * @return The [PathData] which can be used for adding other paths.
 */
fun MapState.addPath(
    id: String,
    width: Dp? = null,
    color: Color? = null,
    offset: Int? = null,
    count: Int? = null,
    cap: Cap = Cap.Round,
    simplify: Float? = null,
    builder: (PathDataBuilder).() -> Unit
): PathData? {
    val pathData = makePathDataBuilder().apply { builder() }.build() ?: return null
    pathState.addPath(id, pathData, width, color, offset, count, cap, simplify)
    return pathData
}

/**
 * Updates some properties of a previously added path (using [addPath]).
 *
 * @param id The unique identifier of the path
 * @param pathData The points of the path. The [PathDataBuilder] which was originally used to create
 * the path can be used to build new [PathData] instances with additional points.
 * @param width The width of the path, in [Dp]
 * @param color The color of the path
 * @param offset The number of points to skip from the beginning of the path
 * @param count The number of points to draw after [offset]
 * @param cap The cap style at the start and end of the path
 * @param simplify By default, the path is simplified depending on the scale to improve performance.
 * Higher values increase the simplification effect, while a value of 0f effectively disables path
 * simplification. Sensible values a typically in the range [0.5f..2f]. Default value is 1f.
 */
fun MapState.updatePath(
    id: String,
    pathData: PathData? = null,
    visible: Boolean? = null,
    width: Dp? = null,
    color: Color? = null,
    offset: Int? = null,
    count: Int? = null,
    cap: Cap? = null,
    simplify: Float? = null
) {
    pathState.updatePath(id, pathData, visible, width, color, offset, count, cap, simplify)
}

/**
 * Removes a path.
 *
 * @param id The id of the path
 */
fun MapState.removePath(id: String) {
    pathState.removePath(id)
}

/**
 * Removes all paths.
 */
fun MapState.removeAllPaths() {
    pathState.removeAllPaths()
}

/**
 * Check whether a path was already added or not.
 *
 * @param id The id of the path
 */
fun MapState.hasPath(id: String): Boolean {
    return pathState.hasPath(id)
}

/**
 * Get a new instance of [PathDataBuilder].
 * Adding a path is done using [addPath], which requires a [PathData] instance. A [PathData]
 * instance can only be built using a [PathDataBuilder].
 */
fun MapState.makePathDataBuilder(): PathDataBuilder {
    return PathDataBuilder(zoomPanRotateState.fullWidth, zoomPanRotateState.fullHeight)
}
