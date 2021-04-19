package ovh.plrapps.mapcompose.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import ovh.plrapps.mapcompose.ui.screens.Home
import ovh.plrapps.mapcompose.ui.screens.MapDemo
import ovh.plrapps.mapcompose.ui.theme.MapComposeTheme

@Composable
fun MapComposeDemoApp() {
    val navController = rememberNavController()

    MapComposeTheme {
        NavHost(navController, startDestination = MainDestinations.HOME.name) {
            composable(MainDestinations.HOME.name) {
                Home(demoListState = rememberLazyListState()) {
                    navController.navigate(it.name)
                }
            }
            composable(MainDestinations.MAP_ALONE.name) { MapDemo() }
        }
    }
}