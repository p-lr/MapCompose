package ovh.plrapps.mapcompose.ui.state.markers

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.ClusterScaleThreshold
import ovh.plrapps.mapcompose.ui.markers.Clusterer
import ovh.plrapps.mapcompose.ui.markers.LazyLoader
import ovh.plrapps.mapcompose.ui.gestures.model.HitType
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.ClusterClickBehavior
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerType
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy

internal class MarkerState(
    scope: CoroutineScope,
    private val markerRenderState: MarkerRenderState
) {
    private val markers = MutableStateFlow<List<MarkerData>>(emptyList())
    internal var markerClickCb: MarkerHitCb? = null
    internal var markerLongPressCb: MarkerHitCb? = null
    internal var markerMoveCb: MarkerMoveCb? = null

    private val clusterersById = mutableMapOf<String, Clusterer>()
    private val lazyLoaderById = mutableMapOf<String, LazyLoader>()

    init {
        scope.launch {
            renderRegularMarkers()
        }
    }

    fun hasMarker(id: String): Boolean = markers.value.any { it.id == id }

    fun getMarker(id: String): MarkerData? {
        return markers.value.firstOrNull { it.id == id }
    }

    fun getRenderedMarkers(): List<MarkerData> = markerRenderState.markers.value

    fun addMarker(
        id: String, x: Double, y: Double,
        relativeOffset: Offset,
        absoluteOffset: DpOffset,
        zIndex: Float,
        clickable: Boolean,
        isConstrainedInBounds: Boolean,
        clickableAreaScale: Offset,
        clickableAreaCenterOffset: Offset,
        renderingStrategy: RenderingStrategy,
        c: @Composable () -> Unit
    ) {
        if (hasMarker(id)) return
        markers.value += MarkerData(
            id = id,
            x = x,
            y = y,
            relativeOffset = relativeOffset,
            absoluteOffset = absoluteOffset,
            zIndex = zIndex,
            clickable = clickable,
            isConstrainedInBounds = isConstrainedInBounds,
            clickableAreaScale = clickableAreaScale,
            clickableAreaCenterOffset = clickableAreaCenterOffset,
            renderingStrategy = renderingStrategy,
            type = MarkerType.Marker,
            c = c
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
        scaleThreshold: ClusterScaleThreshold,
        clusterFactory: (ids: List<String>) -> (@Composable () -> Unit)
    ) {
        val clusterer = Clusterer(
            id = id,
            clusteringThreshold = clusteringThreshold,
            mapState = mapState,
            markerRenderState = markerRenderState,
            markersDataFlow = markers,
            clusterClickBehavior = clusterClickBehavior,
            scaleThreshold = scaleThreshold,
            clusterFactory = clusterFactory
        )
        clusterersById[id] = clusterer
    }

    fun setClusteredExemptList(id: String, markersToExempt: Set<String>) {
        clusterersById[id]?.apply {
            exemptionSet.value = markersToExempt
        }
    }

    fun addLazyLoader(
        mapState: MapState,
        id: String,
        padding: Dp
    ) {
        val lazyLoader = LazyLoader(
            id, mapState, markerRenderState, markers, padding, mapState.scope
        )
        lazyLoaderById[id] = lazyLoader
    }

    fun removeClusterer(id: String, removeManaged: Boolean) {
        clusterersById[id]?.apply {
            cancel(removeManaged = removeManaged)
        }
        clusterersById.remove(id)

        if (removeManaged) {
            removeAll {
                (it.renderingStrategy is RenderingStrategy.Clustering) &&
                        (it.renderingStrategy.clustererId == id)
            }
        }
    }

    fun removeLazyLoader(id: String, removeManaged: Boolean) {
        lazyLoaderById[id]?.apply {
            cancel(removeManaged = removeManaged)
        }

        if (removeManaged) {
            removeAll {
                (it.renderingStrategy is RenderingStrategy.LazyLoading) &&
                        (it.renderingStrategy.lazyLoaderId == id)
            }
        }
    }

    fun onHit(x: Int, y: Int, hitType: HitType): Boolean {
        return markerRenderState.getMarkerForHit(x, y)?.also { markerData ->
            /* If it's a cluster, run the corresponding click behavior. */
            if (markerData.type is MarkerType.Cluster && hitType == HitType.Click) {
                val clusterer = clusterersById[markerData.type.clustererId]
                clusterer?.onPlaceableClick(markerData)
            } else {
                /* It's not a cluster. Invoke user callback, if any. */
                when (hitType) {
                    HitType.Click -> markerClickCb?.invoke(markerData.id, markerData.x, markerData.y)
                    HitType.LongPress -> markerLongPressCb?.invoke(markerData.id, markerData.x, markerData.y)
                }
            }
        } != null
    }

    private fun removeAll(predicate: (MarkerData) -> Boolean) {
        markers.value = markers.value.filterNot {
            predicate(it)
        }
    }

    private suspend fun renderRegularMarkers() {
        markers.collect {
            val regular = it.filter { markerData ->
                markerData.renderingStrategy is RenderingStrategy.Default
            }
            val rendered = markerRenderState.getRegularMarkers()
            val toAdd = regular - rendered
            val toRemove = rendered - regular

            markerRenderState.addRegularMarkers(toAdd)
            markerRenderState.removeRegularMarkers(toRemove)
        }
    }
}