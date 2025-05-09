@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.testapp.features.clustering

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.testapp.core.ui.nav.NavDestinations
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MarkerClusteringUi(
    viewModel: MarkersClusteringViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(NavDestinations.CLUSTERING.title)) },
            )
        }
    ) { padding ->
        MapUI(Modifier.padding(padding), state = viewModel.state)
    }
}