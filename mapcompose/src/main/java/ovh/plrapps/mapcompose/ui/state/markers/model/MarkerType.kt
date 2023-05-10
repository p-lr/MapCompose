package ovh.plrapps.mapcompose.ui.state.markers.model

internal sealed interface MarkerType {
    object Marker : MarkerType
    object Callout : MarkerType
    data class Cluster(val clustererId: String, val markersData: List<MarkerData>) : MarkerType
}
