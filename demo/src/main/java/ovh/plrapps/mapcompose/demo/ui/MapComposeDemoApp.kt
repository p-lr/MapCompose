package ovh.plrapps.mapcompose.demo.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import ovh.plrapps.mapcompose.demo.ui.screens.*
import ovh.plrapps.mapcompose.demo.ui.theme.MapComposeTheme
import ovh.plrapps.mapcompose.demo.viewmodels.AddingMarkerVM
import ovh.plrapps.mapcompose.demo.viewmodels.CenteringOnMarkerVM
import ovh.plrapps.mapcompose.demo.viewmodels.RotationVM
import ovh.plrapps.mapcompose.demo.viewmodels.SimpleDemoVM

@Composable
fun MapComposeDemoApp() {
    val navController = rememberNavController()

    val simpleDemoVM: SimpleDemoVM = viewModel()
    val rotationVM: RotationVM = viewModel()
    val addingMarkerVM: AddingMarkerVM = viewModel()
    val centeringOnMarkerVM: CenteringOnMarkerVM = viewModel()

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
                RotationDemo(viewModel = rotationVM)
            }
            composable(MainDestinations.ADDING_MARKERS.name) {
                AddingMarkerDemo(viewModel = addingMarkerVM)
            }
            composable(MainDestinations.CENTERING_ON_MARKER.name) {
                CenteringOnMarkerDemo(
                    viewModel = centeringOnMarkerVM,
                    onCenter = centeringOnMarkerVM::onCenter
                )
            }
        }
    }
}