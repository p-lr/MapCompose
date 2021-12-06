package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.viewmodels.TestPrimaryLayerSwitchVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun TestPrimaryLayerSwitch(
    modifier: Modifier = Modifier, viewModel: TestPrimaryLayerSwitchVM = viewModel()
) {
    MapUI(modifier, state = viewModel.state)
}