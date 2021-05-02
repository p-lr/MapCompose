package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.demo.viewmodels.SimpleDemoVM
import ovh.plrapps.mapcompose.ui.MapUI


@Composable
fun MapDemoSimple(
    modifier: Modifier = Modifier, viewModel: SimpleDemoVM
) {
    MapUI(modifier, state = viewModel.state)
}
