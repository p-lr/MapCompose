package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset

internal class MarkerState {
    internal val markers = mutableStateMapOf<String, MarkerData>()

    fun addMarker(id: String, x: Double, y: Double, relativeOffset: Offset, absoluteOffset: Offset,
                  c: @Composable () -> Unit) {
        markers[id] = MarkerData(x, y, relativeOffset, absoluteOffset, c)
    }
}

internal data class MarkerData(val x: Double, val y: Double,
                               val relativeOffset: Offset,
                               val absoluteOffset: Offset,
                               val c: @Composable () -> Unit)