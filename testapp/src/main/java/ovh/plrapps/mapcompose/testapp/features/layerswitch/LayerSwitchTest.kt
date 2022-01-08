package ovh.plrapps.mapcompose.testapp.features.layerswitch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun LayerSwitchTest(modifier: Modifier = Modifier, viewModel: LayerSwitchViewModel = viewModel()) {
    MapUI(modifier, state = viewModel.state)
}