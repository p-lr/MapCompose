package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.demo.viewmodels.HttpTilesVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun HttpTilesDemo(
    modifier: Modifier = Modifier, viewModel: HttpTilesVM
) {
    MapUI(
        modifier,
        state = viewModel.state
    )
}