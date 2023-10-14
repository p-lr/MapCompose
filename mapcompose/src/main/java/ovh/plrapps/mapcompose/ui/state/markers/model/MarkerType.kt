package ovh.plrapps.mapcompose.ui.state.markers.model

internal sealed interface MarkerType {
    data object Marker : MarkerType
    data object Callout : MarkerType
    data class Cluster(val clustererId: String, val markersData: List<MarkerData>) : MarkerType
}
