package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape

internal class MarkerState {
    internal val markers = mutableStateMapOf<String, MarkerData>()
    internal var markerMoveCb: MarkerMoveCb? = null
    internal var markerClickCb: MarkerClickCb? = null

    internal val callouts = mutableStateMapOf<String, CalloutData>()
    internal var calloutClickCb: MarkerClickCb? = null

    fun addMarker(
        id: String, x: Double, y: Double, relativeOffset: Offset, absoluteOffset: Offset,
        zIndex: Float, clickable: Boolean, clipShape: Shape?, isConstrainedInBounds: Boolean,
        c: @Composable () -> Unit
    ) {
        markers[id] =
            MarkerData(
                id,
                x,
                y,
                relativeOffset,
                absoluteOffset,
                zIndex,
                clickable,
                clipShape,
                isConstrainedInBounds,
                c
            )
    }

    fun addCallout(
        id: String, x: Double, y: Double, relativeOffset: Offset, absoluteOffset: Offset,
        zIndex: Float, autoDismiss: Boolean, clickable: Boolean, isConstrainedInBounds: Boolean,
        c: @Composable () -> Unit
    ) {
        val markerData =
            MarkerData(
                id,
                x,
                y,
                relativeOffset,
                absoluteOffset,
                zIndex,
                clickable,
                clipShape = null,
                isConstrainedInBounds,
                c
            )
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

    fun moveMarkerTo(id: String, x: Double, y: Double) {
        val marker = markers[id] ?: return
        with(marker) {
            val prevX = x
            val prevY = y
            this.x = if (isConstrainedInBounds) x.coerceIn(0.0, 1.0) else x
            this.y = if (isConstrainedInBounds) y.coerceIn(0.0, 1.0) else y
            onMarkerMove(this, this.x - prevX, this.y - prevY)
        }
    }

    /**
     * Move a marker by the provided delta (normalized) coordinates.
     */
    fun moveMarkerBy(id: String, deltaX: Double, deltaY: Double) {
        markers[id]?.apply {
            x = (x + deltaX).let {
                if (isConstrainedInBounds) it.coerceIn(0.0, 1.0) else it
            }
            y = (y + deltaY).let {
                if (isConstrainedInBounds) it.coerceIn(0.0, 1.0) else it
            }
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

    internal fun onCalloutClick(data: MarkerData) {
        calloutClickCb?.invoke(data.id, data.x, data.y)
    }
}

internal class MarkerData(
    val id: String,
    x: Double, y: Double,
    val relativeOffset: Offset,
    val absoluteOffset: Offset,
    zIndex: Float,
    clickable: Boolean,
    clipShape: Shape?,
    isConstrainedInBounds: Boolean,
    val c: @Composable () -> Unit
) {
    var x: Double by mutableStateOf(x)
    var y: Double by mutableStateOf(y)
    var isDraggable by mutableStateOf(false)
    var dragInterceptor: DragInterceptor? by mutableStateOf(null)
    var isClickable: Boolean by mutableStateOf(clickable)
    var clipShape: Shape? by mutableStateOf(clipShape)
    var zIndex: Float by mutableStateOf(zIndex)
    var isConstrainedInBounds by mutableStateOf(isConstrainedInBounds)

    var measuredWidth = 0
    var measuredHeight = 0

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

internal data class CalloutData(val markerData: MarkerData, val autoDismiss: Boolean)

internal typealias MarkerMoveCb = (id: String, x: Double, y: Double, dx: Double, dy: Double) -> Unit
internal typealias MarkerClickCb = (id: String, x: Double, y: Double) -> Unit

fun interface DragInterceptor {
    /**
     * The default behavior (e.g without a drag interceptor) updates the marker coordinates like so:
     * * x: [x] + [dx]
     * * y: [y] + [dy]
     *
     * @param id: The id of the marker
     * @param x, y: The current normalized coordinates of the marker
     * @param dx, dy: The virtual displacement expressed in relative coordinates (not in pixels) that would
     * have been applied if there were no drag interceptor
     * @param px, py: The current normalized coordinates of the pointer
     */
    fun onMove(
        id: String,
        x: Double,
        y: Double,
        dx: Double,
        dy: Double,
        px: Double,
        py: Double
    )
}