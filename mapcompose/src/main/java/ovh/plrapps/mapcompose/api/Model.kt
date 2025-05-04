package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.core.Layer
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.core.makeLayerId
import ovh.plrapps.mapcompose.ui.state.markers.model.ClusterClickBehavior as ClusterClickBehaviorInternal
import ovh.plrapps.mapcompose.ui.state.markers.model.Custom as CustomInternal
import ovh.plrapps.mapcompose.ui.state.markers.model.Default as DefaultInternal
import ovh.plrapps.mapcompose.ui.state.markers.model.None as NoneInternal

data class Scroll(val x: Double, val y: Double)

sealed interface ClusterClickBehavior

/**
 * Zoom-in to reveal a subset or all markers of the cluster.
 */
data object Default : ClusterClickBehavior

/**
 * When a cluster is clicked, the provided [onClick] callback is invoked.
 * the optional parameter [withDefaultBehavior] signifies if the [Default] callback behavior should be applied too
 */
data class Custom(val withDefaultBehavior: Boolean = false, val onClick: (ClusterData) -> Unit) : ClusterClickBehavior

/**
 * Cluster related data.
 * @param x, y The coordinates of the cluster's barycenter
 * @param markers The list of markers contained by the cluster
 */
data class ClusterData(val x: Double, val y: Double, val markers: List<MarkerDataSnapshot>)

/**
 * Clusters aren't clickable
 */
data object None : ClusterClickBehavior

/**
 * Convert public api type to internal type.
 */
internal fun ClusterClickBehavior.toInternal(): ClusterClickBehaviorInternal {
    return when (this) {
        is Custom -> CustomInternal(
            onClick = {
                this.onClick(
                    ClusterData(
                        it.x,
                        it.y,
                        it.markers.map { markerData ->
                            MarkerDataSnapshot(markerData.id, markerData.x, markerData.y)
                        }
                    )
                )
            },
            withDefaultBehavior = this.withDefaultBehavior

        )
        Default -> DefaultInternal
        None -> NoneInternal
    }
}

sealed interface ClusterScaleThreshold {
    data object MaxScale : ClusterScaleThreshold
    data class FixedScale(val scale: Double) : ClusterScaleThreshold
}

internal class LayersBuilderInternal : LayersBuilder {
    internal val layers = mutableListOf<Layer>()
    override fun addLayer(tileStreamProvider: TileStreamProvider, initialOpacity: Float) {
        val id = makeLayerId()
        val layer = Layer(id, tileStreamProvider, initialOpacity)
        layers.add(layer)
    }
}