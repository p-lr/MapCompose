package ovh.plrapps.mapcompose.demo.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ovh.plrapps.mapcompose.demo.ui.screens.AddingMarkerDemo
import ovh.plrapps.mapcompose.demo.ui.screens.AnimationDemo
import ovh.plrapps.mapcompose.demo.ui.screens.AnimationJitterDemo
import ovh.plrapps.mapcompose.demo.ui.screens.CalloutDemo
import ovh.plrapps.mapcompose.demo.ui.screens.CenteringOnMarkerDemo
import ovh.plrapps.mapcompose.demo.ui.screens.CustomDrawDemo
import ovh.plrapps.mapcompose.demo.ui.screens.Home
import ovh.plrapps.mapcompose.demo.ui.screens.HttpTilesDemo
import ovh.plrapps.mapcompose.demo.ui.screens.LayersDemoSimple
import ovh.plrapps.mapcompose.demo.ui.screens.MapDemoSimple
import ovh.plrapps.mapcompose.demo.ui.screens.MarkersClusteringDemo
import ovh.plrapps.mapcompose.demo.ui.screens.MarkersLazyLoadingDemo
import ovh.plrapps.mapcompose.demo.ui.screens.OsmDemo
import ovh.plrapps.mapcompose.demo.ui.screens.PathsDemo
import ovh.plrapps.mapcompose.demo.ui.screens.RotationDemo
import ovh.plrapps.mapcompose.demo.ui.screens.VisibleAreaPaddingDemo
import ovh.plrapps.mapcompose.demo.ui.theme.MapComposeTheme

@Composable
fun MapComposeDemoApp() {
    val navController = rememberNavController()

    MapComposeTheme {
        NavHost(navController, startDestination = HOME) {
            composable(HOME) {
                Home(demoListState = rememberLazyListState()) {
                    navController.navigate(it.name)
                }
            }
            composable(MainDestinations.MAP_ALONE.name) {
                MapDemoSimple()
            }
            composable(MainDestinations.LAYERS_DEMO.name) {
                LayersDemoSimple()
            }
            composable(MainDestinations.MAP_WITH_ROTATION_CONTROLS.name) {
                RotationDemo()
            }
            composable(MainDestinations.ADDING_MARKERS.name) {
                AddingMarkerDemo()
            }
            composable(MainDestinations.CENTERING_ON_MARKER.name) {
                CenteringOnMarkerDemo()
            }
            composable(MainDestinations.PATHS.name) {
                PathsDemo()
            }
            composable(MainDestinations.CUSTOM_DRAW.name) {
                CustomDrawDemo()
            }
            composable(MainDestinations.CALLOUT_DEMO.name) {
                CalloutDemo()
            }
            composable(MainDestinations.ANIMATION_DEMO.name) {
                AnimationDemo()
            }
            composable(MainDestinations.OSM_DEMO.name) {
                OsmDemo()
            }
            composable(MainDestinations.HTTP_TILES_DEMO.name) {
                HttpTilesDemo()
            }
            composable(MainDestinations.VISIBLE_AREA_PADDING.name) {
                VisibleAreaPaddingDemo()
            }
            composable(MainDestinations.MARKERS_CLUSTERING.name) {
                MarkersClusteringDemo()
            }
            composable(MainDestinations.MARKERS_LAZY_LOADING.name) {
                MarkersLazyLoadingDemo()
            }
            composable(MainDestinations.ANIMATION_JITTER_DEMO.name) {
                AnimationJitterDemo()
            }
        }
    }
}