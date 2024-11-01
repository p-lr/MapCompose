@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.ui.MainDestinations
import ovh.plrapps.mapcompose.demo.viewmodels.LayersVM
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun LayersDemoSimple(viewModel: LayersVM = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MainDestinations.LAYERS_DEMO.title) },
            )
        }
    ) { padding ->
        LayersDemoScreen(
            Modifier.padding(padding),
            viewModel.state,
            onSatelliteOpacity = viewModel::setSatelliteOpacity,
            onIgnV2Opacity = viewModel::setIgnV2Opacity
        )
    }
}

@Composable
fun LayersDemoScreen(
    modifier: Modifier = Modifier,
    mapState: MapState,
    onSatelliteOpacity: (Float) -> Unit,
    onIgnV2Opacity: (Float) -> Unit
) {
    var satelliteSliderValue by remember {
        mutableFloatStateOf(1f)
    }

    var ignV2SliderValue by remember {
        mutableFloatStateOf(0.5f)
    }

    Column(modifier) {
        MapUI(Modifier.weight(1f), state = mapState)
        LayerSlider(
            name = "Satellite",
            value = satelliteSliderValue,
            onValueChange = {
                satelliteSliderValue = it
                onSatelliteOpacity(it)
            }
        )
        LayerSlider(
            name = "IGN v2",
            value = ignV2SliderValue,
            onValueChange = {
                ignV2SliderValue = it
                onIgnV2Opacity(it)
            }
        )
    }
}

@Composable
private fun LayerSlider(name: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        Modifier
            .height(50.dp)
            .padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, Modifier.padding(horizontal = 16.dp))
            Slider(
                value = value,
                onValueChange = onValueChange
            )
        }
    }
}