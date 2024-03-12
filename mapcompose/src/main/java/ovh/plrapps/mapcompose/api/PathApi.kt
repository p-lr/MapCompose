@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ovh.plrapps.mapcompose.ui.gestures.model.HitType
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.ui.paths.model.Cap
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * Adds a path, optionally setting some properties.
 *
 * @param id The unique identifier of the path
 * @param pathData Obtained from [PathDataBuilder.build]
 * @param width The width of the path, in [Dp]. Defaults to 4.dp
 * @param color The color of the path. Defaults to Color(0xFF448AFF)
 * @param fillColor If set - the path will be filled and the area drawn in this color.
 * @param offset The number of points to skip from the beginning of the path. Defaults to 0.
 * @param count The number of points to draw after [offset]. Defaults to the number of points added
 * to built [pathData].
 * @param cap The cap style at the start and end of the path. Defaults to [Cap.Round].
 * @param simplify By default, the path is simplified depending on the scale to improve performance.
 * Higher values increase the simplification effect, while a value of 0f effectively disables path
 * simplification. Sensible values a typically in the range [0.5f..2f]. Default value is 1f.
 * @param clickable Controls whether the path is clickable. Default is false. If a click listener
 * is registered using [onPathClick], that listener will be invoked for that marker if [clickable]
 * is true.
 * @param zIndex A path with larger zIndex will be drawn on top of paths with smaller zIndex.
 * When paths have the same zIndex, the more recently added path is drawn on top of the others.
 * @param pattern The dash pattern. By default, no dash effect is applied.
 */
fun MapState.addPath(
    id: String,
    pathData: PathData,
    width: Dp? = null,
    color: Color? = null,
    fillColor: Color? = null,
    offset: Int? = null,
    count: Int? = null,
    cap: Cap = Cap.Round,
    simplify: Float? = null,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    pattern: List<PatternItem>? = null
) {
    pathState.addPath(id, pathData, width, color, fillColor, offset, count, cap, simplify, clickable, zIndex, pattern)
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
 * @param clickable Controls whether the path is clickable. Default is false. If a click listener
 * is registered using [onPathClick], that listener will be invoked for that marker if [clickable]
 * is true.
 * @param zIndex A path with larger zIndex will be drawn on top of paths with smaller zIndex.
 * When paths have the same zIndex, the more recently added path is drawn on top of the others.
 * @param pattern The dash pattern. By default, no dash effect is applied.
 * @param builder The builder block from with to add individual points or list of points.
 *
 * @return The [PathData] which can be used for adding other paths.
 */
fun MapState.addPath(
    id: String,
    width: Dp? = null,
    color: Color? = null,
    fillColor: Color? = null,
    offset: Int? = null,
    count: Int? = null,
    cap: Cap = Cap.Round,
    simplify: Float? = null,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    pattern: List<PatternItem>? = null,
    builder: (PathDataBuilder).() -> Unit
): PathData? {
    val pathData = makePathDataBuilder().apply { builder() }.build() ?: return null
    pathState.addPath(id, pathData, width, color, fillColor, offset, count, cap, simplify, clickable, zIndex, pattern)
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
 * @param zIndex A path with larger zIndex will be drawn on top of paths with smaller zIndex.
 * When paths have the same zIndex, the more recently added path is drawn on top of the others.
 * @param clickable Controls whether the path is clickable.
 * @param pattern The dash pattern. By default, no dash effect is applied.
 */
fun MapState.updatePath(
    id: String,
    pathData: PathData? = null,
    visible: Boolean? = null,
    width: Dp? = null,
    color: Color? = null,
    fillColor: Color? = null,
    offset: Int? = null,
    count: Int? = null,
    cap: Cap? = null,
    simplify: Float? = null,
    clickable: Boolean? = null,
    zIndex: Float? = null,
    pattern: List<PatternItem>? = null
) {
    pathState.updatePath(id, pathData, visible, width, color, fillColor, offset, count, cap, simplify, clickable, zIndex, pattern)
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

/**
 * Register a callback which will be invoked when a path is tapped.
 * Beware that this click listener will only be invoked if at least one path is clickable, and when
 * the click gesture isn't already consumed by some other composable (like a button), or a marker.
 * When several paths hover each other, the [cb] is invoked for the path with the highest z-index.
 */
fun MapState.onPathClick(cb: (id: String, x: Double, y: Double) -> Unit) {
    pathState.pathClickCb = cb
}

/**
 * Register a callback which will be invoked when a path is long-clicked.
 * Beware that the provided callback will only be invoked if at least one path is clickable, and when
 * the gesture isn't already consumed by some other composable (like a button), or a marker.
 * When several paths hover each other, the [cb] is invoked for the path with the highest z-index.
 */
fun MapState.onPathLongPress(cb: (id: String, x: Double, y: Double) -> Unit) {
    pathState.pathLongPressCb = cb
}

/**
 * Register a callback which will be invoked when one or more paths are tapped.
 * /!\ This api takes precedence over the [onPathClick] and [onPathLongPress] apis. For example,
 * when [onPathHitTraversal] is set, the callback registered with [onPathClick] isn't invoked.
 * Beware that this click listener will only be invoked if at least one path is clickable, and when
 * the click gesture isn't already consumed by some other composable (like a button), or a marker.
 * When several paths hover each other, the [cb] is invoked for all paths, regardless of their
 * z-index.
 *
 * To unregister the callback, set it to null.
 */
fun MapState.onPathHitTraversal(cb: ((ids: List<String>, x: Double, y: Double, hitType: HitType) -> Unit)?) {
    pathState.pathHitTraversalCb = cb
}

/**
 * When application code lost reference on a [PathData], this api can be useful to retrieve the
 * [PathData] instance.
 * A typical use case is to draw a new path on top or underneath the path with id [id].
 */
fun MapState.getPathData(id: String): PathData? {
    return pathState.pathState[id]?.pathData
}

/**
 * Loops on all paths and snapshots each path properties.
 * Useful to loop and update paths depending on their properties.
 */
fun MapState.allPaths(block: MapState.(properties: PathProperties) -> Unit) {
    pathState.pathState.values.forEach { drawablePathState ->
        val properties = PathProperties(
            id = drawablePathState.id,
            visible = drawablePathState.visible,
            width = drawablePathState.width,
            color = drawablePathState.color,
            offset = drawablePathState.offsetAndCount.x,
            count = drawablePathState.offsetAndCount.y,
            cap = drawablePathState.cap,
            simplify = drawablePathState.simplify,
            clickable = drawablePathState.isClickable,
            zIndex = drawablePathState.zIndex
        )
        block(properties)
    }
}

/**
 * Checks if a circle centered on ([x], [y]) with a radius of [rangePx] at scale 1 intersects the
 * path with id = [id].
 */
fun MapState.isPathWithinRange(id: String, rangePx: Int, x: Double, y: Double): Boolean {
    return pathState.isPathWithinRange(id, rangePx, x, y)
}

/**
 * Removes the dash effect of a path.
 */
fun MapState.removePathPattern(id: String) {
    pathState.pathState[id]?.apply {
        pattern = null
    }
}

data class PathProperties(
    val id: String, val visible: Boolean, val width: Dp, val color: Color, val offset: Int,
    val count: Int, val cap: Cap, val simplify: Float, val clickable: Boolean,
    val zIndex: Float
)
