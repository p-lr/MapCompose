package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

internal class MarkerState {
    internal val markers = mutableStateMapOf<String, MarkerData>()

    fun addMarker(id: String, x: Double, y: Double, relativeOffset: Offset, absoluteOffset: Offset,
                  c: @Composable () -> Unit) {
        markers[id] = MarkerData(id, x, y, relativeOffset, absoluteOffset, c)
    }

    fun removeMarker(id: String): Boolean {
        return markers.remove(id) != null
    }

    fun moveMarker(id: String, x: Double, y: Double) {
        markers[id]?.apply {
            this.x = x
            this.y = y
        }
    }

    internal fun onMarkerClick(data: MarkerData) {
        println("marker click $data")
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