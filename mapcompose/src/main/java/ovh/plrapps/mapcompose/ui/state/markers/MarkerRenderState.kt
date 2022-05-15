package ovh.plrapps.mapcompose.ui.state.markers

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData

internal class MarkerRenderState {
    internal val markers = derivedStateOf {
        regularMarkers + clustererManagedMarkers
    }

    internal val regularMarkers = mutableStateListOf<MarkerData>()
    internal val clustererManagedMarkers = mutableStateListOf<MarkerData>()
    internal var markerClickCb: MarkerClickCb? = null

    internal val callouts = mutableStateMapOf<String, CalloutData>()
    internal var calloutClickCb: MarkerClickCb? = null

    fun addClustererManagedMarker(markerData: MarkerData) {
        clustererManagedMarkers.add(markerData)
    }

    fun removeClustererManagedMarker(id: String): Boolean {
        var removed = false
        val it = clustererManagedMarkers.iterator()
        while (it.hasNext()) {
            if (it.next().id == id) {
                it.remove()
                removed = true
                break
            }
        }
        return removed
    }

    fun removeAllClusterManagedMarkers(clusteredId: String) {
        val it = clustererManagedMarkers.iterator()
        while (it.hasNext()) {
            if (it.next().clustererId == clusteredId) {
                it.remove()
            }
        }
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
                null,
                c
            )
        callouts[id] = CalloutData(markerData, autoDismiss)
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

    internal fun onMarkerClick(data: MarkerData) {
        markerClickCb?.invoke(data.id, data.x, data.y)
    }

    internal fun onCalloutClick(data: MarkerData) {
        calloutClickCb?.invoke(data.id, data.x, data.y)
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