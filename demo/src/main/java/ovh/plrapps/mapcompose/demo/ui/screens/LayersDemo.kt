package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.viewmodels.LayersVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun LayersDemoSimple(
    modifier: Modifier = Modifier, viewModel: LayersVM = viewModel()
) {
    var sliderValue by remember {
        mutableStateOf(0.8f)
    }

    Column {
        BoxWithConstraints {
            MapUI(modifier.size(maxWidth, maxHeight - 50.dp), state = viewModel.state)
        }
        Row(Modifier.height(50.dp)) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    viewModel.setSatelliteOpacity(it)
                }
            )
        }
    }
}