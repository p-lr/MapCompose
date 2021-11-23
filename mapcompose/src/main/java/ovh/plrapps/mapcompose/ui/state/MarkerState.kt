package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.utils.Point

internal class MarkerState {
    internal val markers = mutableStateMapOf<String, MarkerData>()
    internal var markerMoveCb: MarkerMoveCb? = null
    internal var markerClickCb: MarkerClickCb? = null

    internal val callouts = mutableStateMapOf<String, CalloutData>()
    internal var calloutClickCb: MarkerClickCb? = null

    fun addMarker(
        id: String, position: Point, relativeOffset: Offset, absoluteOffset: Offset,
        zIndex: Float, clickable: Boolean,
        c: @Composable () -> Unit
    ) {
        markers[id] = MarkerData(id, position, relativeOffset, absoluteOffset, zIndex, clickable, c)
    }

    fun addCallout(
        id: String, position: Point, relativeOffset: Offset, absoluteOffset: Offset,
        zIndex: Float, autoDismiss: Boolean, clickable: Boolean,
        c: @Composable () -> Unit
    ) {
        val markerData = MarkerData(id, position, relativeOffset, absoluteOffset, zIndex, clickable, c)
        callouts[id] = CalloutData(markerData, autoDismiss)
    }

    fun removeMarker(id: String): Boolean {
        return markers.remove(id) != null
    }

    fun removeCallout(id: String): Boolean {
        return callouts.remove(id) != null
    }

    fun removeAllAutoDismissCallouts() {
        if (callouts.isEmpty()) return
        val it = callouts.iterator()
        while (it.hasNext()) {
            if (it.next().value.autoDismiss) it.remove()
        }
    }

    fun moveMarkerTo(id: String, position: Point) {
        val marker = markers[id] ?: return
        with(marker) {
            val prev = position
            this.position = position.coerceIn(0f, 1f)
            onMarkerMove(this, position - prev)
        }
    }

    /**
     * Move a marker by the provided delta (normalized) coordinates.
     */
    fun moveMarkerBy(id: String, delta: Point) {
        markers[id]?.apply {
            position = (position + delta).coerceIn(0f, 1f)
        }.also {
            if (it != null) onMarkerMove(it, delta)
        }
    }

    /**
     * If set, drag gestures will be handled for the marker identifiable by the [id].
     */
    fun setDraggable(id: String, draggable: Boolean) {
        markers[id]?.isDraggable = draggable
    }

    private fun onMarkerMove(data: MarkerData, delta: Point) {
        markerMoveCb?.invoke(data.id, data.position, delta)
    }

    internal fun onMarkerClick(data: MarkerData) {
        markerClickCb?.invoke(data.id, data.position)
    }

    internal fun onCalloutClick(data: MarkerData) {
        calloutClickCb?.invoke(data.id, data.position)
    }
}

internal class MarkerData(
    val id: String,
    position: Point,
    val relativeOffset: Offset,
    val absoluteOffset: Offset,
    zIndex: Float,
    clickable: Boolean,
    val c: @Composable () -> Unit
) {
    var position by mutableStateOf(position)
    var isDraggable by mutableStateOf(false)
    var dragInterceptor: DragInterceptor? by mutableStateOf(null)
    var isClickable: Boolean by mutableStateOf(clickable)
    var zIndex: Float by mutableStateOf(zIndex)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MarkerData

        if (id != other.id) return false
        if (position != other.position) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + position.hashCode()
        return result
    }
}

internal data class CalloutData(val markerData: MarkerData, val autoDismiss: Boolean)

internal typealias MarkerMoveCb = (id: String, position: Point, delta: Point) -> Unit
internal typealias MarkerClickCb = (id: String, position: Point) -> Unit
internal typealias DragInterceptor = (id: String, position: Point, delta: Point) -> Unit