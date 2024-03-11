package ovh.plrapps.mapcompose.ui.state

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.ui.gestures.model.HitType
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.paths.model.Cap
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem
import ovh.plrapps.mapcompose.utils.Point
import ovh.plrapps.mapcompose.utils.dpToPx
import ovh.plrapps.mapcompose.utils.getDistance
import ovh.plrapps.mapcompose.utils.getDistanceFromBox
import ovh.plrapps.mapcompose.utils.getNearestPoint
import ovh.plrapps.mapcompose.utils.isInsideBox

internal class PathState(
    val fullWidth: Int,
    val fullHeight: Int
) {
    val pathState = mutableStateMapOf<String, DrawablePathState>()

    var pathClickCb: PathClickCb? = null
    var pathHitTraversalCb: PathHitTraversalCb? = null
    var pathLongPressCb: PathClickCb? = null

    private val hasClickable = derivedStateOf {
        pathState.values.any {
            it.isClickable
        }
    }

    fun addPath(
        id: String,
        path: PathData,
        width: Dp?,
        color: Color?,
        fillColor: Color?,
        offset: Int?,
        count: Int?,
        cap: Cap,
        simplify: Float?,
        clickable: Boolean,
        zIndex: Float,
        pattern: List<PatternItem>?
    ) {
        if (hasPath(id)) return
        pathState[id] = DrawablePathState(id, path, width, color,fillColor, offset, count, cap, simplify, clickable, zIndex, pattern)
    }

    fun removePath(id: String): Boolean {
        return pathState.remove(id) != null
    }

    fun removeAllPaths() {
        pathState.clear()
    }

    fun updatePath(
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
        pathState[id]?.apply {
            val path = this
            pathData?.also { path.pathData = it }
            visible?.also { path.visible = it }
            width?.also { path.width = it }
            color?.also { path.color = it }
            fillColor?.also { path.fillColor = it }
            cap?.also { path.cap = it }
            simplify?.also { path.simplify = it.coerceAtLeast(0f) }
            if (offset != null || count != null || pathData != null) {
                offsetAndCount = coerceOffsetAndCount(offset, count)
            }
            clickable?.also { path.isClickable = it }
            zIndex?.also { path.zIndex = it }
            pattern?.also { path.pattern = it }
        }
    }

    fun hasPath(id: String): Boolean {
        return pathState.keys.contains(id)
    }

    /**
     * [x], [y] are the relative coordinates of the tap.
     */
    fun onHit(x: Double, y: Double, scale: Float, hitType: HitType): Boolean {
        if (!hasClickable.value) return false

        /* Compute pixel coordinates, at scale 1 because path coordinates (see below) are at scale 1 */
        val xPx = (x * fullWidth).toFloat()
        val yPx = (y * fullHeight).toFloat()

        val radius = dpToPx(12f)
        val threshold = radius / scale

        val traversalClickIds = mutableListOf<String>()
        var traversalClickPosition: Point? = null
        val candidates = pathState.entries
            .filter { it.value.isClickable }
            .sortedByDescending { it.value.zIndex }

        for ((id, pathState) in candidates) {

            val bb = pathState.pathData.boundingBox ?: continue
            val (topLeft, bottomRight) = bb
            val (xMin, yMin) = topLeft
            val (xMax, yMax) = bottomRight

            /* Don't compute the nearest point for a point outside of the bounding box and with a
             * distance to the bounding box greater than the threshold */
            if (!isInsideBox(xPx, yPx, xMin, xMax, yMin, yMax) && getDistanceFromBox(xPx, yPx, xMin, xMax, yMin, yMax) > threshold) {
                continue
            }

            var d = Float.MAX_VALUE
            var nearestP1: Offset? = null
            var nearestP2: Offset? = null
            for (i in 0 until pathState.pathData.data.size) {
                if (i + 1 == pathState.pathData.data.size) break
                val p1 = pathState.pathData.data[i]
                val p2 = pathState.pathData.data[i + 1]
                val dist = getDistance(xPx, yPx, p1.x, p1.y, p2.x, p2.y)
                if (dist < threshold && dist < d) {
                    d = dist
                    nearestP1 = p1
                    nearestP2 = p2
                }
            }

            if (nearestP1 != null && nearestP2 != null) {
                val nearest =
                    getNearestPoint(xPx, yPx, nearestP1.x, nearestP1.y, nearestP2.x, nearestP2.y)
                val xOnPath = (nearest.x / fullWidth).toDouble()
                val yOnPath = (nearest.y / fullHeight).toDouble()

                if (pathHitTraversalCb == null) {
                    when (hitType) {
                        HitType.Click -> pathClickCb?.invoke(id, xOnPath, yOnPath)
                        HitType.LongPress -> pathLongPressCb?.invoke(id, xOnPath, yOnPath)
                    }
                    return true
                } else {
                    traversalClickIds.add(id)
                    if (traversalClickPosition == null) {
                        traversalClickPosition = Point(xOnPath, yOnPath)
                    }
                }
            }
        }

        return if (pathHitTraversalCb == null) {
            false
        } else {
            if (traversalClickIds.isNotEmpty()) {
                val pos = traversalClickPosition
                if (pos != null) {  // should always be true
                    pathHitTraversalCb?.invoke(traversalClickIds, pos.x, pos.y, hitType)
                }
                true
            } else false
        }
    }

    /**
     * The scale doesn't matter as all computations are done at scale 1.
     */
    fun isPathWithinRange(id: String, rangePx: Int, x: Double, y: Double): Boolean {
        val drawablePathState = pathState[id] ?: return false

        /* Compute pixel coordinates, at scale 1 because path coordinates (see below) are at scale 1 */
        val xPx = (x * fullWidth).toFloat()
        val yPx = (y * fullHeight).toFloat()

        for (i in 0 until drawablePathState.pathData.data.size) {
            if (i + 1 == drawablePathState.pathData.data.size) break
            val p1 = drawablePathState.pathData.data[i]
            val p2 = drawablePathState.pathData.data[i + 1]
            val dist = getDistance(xPx, yPx, p1.x, p1.y, p2.x, p2.y)
            if (dist < rangePx) {
                return true
            }
        }

        return false
    }
}

internal class DrawablePathState(
    val id: String,
    pathData: PathData,
    width: Dp?,
    color: Color?,
    fillColor: Color?,
    offset: Int?,
    count: Int?,
    cap: Cap,
    simplify: Float?,
    clickable: Boolean,
    zIndex: Float,
    pattern: List<PatternItem>?
) {
    var lastRenderedPath: Path = Path()
    var pathData by mutableStateOf(pathData)
    var visible by mutableStateOf(true)
    var width: Dp by mutableStateOf(width ?: 4.dp)
    var color: Color by mutableStateOf(color ?: Color(0xFF448AFF))
    var fillColor: Color? by mutableStateOf(fillColor)
    var cap: Cap by mutableStateOf(cap)
    var isClickable: Boolean by mutableStateOf(clickable)
    var zIndex: Float by mutableFloatStateOf(zIndex)
    var pattern: List<PatternItem>? by mutableStateOf(pattern)

    /**
     * The "count" is the number of values in [pathData] to process, after skipping "offset" of them.
     */
    var offsetAndCount: IntOffset by mutableStateOf(initializeOffsetAndCount(offset, count))
    var simplify: Float by mutableFloatStateOf(simplify?.coerceAtLeast(0f) ?: 1f)

    private fun initializeOffsetAndCount(offset: Int?, cnt: Int?): IntOffset {
        val ofst = offset?.coerceIn(0, pathData.data.size) ?: 0
        val count = cnt?.coerceIn(
            0, (pathData.data.size - ofst)
        ) ?: pathData.data.size
        return IntOffset(ofst, count)
    }

    /**
     * Ensure that "count" + "offset" shouldn't exceed the path length.
     */
    fun coerceOffsetAndCount(offset: Int?, cnt: Int?): IntOffset {
        val ofst = (offset ?: offsetAndCount.x).coerceIn(0, pathData.data.size)
        val count = (cnt ?: offsetAndCount.y).coerceIn(0, (pathData.data.size - ofst))
        return IntOffset(ofst, count)
    }

    override fun hashCode(): Int {
        var hash = id.hashCode()
        hash += 31 * pathData.data.size

        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DrawablePathState
        if (id != other.id) return false
        if (pathData.data.size != other.pathData.data.size) return false

        return true
    }
}

internal typealias PathClickCb = (id: String, x: Double, y: Double) -> Unit
internal typealias PathHitTraversalCb = (ids: List<String>, x: Double, y: Double, hitType: HitType) -> Unit