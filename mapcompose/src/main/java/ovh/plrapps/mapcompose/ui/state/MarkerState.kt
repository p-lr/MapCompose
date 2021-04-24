package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

internal class MarkerState {
    internal val markers = mutableStateMapOf<String, MarkerData>()
    internal var markerMoveCb: MarkerMoveCb? = null
    internal var markerClickCb: MarkerClickCb? = null

    fun addMarker(id: String, x: Double, y: Double, relativeOffset: Offset, absoluteOffset: Offset,
                  c: @Composable () -> Unit) {
        markers[id] = MarkerData(id, x, y, relativeOffset, absoluteOffset, c)
    }

    fun removeMarker(id: String): Boolean {
        return markers.remove(id) != null
    }

    fun moveMarkerTo(id: String, x: Double, y: Double) {
        val marker = markers[id] ?: return
        with(marker) {
            val prevX = x
            val prevY = y
            this.x = x.coerceIn(0.0, 1.0)
            this.y = y.coerceIn(0.0, 1.0)
            onMarkerMove(this, this.x - prevX, this.y - prevY)
        }
    }

    /**
     * Move a marker by the provided delta (normalized) coordinates.
     */
    fun moveMarkerBy(id: String, deltaX: Double, deltaY: Double) {
        markers[id]?.apply {
            x = (x + deltaX).coerceIn(0.0, 1.0)
            y = (y + deltaY).coerceIn(0.0, 1.0)
        }.also {
            if (it != null) onMarkerMove(it, deltaX, deltaY)
        }
    }

    /**
     * If set, drag gestures will be handled for the marker identifiable by the [id].
     */
    fun setDraggable(id: String, draggable: Boolean) {
        markers[id]?.isDraggable = draggable
    }

    private fun onMarkerMove(data: MarkerData, dx: Double, dy: Double) {
        markerMoveCb?.invoke(data.id, data.x, data.y, dx, dy)
    }

    internal fun onMarkerClick(data: MarkerData) {
        markerClickCb?.invoke(data.id, data.x, data.y)
    }
}

internal class MarkerData(
    val id: String,
    x: Double, y: Double,
    val relativeOffset: Offset,
    val absoluteOffset: Offset,
    val c: @Composable () -> Unit
) {
    var x: Double by mutableStateOf(x)
    var y: Double by mutableStateOf(y)
    var isDraggable by mutableStateOf(false)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MarkerData

        if (id != other.id) return false
        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}

internal typealias MarkerMoveCb = (id: String, x: Double, y: Double, dx: Double, dy: Double) -> Unit
internal typealias MarkerClickCb = (id: String, x: Double, y: Double) -> Unit