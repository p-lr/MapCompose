package ovh.plrapps.mapcompose.demo.ui.state

import android.graphics.Paint
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.demo.ui.paths.PathData

internal class PathState {
    val pathState = mutableStateMapOf<String, DrawablePathState>()

    fun addPath(id: String, path: PathData) {
        pathState[id] = DrawablePathState(id, path)
    }

    fun removePath(id: String): Boolean {
        return pathState.remove(id) != null
    }

    fun updatePath(
        id: String,
        pathData: PathData? = null,
        visible: Boolean? = null,
        width: Dp? = null,
        color: Color? = null,
        offset: Int? = null,
        count: Int? = null
    ) {
        pathState[id]?.apply {
            val path = this
            pathData?.also { path.pathData = it }
            visible?.also { path.visible = it }
            width?.also { path.width = it }
            color?.also { path.color = it }
            count?.also { path.count = it }
            offset?.also { path.offset = it }
        }
    }
}

internal class DrawablePathState(
    val id: String,
    pathData: PathData
) {
    var pathData by mutableStateOf(pathData)
    var visible by mutableStateOf(true)
    var width: Dp by mutableStateOf(8.dp)
    var color: Color by mutableStateOf(Color.Blue)
    var offset: Int by mutableStateOf(0)

    /**
     * The number of values in [pathData] to process, after skipping [offset] of them.
     * Beware that [count] + [offset] shouldn't exceed the [pathData] length, or an exception will
     * be thrown.
     */
    var count: Int by mutableStateOf(pathData.data.size)

    private val _paint = Paint()    // Create this only once
    val paint: Paint
        get() = _paint.apply {
            color = this@DrawablePathState.color.toArgb()
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