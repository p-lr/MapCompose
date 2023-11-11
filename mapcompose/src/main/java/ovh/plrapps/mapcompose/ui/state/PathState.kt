package ovh.plrapps.mapcompose.ui.state

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.paths.generatePath
import ovh.plrapps.mapcompose.ui.paths.model.Cap
import ovh.plrapps.mapcompose.utils.Point
import ovh.plrapps.mapcompose.utils.dpToPx

internal class PathState(
    val fullWidth: Int,
    val fullHeight: Int
) {
    val pathState = mutableStateMapOf<String, DrawablePathState>()

    var pathClickCb: PathClickCb? = null
    var pathClickTraversalCb: PathClickTraversalCb? = null

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
        offset: Int?,
        count: Int?,
        cap: Cap,
        simplify: Float?,
        clickable: Boolean,
        zIndex: Float
    ) {
        if (hasPath(id)) return
        pathState[id] = DrawablePathState(id, path, width, color, offset, count, cap, simplify, clickable, zIndex)
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
        offset: Int? = null,
        count: Int? = null,
        cap: Cap? = null,
        simplify: Float? = null,
        clickable: Boolean? = null,
        zIndex: Float? = null
    ) {
        pathState[id]?.apply {
            val path = this
            pathData?.also { path.pathData = it }
            visible?.also { path.visible = it }
            width?.also { path.width = it }
            color?.also { path.color = it }
            cap?.also { path.cap = it }
            simplify?.also { path.simplify = it.coerceAtLeast(0f) }
            if (offset != null || count != null || pathData != null) {
                offsetAndCount = coerceOffsetAndCount(offset, count)
            }
            clickable?.also { path.isClickable = it }
            zIndex?.also { path.zIndex = it }
        }
    }

    fun hasPath(id: String): Boolean {
        return pathState.keys.contains(id)
    }

    /**
     * [x], [y] are the relative coordinates of the tap.
     */
    fun onHit(x: Double, y: Double, scale: Float): Boolean {
        if (!hasClickable.value) return false

        /* Compute pixel coordinates, at scale 1 because path coordinates (see below) are at scale 1 */
        val xPx = (x * fullWidth).toFloat()
        val yPx = (y * fullHeight).toFloat()

        val radius = dpToPx(12f)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            /* The strokeWidth value is set to a fixed value greater than 1f, so that the path
             * built with getFillPath (see below) is non-hairline. If the path is hairline, the
             * behavior of Path.op(..) changes. In other words, getFillPath must return true. */
            strokeWidth = 2f
        }

        val traversalClickIds = mutableListOf<String>()
        var traversalClickPosition: Point? = null
        for ((id, pathState) in pathState.entries.sortedByDescending { it.value.zIndex }) {
            if (!pathState.isClickable) continue
            val touchPath = Path()
            touchPath.addCircle(xPx, yPx, radius / scale, Path.Direction.CW)

            val path = pathState.lastRenderedPath
            val pathCopy = Path()
            paint.getFillPath(path, pathCopy)

            val intersectOpSuccess = touchPath.op(pathCopy, Path.Op.INTERSECT)
            val bounds = RectF()
            if (intersectOpSuccess) {
                touchPath.computeBounds(bounds, true)
            }

            if (!bounds.isEmpty) {
                val xOnPath = (bounds.centerX() / fullWidth).toDouble()
                val yOnPath = (bounds.centerY() / fullHeight).toDouble()

                if (pathClickTraversalCb == null) {
                    pathClickCb?.invoke(id, xOnPath, yOnPath)
                    return true
                } else {
                    traversalClickIds.add(id)
                    if (traversalClickPosition == null) {
                        traversalClickPosition = Point(xOnPath, yOnPath)
                    }
                }
            }
        }

        return if (pathClickTraversalCb == null) {
            false
        } else {
            if (traversalClickIds.isNotEmpty()) {
                val pos = traversalClickPosition
                if (pos != null) {  // should always be true
                    pathClickTraversalCb?.invoke(traversalClickIds, pos.x, pos.y)
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

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            /* The strokeWidth value is set to a fixed value greater than 1f, so that the path
             * built with getFillPath (see below) is non-hairline. If the path is hairline, the
             * behavior of Path.op(..) changes. In other words, getFillPath must return true. */
            strokeWidth = 2f
        }

        val touchPath = Path()
        touchPath.addCircle(xPx, yPx, rangePx.toFloat(), Path.Direction.CW)

        if (drawablePathState.lastRenderedPath.isEmpty) {
            /* Do this synchronously the first time only. This is necessary when the MapState is
             * used without any rendering. */
            drawablePathState.lastRenderedPath = generatePath(
                pathData = drawablePathState.pathData,
                offset = drawablePathState.offsetAndCount.x,
                count = drawablePathState.offsetAndCount.y,
                simplify = drawablePathState.simplify,
                scale = 1f
            )
        }
        val path = drawablePathState.lastRenderedPath
        val pathCopy = Path()
        paint.getFillPath(path, pathCopy)

        val intersectOpSuccess = touchPath.op(pathCopy, Path.Op.INTERSECT)
        val bounds = RectF()
        if (intersectOpSuccess) {
            touchPath.computeBounds(bounds, true)
        }

        return !bounds.isEmpty
    }
}

internal class DrawablePathState(
    val id: String,
    pathData: PathData,
    width: Dp?,
    color: Color?,
    offset: Int?,
    count: Int?,
    cap: Cap,
    simplify: Float?,
    clickable: Boolean,
    zIndex: Float
) {
    var lastRenderedPath: Path = Path()
    var pathData by mutableStateOf(pathData)
    var visible by mutableStateOf(true)
    var width: Dp by mutableStateOf(width ?: 4.dp)
    var color: Color by mutableStateOf(color ?: Color(0xFF448AFF))
    var cap: Cap by mutableStateOf(cap)
    var isClickable: Boolean by mutableStateOf(clickable)
    var zIndex: Float by mutableFloatStateOf(zIndex)

    /**
     * The "count" is the number of values in [pathData] to process, after skipping "offset" of them.
     */
    var offsetAndCount: IntOffset by mutableStateOf(initializeOffsetAndCount(offset, count))
    var simplify: Float by mutableFloatStateOf(simplify?.coerceAtLeast(0f) ?: 1f)

    private val _paint = Paint().apply {// Create this only once
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    val paint: Paint by derivedStateOf(
        /* Native Paint objects are considered equals when they have the same effect on text
         * measurement. However, we're mutating some properties which does not affect text
         * measurement, such as color or strokeCap. */
        policy = neverEqualPolicy()
    ) {
        _paint.apply {
            this.color = this@DrawablePathState.color.toArgb()
            strokeCap = when (this@DrawablePathState.cap) {
                Cap.Butt -> Paint.Cap.BUTT
                Cap.Round -> Paint.Cap.ROUND
                Cap.Square -> Paint.Cap.SQUARE
            }
        }
    }

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
internal typealias PathClickTraversalCb = (ids: List<String>, x: Double, y: Double) -> Unit