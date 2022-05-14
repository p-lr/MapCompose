package ovh.plrapps.mapcompose.ui.state.markers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.markers.Clusterer
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.ClusterClickBehavior
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData

internal class MarkerState(
    scope: CoroutineScope,
    private val markerRenderState: MarkerRenderState
) {
    private val markers = MutableStateFlow<List<MarkerData>>(emptyList())
    internal var markerMoveCb: MarkerMoveCb? = null

    private val clusterersById = mutableMapOf<String, Clusterer>()

    init {
        scope.launch {
            renderNonClusteredMarkers()
        }
    }

    fun hasMarker(id: String): Boolean = markers.value.any { it.id == id }

    fun getMarker(id: String): MarkerData? {
        return markers.value.firstOrNull { it.id == id }
    }

    fun getRenderedMarkers(): List<MarkerData> = markerRenderState.markers.value

    fun getRenderedAndClusteredManaged(): SnapshotStateList<MarkerData> {
        return markerRenderState.clustererManagedMarkers
    }

    fun addMarker(
        id: String, x: Double, y: Double, relativeOffset: Offset, absoluteOffset: Offset,
        zIndex: Float, clickable: Boolean, clipShape: Shape?, isConstrainedInBounds: Boolean,
        clustererId: String?,
        c: @Composable () -> Unit
    ) {
        if (hasMarker(id)) return
        markers.value += MarkerData(
            id,
            x,
            y,
            relativeOffset,
            absoluteOffset,
            zIndex,
            clickable,
            clipShape,
            isConstrainedInBounds,
            clustererId,
            c
        )
    }

    fun removeMarker(id: String): Boolean {
        return getMarker(id)?.let {
            markers.value = markers.value - it
            true
        } ?: false
    }

    fun removeAllMarkers() {
        markers.value = emptyList()
    }

    /**
     * Move a marker by the provided delta (normalized) coordinates.
     */
    fun moveMarkerBy(id: String, deltaX: Double, deltaY: Double) {
        getMarker(id)?.apply {
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

    fun moveMarkerTo(id: String, x: Double, y: Double) {
        val marker = getMarker(id) ?: return
        moveMarkerTo(marker, x, y)
    }

    fun moveMarkerTo(markerData: MarkerData, x: Double, y: Double) {
        with(markerData) {
            val prevX = x
            val prevY = y
            this.x = if (isConstrainedInBounds) x.coerceIn(0.0, 1.0) else x
            this.y = if (isConstrainedInBounds) y.coerceIn(0.0, 1.0) else y
            onMarkerMove(this, this.x - prevX, this.y - prevY)
        }
    }

    /**
     * If set, drag gestures will be handled for the marker identifiable by the [id].
     */
    fun setDraggable(id: String, draggable: Boolean) {
        getMarker(id)?.isDraggable = draggable
    }

    private fun onMarkerMove(data: MarkerData, dx: Double, dy: Double) {
        markerMoveCb?.invoke(data.id, data.x, data.y, dx, dy)
    }

    fun addClusterer(
        mapState: MapState,
        id: String,
        clusteringThreshold: Dp,
        clusterClickBehavior: ClusterClickBehavior,
        clusterFactory: (Int) -> (@Composable () -> Unit)
    ) {
        val clusterer = Clusterer(id, clusteringThreshold, mapState, markerRenderState, markers, clusterClickBehavior, clusterFactory)
        clusterersById[id] = clusterer
    }

    fun removeClusterer(id: String) {
        clusterersById[id]?.apply {
            cancel()
        }
        clusterersById.remove(id)
    }

    private suspend fun renderNonClusteredMarkers() {
        markers.collect {
            val nonClustered = it.filter { markerData ->
                markerData.clustererId == null
            }
            val rendered = markerRenderState.regularMarkers
            val toAdd = nonClustered - rendered
            val toRemove = rendered - nonClustered

            markerRenderState.regularMarkers += toAdd
            markerRenderState.regularMarkers -= toRemove
        }
    }
}