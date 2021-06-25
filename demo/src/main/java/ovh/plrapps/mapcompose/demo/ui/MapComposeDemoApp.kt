package ovh.plrapps.mapcompose.demo.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ovh.plrapps.mapcompose.demo.ui.screens.*
import ovh.plrapps.mapcompose.demo.ui.theme.MapComposeTheme
import ovh.plrapps.mapcompose.demo.viewmodels.*

@Composable
fun MapComposeDemoApp() {
    val navController = rememberNavController()

    val simpleDemoVM: SimpleDemoVM = viewModel()
    val rotationVM: RotationVM = viewModel()
    val addingMarkerVM: AddingMarkerVM = viewModel()
    val centeringOnMarkerVM: CenteringOnMarkerVM = viewModel()
    val pathsVM: PathsVM = viewModel()
    val customDrawVM: CustomDrawVM = viewModel()
    val calloutVM: CalloutVM = viewModel()
    val animationVM: AnimationDemoVM = viewModel()
    val httpTilesVM: HttpTilesVM = viewModel()

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
            composable(MainDestinations.PATHS.name) {
                PathsDemo(viewModel = pathsVM)
            }
            composable(MainDestinations.CUSTOM_DRAW.name) {
                CustomDraw(viewModel = customDrawVM)
            }
            composable(MainDestinations.CALLOUT_DEMO.name) {
                CalloutDemo(viewModel = calloutVM)
            }
            composable(MainDestinations.ANIMATION_DEMO.name) {
                AnimationDemo(viewModel = animationVM, onRestart = animationVM::startAnimation)
            }
            composable(MainDestinations.HTTP_TILES_DEMO.name) {
                HttpTilesDemo(viewModel = httpTilesVM)
            }
        }
    }
}