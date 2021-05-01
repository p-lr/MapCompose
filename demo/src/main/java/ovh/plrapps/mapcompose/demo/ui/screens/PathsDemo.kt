package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ovh.plrapps.mapcompose.demo.ui.MapUI
import ovh.plrapps.mapcompose.demo.viewmodels.PathsVM

@Composable
fun PathsDemo(
    modifier: Modifier = Modifier, viewModel: PathsVM
) {
    MapUI(
        modifier
            .fillMaxSize()
            .background(Color.White),
        state = viewModel.state
    )
}