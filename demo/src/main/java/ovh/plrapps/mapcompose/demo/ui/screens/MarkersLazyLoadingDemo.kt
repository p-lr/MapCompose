package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ovh.plrapps.mapcompose.demo.viewmodels.MarkersLazyLoadingVM
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MarkersLazyLoadingDemo(modifier: Modifier = Modifier, viewModel: MarkersLazyLoadingVM = viewModel()) {
    MapUI(modifier, state = viewModel.state)
}