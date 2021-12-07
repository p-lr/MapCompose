package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.viewmodels.TestLayerSwitchVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun TestLayerSwitch(
    modifier: Modifier = Modifier, viewModel: TestLayerSwitchVM = viewModel()
) {
    MapUI(modifier, state = viewModel.state)
}