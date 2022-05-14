package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.demo.viewmodels.MarkersClusteringVM
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MarkersClusteringDemo(modifier: Modifier = Modifier, viewModel: MarkersClusteringVM = viewModel()) {
    MapUI(modifier, state = viewModel.state)
}