package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.MarkerRenderState
import ovh.plrapps.mapcompose.ui.state.markers.model.*
import ovh.plrapps.mapcompose.ui.state.markers.model.ClusterClickBehavior
import ovh.plrapps.mapcompose.ui.state.markers.model.Custom
import ovh.plrapps.mapcompose.ui.state.markers.model.Default
import ovh.plrapps.mapcompose.ui.state.markers.model.None
import ovh.plrapps.mapcompose.utils.contains
import ovh.plrapps.mapcompose.utils.dpToPx
import ovh.plrapps.mapcompose.utils.map
import ovh.plrapps.mapcompose.utils.throttle
import java.util.*
import kotlin.math.*

internal class Clusterer(
    val id: String,
    clusteringThreshold: Dp,
    private val mapState: MapState,
    private val markerRenderState: MarkerRenderState,
    markersDataFlow: MutableStateFlow<List<MarkerData>>,
    private val clusterClickBehavior: ClusterClickBehavior,
    private val clusterFactory: (ids: List<String>) -> (@Composable () -> Unit)
) {
    private val scope = CoroutineScope(
        mapState.scope.coroutineContext + SupervisorJob(mapState.scope.coroutineContext[Job])
    )

    /* Create a derived state flow from the original unique source of truth */
    private val markers = markersDataFlow.map(scope) {
        it.filter { markerData ->
            (markerData.renderingStrategy is RenderingStrategy.Clustering) &&
            markerData.renderingStrategy.clustererId == id
        }.map { markerData ->
            Marker(markerData)
        }
    }

    private val referentialSnapshotFlow = mapState.referentialSnapshotFlow()
    private val clusterIdPrefix = "#cluster#-$id"
    private val epsilon = dpToPx(clusteringThreshold.value)

    init {
        scope.launch {
            markers.throttle(100).collectLatest {
                referentialSnapshotFlow.throttle(500).collectLatest {
                    val scale = it.scale
                    val padding = dpToPx(100f).toInt()
                    val visibleArea = mapState.visibleArea(IntOffset(padding, padding))

                    /* Get the list of rendered clusterer managed (by this clusterer) markers */
                    val markersOnMap =
                        markerRenderState.getClusteredMarkers().filter { markerData ->
                            (markerData.renderingStrategy is RenderingStrategy.Clustering) &&
                                    markerData.renderingStrategy.clustererId == id
                        }
                    withContext(Dispatchers.Default) {
                        clusterize(scale, visibleArea, markersOnMap, epsilon)
                    }
                }
            }
        }
    }

    fun onPlaceableClick(clusterData: MarkerData) {
        if (clusterData.type !is MarkerType.Cluster) return
        val markersData = clusterData.type.markersData
        when (clusterClickBehavior) {
            is Custom -> {
                clusterClickBehavior.onClick(
                    ClusterInfo(clusterData.x, clusterData.y, markersData)
                )
            }
            Default -> {
                defaultClusterClickListener(markersData)
            }
            None -> {
            }
        }
    }

    /**
     * The user might want to cancel a clusterer while keeping managed markers. For example,
     * removing a clusterer and adding it back with the same id but with a different cluster style.
     * This allows for replacing a clusterer without any visual blinks.
     */
    fun cancel(removeManaged: Boolean) {
        scope.cancel()
        if (removeManaged) {
            markerRenderState.removeAllClusterManagedMarkers(id)
        }
    }

    private suspend fun clusterize(
        scale: Float,
        visibleArea: VisibleArea,
        markersOnMap: List<MarkerData>,
        epsilon: Float
    ) = coroutineScope {
        val visibleMarkers = markers.value.filter { marker ->
            visibleArea.contains(marker.x, marker.y)
        }

        /* Disable clustering if scale is max scale */
        val result = if (scale < mapState.maxScale) {
            val densitySearchPass = processMarkers(visibleMarkers, scale, epsilon)
            mergeClosest(densitySearchPass, epsilon, scale)
        } else {
            ClusteringResult(markers = visibleMarkers)
        }

        withContext(Dispatchers.Main) {
            render(markersOnMap, result.clusters, result.markers)
        }
    }

    private fun render(
        markersOnMap: List<MarkerData>,
        clusters: List<Cluster>,
        markers: List<Marker>
    ) {
        val clustersById = clusters.associateByTo(mutableMapOf()) { it.id }
        val markersById = markers.associateByTo(mutableMapOf()) { it.uuid }

        val clusterIds = mutableListOf<String>()
        val markerIds = mutableListOf<UUID>()

        markersOnMap.forEach { markerData ->
            if (markerData.id.startsWith(clusterIdPrefix)) {
                clusterIds.add(markerData.id)
                val inMemory = clustersById[markerData.id]
                if (inMemory == null) {
                    markerRenderState.removeClustererManagedMarker(markerData.id)
                } else {
                    if (inMemory.x != markerData.x || inMemory.y != markerData.y) {
                        mapState.markerState.moveMarkerTo(markerData, inMemory.x, inMemory.y)
                    }
                }
            } else { // then it must be a marker
                if (shouldProcessMarker(markerData)) {
                    markerIds.add(markerData.uuid)
                    val inMemory = markersById[markerData.uuid]
                    if (inMemory == null) {
                        markerRenderState.removeClustererManagedMarker(markerData.id)
                    } else {
                        if (inMemory.x != markerData.x || inMemory.y != markerData.y) {
                            mapState.markerState.moveMarkerTo(markerData, inMemory.x, inMemory.y)
                        }
                    }
                }
            }
        }

        clustersById.entries.forEach {
            if (it.key !in clusterIds) {
                it.value.addToMap()
            }
        }
        markersById.entries.forEach {
            if (it.key !in markerIds) {
                it.value.addToMap()
            }
        }
    }

    private fun processMarkers(
        markers: List<Marker>, scale: Float, epsilon: Float
    ): ClusteringResult {
        val snapScale = getSnapScale(scale)
        val mesh = Mesh(epsilon, snapScale, mapState.fullSize)
        markers.forEach { marker ->
            mesh.add(marker)
        }

        return findNewClustersByDensity(mesh, scale, epsilon)
    }

    private fun findNewClustersByDensity(
        mesh: Mesh,
        scale: Float,
        epsilon: Float,
    ): ClusteringResult {
        /* Compute density for each window */
        mesh.gridMap.keys.forEach { key ->
            val neighbors = mesh.getNeighbors(key)
            val window = mesh.gridMap[key]
            if (window != null) {
                window.density = window.markers.size + neighbors.sumOf { it.markers.size }
            }
        }

        val entriesSorted = mesh.gridMap.entries.sortedByDescending {
            it.value.density
        }

        val clusterList = mutableListOf<Cluster>()
        val markerList = mutableListOf<Marker>()

        val markerAssigned = markers.value.associateTo(mutableMapOf()) {
            it.uuid to false
        }

        for (e in entriesSorted) {
            val neighbors = mesh.getNeighbors(e.key)
            val neighborsMarkers = neighbors.flatMap {
                it.markers
            }
            val startBary = getBarycenter(e.value.markers) ?: break

            val mergedMarkers = (e.value.markers + neighborsMarkers).filter { marker ->
                distance(startBary, marker, scale) < epsilon && (markerAssigned[marker.uuid]
                    ?: false).not()
            }.onEach {
                markerAssigned[it.uuid] = true
            }

            if (mergedMarkers.size == 1) {
                markerList.add(mergedMarkers.first())
                continue
            }

            val cluster = mergedMarkers.toCluster()

            if (cluster.markers.isNotEmpty()) {
                clusterList.add(cluster)
            }
        }

        return ClusteringResult(clusterList, markerList)
    }

    private tailrec fun mergeClosest(
        result: ClusteringResult,
        epsilon: Float,
        scale: Float
    ): ClusteringResult {
        fun findInVicinity(cluster: Cluster): Placeable? {
            val closeEnoughMarker = result.markers.firstOrNull {
                distance(cluster.x, cluster.y, it.x, it.y, scale) < epsilon
            }
            return closeEnoughMarker ?: result.clusters.firstOrNull { otherCluster ->
                distance(otherCluster.x, otherCluster.y, cluster.x, cluster.y, scale) < epsilon
                        && otherCluster != cluster
            }
        }

        for (cluster in result.clusters) {
            val inVicinity = findInVicinity(cluster)
            if (inVicinity != null) {
                return when (inVicinity) {
                    is Cluster -> {
                        val fusedCluster = fuseClusters(cluster, inVicinity)
                        val newClusterList = result.clusters.filter {
                            it != cluster && it != inVicinity
                        } + fusedCluster
                        mergeClosest(
                            ClusteringResult(newClusterList, result.markers),
                            epsilon,
                            scale
                        )
                    }
                    is Marker -> {
                        val fusedCluster = cluster.addMarker(inVicinity)
                        val newClusterList = result.clusters.filter {
                            it != cluster
                        } + fusedCluster
                        val newMarkerList = result.markers.filter {
                            it != inVicinity
                        }
                        mergeClosest(
                            ClusteringResult(newClusterList, newMarkerList),
                            epsilon,
                            scale
                        )
                    }
                }
            }
        }

        return result
    }

    private fun getBarycenter(markers: List<Marker>): Barycenter? {
        if (markers.isEmpty()) return null
        return Barycenter(
            x = markers.sumOf { it.x } / markers.size,
            y = markers.sumOf { it.y } / markers.size,
            weight = markers.size
        )
    }

    private fun distance(b: Barycenter, marker: Marker, scale: Float): Double {
        return distance(b.x, b.y, marker.x, marker.y, scale)
    }

    private fun distance(x1: Double, y1: Double, x2: Double, y2: Double, scale: Float): Double {
        return sqrt(
            (abs(x1 - x2) * mapState.fullSize.width * scale).pow(2) +
                    (abs(y1 - y2) * mapState.fullSize.height * scale).pow(2),
        )
    }

    private fun fuseClusters(cluster1: Cluster, cluster2: Cluster): Cluster {
        val newMarkers = cluster1.markers + cluster2.markers
        return newMarkers.toCluster()
    }

    private fun Cluster.addMarker(marker: Marker): Cluster {
        val newMarkers = markers + marker
        return newMarkers.toCluster()
    }

    private fun List<Marker>.toCluster(): Cluster {
        return Cluster(
            clusterIdPrefix = clusterIdPrefix,
            x = sumOf { it.x } / size,
            y = sumOf { it.y } / size,
            markers = this
        )
    }

    private fun Cluster.addToMap() {
        val markersData = markers.map { it.markerData }
        val markerData = makeClusterMarkerData(id, x, y, markersData) {
            clusterFactory(markers.map { it.id })()
        }
        markerRenderState.addClustererManagedMarker(markerData)
    }

    private fun defaultClusterClickListener(markers: List<MarkerData>) {
        if (markers.isEmpty()) return

        /* Compute the bounding box */
        var minX: Double = Double.MAX_VALUE
        var maxX: Double = Double.MIN_VALUE
        var minY: Double = Double.MAX_VALUE
        var maxY: Double = Double.MIN_VALUE
        markers.forEach {
            minX = if (it.x < minX) it.x else minX
            maxX = if (it.x > maxX) it.x else maxX
            minY = if (it.y < minY) it.y else minY
            maxY = if (it.y > maxY) it.y else maxY
        }
        val bb = BoundingBox(minX, minY, maxX, maxY)

        scope.launch {
            mapState.scrollTo(bb, padding = Offset(0.2f, 0.2f))
        }
    }

    private fun shouldProcessMarker(markerData: MarkerData): Boolean {
        return (markerData.renderingStrategy is RenderingStrategy.Clustering) &&
                markerData.renderingStrategy.clustererId == id
    }

    private fun getSnapScale(scale: Float): Float = 2.0.pow(ceil(ln(scale) / ln(2.0))).toFloat()

    private fun Marker.addToMap() {
        markerRenderState.addClustererManagedMarker(markerData)
    }

    private fun makeClusterMarkerData(
        id: String,
        x: Double,
        y: Double,
        markersData: List<MarkerData>,
        c: @Composable () -> Unit
    ): MarkerData {
        return MarkerData(
            id, x, y,
            relativeOffset = Offset(-0.5f, -0.5f),
            absoluteOffset = Offset.Zero,
            zIndex = markersData.maxOfOrNull { it.zIndex } ?: 0f,
            isConstrainedInBounds = true,
            clickableAreaScale = Offset(1f, 1f),
            clickableAreaCenterOffset = Offset(0f, 0f),
            clickable = true,
            renderingStrategy = RenderingStrategy.Clustering(this@Clusterer.id),
            type = MarkerType.Cluster(clustererId = this@Clusterer.id, markersData),
            c = c
        )
    }

    private data class Barycenter(val x: Double, val y: Double, val weight: Int)

    private data class ClusteringResult(
        val clusters: List<Cluster> = emptyList(),
        val markers: List<Marker> = emptyList()
    )
}

private class Mesh(
    private val meshSize: Float,
    private val scale: Float,
    private val fullSize: IntSize,
) {
    val gridMap = mutableMapOf<Key, MarkerWindow>()
    val markers = mutableListOf<Marker>()

    private fun getKey(marker: Marker, meshSize: Float, scale: Float): Key {
        val relativeWidth = marker.x * fullSize.width * scale
        val relativeHeight = marker.y * fullSize.height * scale

        return Key(
            row = (relativeWidth / meshSize).toInt(),
            col = (relativeHeight / meshSize).toInt()
        )
    }

    fun add(marker: Marker) {
        val key = getKey(marker, meshSize, scale)
        val window = gridMap[key]
        if (window == null) {
            gridMap[key] = MarkerWindow(mutableListOf(marker))
        } else {
            window.markers.add(marker)
        }
        markers.add(marker)
    }

    fun getNeighbors(key: Key): List<MarkerWindow> {
        val neighborKeys = listOf(
            Key(key.row - 1, key.col - 1),
            Key(key.row - 1, key.col),
            Key(key.row - 1, key.col + 1),
            Key(key.row, key.col - 1),
            Key(key.row, key.col + 1),
            Key(key.row + 1, key.col - 1),
            Key(key.row + 1, key.col),
            Key(key.row + 1, key.col + 1),
        )

        return neighborKeys.mapNotNull {
            gridMap[it]
        }
    }
}

private data class MarkerWindow(
    val markers: MutableList<Marker>,
    var density: Int = 0
)

private data class Key(val row: Int, val col: Int)

private sealed interface Placeable

/**
 * A marker can belong to one and only one cluster.
 */
private data class Marker(
    val markerData: MarkerData,
) : Placeable {
    val uuid: UUID
        get() = markerData.uuid
    val id: String
        get() = markerData.id
    val x: Double
        get() = markerData.x
    val y: Double
        get() = markerData.y
}

private data class Cluster(
    val clusterIdPrefix: String,
    val x: Double,
    val y: Double,
    val markers: List<Marker>
) : Placeable {
    val id = clusterIdPrefix + markers.map { it.id }.sorted()
}
