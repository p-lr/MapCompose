package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.utils.randomDouble
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.contains
import ovh.plrapps.mapcompose.utils.throttle
import kotlin.math.*
import kotlin.random.Random.Default.nextDouble

/**
 * In this sample, an experimental clustering algorithm is used to display 400 markers.
 * The lazy loading technique (removing a marker/cluster when it's not visible) is also used for
 * performance reasons.
 */
class MarkersLazyLoadingVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    private val markers = buildList {
        repeat(40) { i ->
            val cx = nextDouble()
            val cy = nextDouble()
            repeat(10) { j ->
                val x = randomDouble(cx, 0.03).coerceAtLeast(0.0)
                val y = randomDouble(cy, 0.03).coerceAtLeast(0.0)
                add(Marker("marker-$i-$j", x, y))
            }
        }
    }

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096) {
            scale(0.81f)
            maxScale(8f)
        }.apply {
            addLayer(tileStreamProvider)
            enableRotation()
            shouldLoopScale = true
        }
    )

    init {
        viewModelScope.launch {
            state.referentialSnapshotFlow().throttle(500).collectLatest {
                val scale = it.scale
                val padding = dpToPx(100).toInt()
                val visibleArea = state.visibleArea(IntOffset(padding, padding))
                val markersOnMap = state.markerSnapshotFlow().firstOrNull().orEmpty()
                removeNonVisibleMarkers(visibleArea, markersOnMap)
                withContext(Dispatchers.Default) {
                    clusterize(scale, visibleArea, markersOnMap)
                }
            }
        }
    }

    private suspend fun removeNonVisibleMarkers(
        visibleArea: VisibleArea,
        markersOnMap: List<MarkerDataSnapshot>
    ) = withContext(Dispatchers.Main) {
        val markersToRemove = withContext(Dispatchers.Default) {
            markersOnMap.filter { dataSnapshot ->
                !visibleArea.contains(dataSnapshot.x, dataSnapshot.y)
            }
        }

        markersToRemove.forEach {
            state.removeMarker(it.id)
        }
    }

    private suspend fun clusterize(
        scale: Float,
        visibleArea: VisibleArea,
        markersOnMap: List<MarkerDataSnapshot>,
        epsilon: Float = dpToPx(50)
    ) = coroutineScope {

        val densitySearchPass = processMarkers(visibleArea, scale, epsilon)
        val mergePass = mergeClosest(densitySearchPass, epsilon, scale)

        withContext(Dispatchers.Main) {
            render(markersOnMap, mergePass.clusters, mergePass.markers)
        }
    }

    private fun render(
        markersOnMap: List<MarkerDataSnapshot>,
        clusters: List<Cluster>,
        markers: List<Marker>
    ) {
        val clustersById = clusters.associateByTo(mutableMapOf()) { it.id }
        val markersById = markers.associateByTo(mutableMapOf()) { it.id }

        val clusterIds = mutableListOf<String>()
        val markerIds = mutableListOf<String>()

        markersOnMap.forEach { markerData ->
            if (markerData.id.startsWith(CLUSTER_PREFIX)) {
                clusterIds.add(markerData.id)
                val inMemory = clustersById[markerData.id]
                if (inMemory == null) {
                    state.removeMarker(markerData.id)
                } else {
                    if (inMemory.x != markerData.x || inMemory.y != markerData.y) {
                        state.moveMarker(markerData.id, inMemory.x, inMemory.y)
                    }
                }
            } else { // then it must be a marker
                markerIds.add(markerData.id)
                val inMemory = markersById[markerData.id]
                if (inMemory == null) {
                    state.removeMarker(markerData.id)
                } else {
                    if (inMemory.x != markerData.x || inMemory.y != markerData.y) {
                        state.moveMarker(markerData.id, inMemory.x, inMemory.y)
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
        visibleArea: VisibleArea, scale: Float, epsilon: Float
    ): ClusteringResult {
        val remaining = markers.filter { marker ->
            visibleArea.contains(marker.x, marker.y)
        }

        val snapScale = getSnapScale(scale)
        val mesh = Mesh(epsilon, snapScale, state.fullSize)
        remaining.forEach { marker ->
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

        val markerAssigned = markers.associateTo(mutableMapOf()) {
            it.id to false
        }

        for (e in entriesSorted) {
            val neighbors = mesh.getNeighbors(e.key)
            val neighborsMarkers = neighbors.flatMap {
                it.markers
            }
            val startBary = getBarycenter(e.value.markers) ?: break

            val mergedMarkers = (e.value.markers + neighborsMarkers).filter { marker ->
                distance(startBary, marker, scale) < epsilon && (markerAssigned[marker.id]
                    ?: false).not()
            }.onEach {
                markerAssigned[it.id] = true
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
    ) : ClusteringResult {
        fun findInVicinity(cluster: Cluster) : Placeable? {
            val closeEnoughMarker = result.markers.firstOrNull {
                distance(cluster.x, cluster.y, it.x, it.y, scale) < epsilon
            }
            return closeEnoughMarker ?:
                result.clusters.firstOrNull { otherCluster ->
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
                        mergeClosest(ClusteringResult(newClusterList, result.markers), epsilon, scale)
                    }
                    is Marker -> {
                        val fusedCluster = cluster.addMarker(inVicinity)
                        val newClusterList = result.clusters.filter {
                            it != cluster
                        } + fusedCluster
                        val newMarkerList = result.markers.filter {
                            it != inVicinity
                        }
                        mergeClosest(ClusteringResult(newClusterList, newMarkerList), epsilon, scale)
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
            (abs(x1 - x2) * state.fullSize.width * scale).pow(2) +
                    (abs(y1 - y2) * state.fullSize.height * scale).pow(2),
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
            x = sumOf { it.x } / size,
            y = sumOf { it.y } / size,
            markers = this
        )
    }

    private fun Cluster.addToMap() {
        state.addMarker(id, x, y, relativeOffset = Offset(-0.5f, -0.5f), clickable = false) {
            Box(
                modifier = Modifier
                    .background(
                        Color(0x992196F3),
                        shape = CircleShape
                    )
                    .size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = weight.toString(), color = Color.White)
            }
        }
    }

    private fun getSnapScale(scale: Float): Float = 2.0.pow(ceil(ln(scale) / ln(2.0))).toFloat()

    private fun Marker.addToMap() {
        state.addMarker(id, x, y, clickable = false) {
            Icon(
                painter = painterResource(id = R.drawable.map_marker),
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color(0xEE2196F3)
            )
        }
    }

    private fun dpToPx(dp: Int): Float = dp * Resources.getSystem().displayMetrics.density

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

data class MarkerWindow(
    val markers: MutableList<Marker>,
    var density: Int = 0
)

data class Key(val row: Int, val col: Int)

private sealed interface Placeable

/**
 * A marker can belong to one and only one cluster.
 */
data class Marker(
    val id: String,
    val x: Double,
    val y: Double,
) : Placeable

data class Cluster(val x: Double, val y: Double, val markers: List<Marker>) : Placeable {
    val id = CLUSTER_PREFIX + markers.map { it.id }.sorted()
    val weight = markers.size
}

const val CLUSTER_PREFIX = "#cluster#-"