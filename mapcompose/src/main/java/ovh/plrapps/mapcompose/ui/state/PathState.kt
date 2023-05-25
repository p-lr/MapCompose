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
        simplify: Boolean = true
    ) {
        pathState[id] = DrawablePathState(id, path).apply {
            val p = this
            width?.also { p.width = it }
            color?.also { p.color = it }
            p.simplify = simplify
            setOffsetAndCount(offset, count)
        }
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
        simplify: Boolean? = null
    ) {
        pathState[id]?.apply {
            val path = this
            pathData?.also { path.pathData = it }
            visible?.also { path.visible = it }
            width?.also { path.width = it }
            color?.also { path.color = it }
            simplify?.also { path.simplify = it }
            setOffsetAndCount(offset, count)
        }
    }

    /**
     * Beware that "count" + "offset" shouldn't exceed the path length, or an exception will
     * be thrown.
     */
    private fun DrawablePathState.setOffsetAndCount(offset: Int?, cnt: Int?) {
        val ofst = offset?.coerceIn(0, pathData.data.size) ?: 0
        val count = cnt?.coerceIn(
            0, (pathData.data.size - ofst)
        ) ?: pathData.data.size
        offsetAndCount = IntOffset(ofst, count)
    }
}

internal class DrawablePathState(
    val id: String,
    pathData: PathData
) {
    var lastRenderedPath: Path = Path()
    var pathData by mutableStateOf(pathData)
    var visible by mutableStateOf(true)
    var width: Dp by mutableStateOf(8.dp)
    var color: Color by mutableStateOf(Color(0xFF448AFF))
    /**
     * The "count" is the number of values in [pathData] to process, after skipping "offset" of them.
     */
    var offsetAndCount: IntOffset by mutableStateOf(IntOffset(0, pathData.data.size))
    var simplify: Boolean by mutableStateOf(true)

    private val _paint = Paint()    // Create this only once
    val paint: Paint
        get() = _paint.apply {
            color = this@DrawablePathState.color.toArgb()
            style = Paint.Style.STROKE
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