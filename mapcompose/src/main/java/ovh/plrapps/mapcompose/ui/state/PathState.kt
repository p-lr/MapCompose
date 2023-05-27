package ovh.plrapps.mapcompose.ui.state

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.ui.paths.PathData

internal class PathState {
    val pathState = mutableStateMapOf<String, DrawablePathState>()

    fun addPath(
        id: String,
        path: PathData,
        width: Dp? = null,
        color: Color? = null,
        offset: Int? = null,
        count: Int? = null,
        simplify: Float? = null
    ) {
        if (hasPath(id)) return
        pathState[id] = DrawablePathState(id, path, width, color, offset, count, simplify)
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
        simplify: Float? = null
    ) {
        pathState[id]?.apply {
            val path = this
            pathData?.also { path.pathData = it }
            visible?.also { path.visible = it }
            width?.also { path.width = it }
            color?.also { path.color = it }
            simplify?.also { path.simplify = it.coerceAtLeast(0f) }
            if (offset != null || count != null) {
                offsetAndCount = coerceOffsetAndCount(offset, count)
            }
        }
    }

    fun hasPath(id: String): Boolean {
        return pathState.keys.contains(id)
    }
}

internal class DrawablePathState(
    val id: String,
    pathData: PathData,
    width: Dp?,
    color: Color?,
    offset: Int?,
    count: Int?,
    simplify: Float?
) {
    var lastRenderedPath: Path = Path()
    var pathData by mutableStateOf(pathData)
    var visible by mutableStateOf(true)
    var width: Dp by mutableStateOf(width ?: 4.dp)
    var color: Color by mutableStateOf(color ?: Color(0xFF448AFF))
    /**
     * The "count" is the number of values in [pathData] to process, after skipping "offset" of them.
     */
    var offsetAndCount: IntOffset by mutableStateOf(initializeOffsetAndCount(offset, count))
    var simplify: Float by mutableStateOf(simplify?.coerceAtLeast(0f) ?: 1f)

    private val _paint = Paint()    // Create this only once
    val paint: Paint
        get() = _paint.apply {
            color = this@DrawablePathState.color.toArgb()
            style = Paint.Style.STROKE
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
        val ofst = offset?.coerceIn(0, pathData.data.size) ?: offsetAndCount.x
        val count = cnt?.coerceIn(
            0, (pathData.data.size - ofst)
        ) ?: offsetAndCount.y
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