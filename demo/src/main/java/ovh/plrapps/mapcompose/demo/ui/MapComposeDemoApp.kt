package ovh.plrapps.mapcompose.demo.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ovh.plrapps.mapcompose.demo.ui.screens.*
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
                CustomDraw()
            }
            composable(MainDestinations.CALLOUT_DEMO.name) {
                CalloutDemo()
            }
            composable(MainDestinations.ANIMATION_DEMO.name) {
                AnimationDemo()
            }
            composable(MainDestinations.HTTP_TILES_DEMO.name) {
                HttpTilesDemo()
            }
        }
    }
}