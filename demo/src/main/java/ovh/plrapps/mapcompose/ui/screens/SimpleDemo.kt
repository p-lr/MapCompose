package ovh.plrapps.mapcompose.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.viewmodels.SimpleDemoViewModel


@Composable
fun MapDemoSimple(
    modifier: Modifier = Modifier, viewModel: SimpleDemoViewModel
) {
    MapUI(
        modifier
            .fillMaxSize()
            .background(Color.White),
        state = viewModel.state
    )
}
