package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.demo.viewmodels.PathsVM
import ovh.plrapps.mapcompose.ui.MapUI
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PathsDemo(
    modifier: Modifier = Modifier, viewModel: PathsVM = viewModel()
) {
    MapUI(
        modifier,
        state = viewModel.state
    )
}