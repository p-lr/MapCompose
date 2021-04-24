package ovh.plrapps.mapcompose.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import ovh.plrapps.mapcompose.ui.screens.Home
import ovh.plrapps.mapcompose.ui.screens.MapDemoSimple
import ovh.plrapps.mapcompose.ui.screens.MarkerDemo
import ovh.plrapps.mapcompose.ui.screens.RotationDemo
import ovh.plrapps.mapcompose.ui.theme.MapComposeTheme
import ovh.plrapps.mapcompose.viewmodels.MarkerDemoViewModel
import ovh.plrapps.mapcompose.viewmodels.RotationDemoViewModel
import ovh.plrapps.mapcompose.viewmodels.SimpleDemoViewModel

@Composable
fun MapComposeDemoApp() {
    val navController = rememberNavController()

    val simpleDemoVM: SimpleDemoViewModel = viewModel()
    val rotationDemoVM: RotationDemoViewModel = viewModel()
    val markerDemoVM: MarkerDemoViewModel = viewModel()

    MapComposeTheme {
        NavHost(navController, startDestination = HOME) {
            composable(HOME) {
                Home(demoListState = rememberLazyListState()) {
                    navController.navigate(it.name)
                }
            }
            composable(MainDestinations.MAP_ALONE.name) {
                MapDemoSimple(viewModel = simpleDemoVM)
            }
            composable(MainDestinations.MAP_WITH_ROTATION_CONTROLS.name) {
                RotationDemo(viewModel = rotationDemoVM)
            }
            composable(MainDestinations.MAP_WITH_MARKERS.name) {
                MarkerDemo(viewModel = markerDemoVM)
            }
        }
    }
}