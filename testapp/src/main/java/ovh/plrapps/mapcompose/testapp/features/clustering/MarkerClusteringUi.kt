package ovh.plrapps.mapcompose.testapp.features.clustering

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MarkerClusteringUi(modifier: Modifier = Modifier, viewModel: MarkersClusteringViewModel = viewModel()) {
    MapUI(modifier, state = viewModel.state)
}