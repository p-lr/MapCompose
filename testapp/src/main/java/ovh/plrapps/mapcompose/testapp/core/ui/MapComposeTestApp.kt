package ovh.plrapps.mapcompose.testapp.core.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ovh.plrapps.mapcompose.testapp.core.ui.nav.HOME
import ovh.plrapps.mapcompose.testapp.core.ui.nav.NavDestinations
import ovh.plrapps.mapcompose.testapp.core.ui.theme.MapComposeTheme
import ovh.plrapps.mapcompose.testapp.features.clustering.MarkerClusteringUi
import ovh.plrapps.mapcompose.testapp.features.home.Home
import ovh.plrapps.mapcompose.testapp.features.layerswitch.LayerSwitchTest

@Composable
fun MapComposeTestApp() {
    val navController = rememberNavController()

    MapComposeTheme {
        NavHost(navController, startDestination = HOME) {
            composable(HOME) {
                Home(demoListState = rememberLazyListState()) {
                    navController.navigate(it.name)
                }
            }
            composable(NavDestinations.LAYERS_SWITCH.name) {
                LayerSwitchTest()
            }
            composable(NavDestinations.CLUSTERING.name) {
                MarkerClusteringUi()
            }
        }
    }

}