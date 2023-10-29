package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.viewmodels.LayersVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun LayersDemoSimple(
    modifier: Modifier = Modifier, viewModel: LayersVM = viewModel()
) {
    var satelliteSliderValue by remember {
        mutableFloatStateOf(1f)
    }

    var ignV2SliderValue by remember {
        mutableFloatStateOf(0.5f)
    }

    Column {
        BoxWithConstraints {
            MapUI(modifier.size(maxWidth, maxHeight - 100.dp), state = viewModel.state)
        }
        LayerSlider(
            name = "Satellite",
            value = satelliteSliderValue,
            onValueChange = {
                satelliteSliderValue = it
                viewModel.setSatelliteOpacity(it)
            }
        )
        LayerSlider(
            name = "IGN v2",
            value = ignV2SliderValue,
            onValueChange = {
                ignV2SliderValue = it
                viewModel.setIgnV2Opacity(it)
            }
        )
    }
}

@Composable
private fun LayerSlider(name: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(Modifier.height(50.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, Modifier.padding(horizontal = 16.dp))
            Slider(
                value = value,
                onValueChange = onValueChange
            )
        }
    }
}