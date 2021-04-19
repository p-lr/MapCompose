package ovh.plrapps.mapcompose.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.ui.MapCompose
import ovh.plrapps.mapcompose.viewmodels.MapComposeViewModel


@Composable
fun MapDemo(modifier: Modifier = Modifier) {
    val viewModel: MapComposeViewModel = viewModel()
    MapCompose(
        modifier
            .size(350.dp, 500.dp)
            .background(Color.Black),
        state = viewModel.state
    )
}
