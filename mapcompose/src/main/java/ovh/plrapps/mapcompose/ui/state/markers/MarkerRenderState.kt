package ovh.plrapps.mapcompose.ui.state.markers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import ovh.plrapps.mapcompose.utils.removeFirst

internal class MarkerRenderState {
    internal val markers = derivedStateOf {
        regularMarkers + lazyLoadedMarkers + clustererManagedMarkers
    }

    private val hasClickable = derivedStateOf {
        markers.value.any {
            it.isClickable
        }
    }

    private val regularMarkers = mutableStateListOf<MarkerData>()
    private val lazyLoadedMarkers = mutableStateListOf<MarkerData>()
    private val clustererManagedMarkers = mutableStateListOf<MarkerData>()

    internal val callouts = mutableStateMapOf<String, CalloutData>()
    internal var calloutClickCb: MarkerClickCb? = null

    fun getRegularMarkers(): List<MarkerData> {
        return regularMarkers
    }

    fun addRegularMarkers(markerDataList: List<MarkerData>) {
        regularMarkers += markerDataList
    }

    fun removeRegularMarkers(markerDataList: List<MarkerData>) {
        regularMarkers -= markerDataList
    }

    fun getClusteredMarkers(): List<MarkerData> {
        return clustererManagedMarkers
    }

    fun addClustererManagedMarker(markerData: MarkerData) {
        clustererManagedMarkers.add(markerData)
    }

    fun removeClustererManagedMarker(id: String): Boolean {
        return clustererManagedMarkers.removeFirst { it.id == id }
    }

    fun removeAllClusterManagedMarkers(clusteredId: String) {
        clustererManagedMarkers.removeAll { markerData ->
            (markerData.renderingStrategy is RenderingStrategy.Clustering)
                    && markerData.renderingStrategy.clustererId == clusteredId
        }
    }

    fun getLazyLoadedMarkers(): List<MarkerData> {
        return lazyLoadedMarkers
    }

    fun addLazyLoadedMarker(markerData: MarkerData) {
        lazyLoadedMarkers.add(markerData)
    }

    fun removeLazyLoadedMarker(id: String): Boolean {
        return lazyLoadedMarkers.removeFirst { it.id == id }
    }

    fun removeAllLazyLoadedMarkers(lazyLoaderId: String) {
        lazyLoadedMarkers.removeAll { markerData ->
            (markerData.renderingStrategy is RenderingStrategy.LazyLoading)
                    && markerData.renderingStrategy.lazyLoaderId == lazyLoaderId
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
                renderingStrategy = RenderingStrategy.Default,
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

    /**
     * When at least one rendered marker is clickable, we need to get notified on tap gesture.
     */
    fun requiresTapGesture(): Boolean {
        return hasClickable.value
    }

    fun getMarkerOnHit(xPx: Int, yPx: Int): MarkerData? {
        return markers.value.filter { markerData ->
            markerData.isClickable && markerData.contains(xPx, yPx)
        }.maxByOrNull {
            it.zIndex
        }
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