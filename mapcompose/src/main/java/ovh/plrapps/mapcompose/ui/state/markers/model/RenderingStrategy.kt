package ovh.plrapps.mapcompose.ui.state.markers.model


sealed interface RenderingStrategy {
    data object Default : RenderingStrategy
    data class Clustering(val clustererId: String) : RenderingStrategy
    data class LazyLoading(val lazyLoaderId: String) : RenderingStrategy
}