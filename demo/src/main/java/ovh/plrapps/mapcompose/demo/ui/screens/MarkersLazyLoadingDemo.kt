@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.ui.MainDestinations
import ovh.plrapps.mapcompose.demo.viewmodels.MarkersLazyLoadingVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MarkersLazyLoadingDemo(
    modifier: Modifier = Modifier,
    viewModel: MarkersLazyLoadingVM = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MainDestinations.MARKERS_LAZY_LOADING.title) },
            )
        }
    ) { padding ->
        MapUI(modifier.padding(padding), state = viewModel.state)
    }
}