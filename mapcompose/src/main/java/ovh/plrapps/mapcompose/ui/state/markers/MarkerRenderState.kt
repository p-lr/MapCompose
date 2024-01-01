package ovh.plrapps.mapcompose.ui.state.markers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerType
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import ovh.plrapps.mapcompose.utils.removeFirst
import kotlin.math.pow

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
    internal var calloutClickCb: MarkerHitCb? = null

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
                id = id,
                x = x,
                y = y,
                relativeOffset = relativeOffset,
                absoluteOffset = absoluteOffset,
                zIndex = zIndex,
                clickable = clickable,
                isConstrainedInBounds = isConstrainedInBounds,
                clickableAreaScale = Offset(1f, 1f),
                clickableAreaCenterOffset = Offset(0f, 0f),
                renderingStrategy = RenderingStrategy.Default,
                type = MarkerType.Callout,
                c = c
            )
        callouts[id] = CalloutData(markerData, autoDismiss)
    }

    fun hasCallout(id: String): Boolean = callouts.containsKey(id)

    fun moveCallout(id: String, x: Double, y: Double) {
        callouts[id]?.markerData?.also {
            it.x = if (it.isConstrainedInBounds) x.coerceIn(0.0, 1.0) else x
            it.y = if (it.isConstrainedInBounds) y.coerceIn(0.0, 1.0) else y
        }
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
     * Get the nearest marker which contains the click position and has the highest z-index.
     */
    fun getMarkerForHit(xPx: Int, yPx: Int): MarkerData? {
        if (!hasClickable.value) return null
        val candidates = markers.value.filter { markerData ->
            markerData.isClickable && markerData.contains(xPx, yPx)
        }
        val highestZ = candidates.maxByOrNull { it.zIndex }?.zIndex ?: return null

        return candidates.filter {
            it.zIndex == highestZ
        }.minWithOrNull { markerData1, markerData2 ->
            if (squareDistance(markerData1, xPx, yPx) > squareDistance(markerData2, xPx, yPx)) 1 else -1
        }
    }

    private fun squareDistance(markerData: MarkerData, x: Int, y: Int): Float {
        val (cx, cy) = markerData.getCenter() ?: return Float.MAX_VALUE
        return (cx - x).pow(2) + (cy - y).pow(2)
    }

    internal fun onCalloutClick(data: MarkerData) {
        calloutClickCb?.invoke(data.id, data.x, data.y)
    }
}

internal data class CalloutData(val markerData: MarkerData, val autoDismiss: Boolean)

internal typealias MarkerMoveCb = (id: String, x: Double, y: Double, dx: Double, dy: Double) -> Unit
internal typealias MarkerHitCb = (id: String, x: Double, y: Double) -> Unit

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