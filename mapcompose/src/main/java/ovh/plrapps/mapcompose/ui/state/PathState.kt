package ovh.plrapps.mapcompose.ui.state

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import ovh.plrapps.mapcompose.ui.paths.model.Cap

internal class PathState {
    val pathState = mutableStateMapOf<String, DrawablePathState>()

    fun addPath(
        id: String,
        path: PathData,
        width: Dp?,
        color: Color?,
        offset: Int?,
        count: Int?,
        cap: Cap,
        simplify: Float?
    ) {
        if (hasPath(id)) return
        pathState[id] = DrawablePathState(id, path, width, color, offset, count, cap, simplify)
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
        simplify: Float? = null
    ) {
        pathState[id]?.apply {
            val path = this
            pathData?.also { path.pathData = it }
            visible?.also { path.visible = it }
            width?.also { path.width = it }
            color?.also { path.color = it }
            cap?.also { path.cap = it }
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
    cap: Cap,
    simplify: Float?
) {
    var lastRenderedPath: Path = Path()
    var pathData by mutableStateOf(pathData)
    var visible by mutableStateOf(true)
    var width: Dp by mutableStateOf(width ?: 4.dp)
    var color: Color by mutableStateOf(color ?: Color(0xFF448AFF))
    var cap: Cap by mutableStateOf(cap)

    /**
     * The "count" is the number of values in [pathData] to process, after skipping "offset" of them.
     */
    var offsetAndCount: IntOffset by mutableStateOf(initializeOffsetAndCount(offset, count))
    var simplify: Float by mutableStateOf(simplify?.coerceAtLeast(0f) ?: 1f)

    private val _paint = Paint().apply {// Create this only once
        style = Paint.Style.STROKE
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