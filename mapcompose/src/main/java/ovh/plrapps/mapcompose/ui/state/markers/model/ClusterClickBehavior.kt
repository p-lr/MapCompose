package ovh.plrapps.mapcompose.ui.state.markers.model

internal sealed interface ClusterClickBehavior
internal object Default : ClusterClickBehavior
internal data class Custom(val onClick : (ClusterInfo) -> Unit) : ClusterClickBehavior
internal object None : ClusterClickBehavior

internal data class ClusterInfo(val x: Double, val y: Double, val markers: List<MarkerData>)