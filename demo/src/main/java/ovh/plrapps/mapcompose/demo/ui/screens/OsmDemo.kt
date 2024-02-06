package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.ui.MapUI
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.viewmodels.OsmVM

@Composable
fun OsmDemo(
    modifier: Modifier = Modifier, viewModel: OsmVM = viewModel()
) {
    MapUI(
        modifier,
        state = viewModel.state
    )
}