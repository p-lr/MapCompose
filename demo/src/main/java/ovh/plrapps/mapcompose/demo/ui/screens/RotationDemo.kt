@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.api.rotation
import ovh.plrapps.mapcompose.demo.ui.MainDestinations
import ovh.plrapps.mapcompose.demo.viewmodels.RotationVM
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun RotationDemo(modifier: Modifier = Modifier, viewModel: RotationVM = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MainDestinations.MAP_WITH_ROTATION_CONTROLS.title) },
            )
        }
    ) { padding ->
        RotationScreen(
            modifier.padding(padding),
            mapState = viewModel.state,
            onRotate = viewModel::onRotate
        )
    }
}

@Composable
private fun RotationScreen(
    modifier: Modifier = Modifier,
    mapState: MapState,
    onRotate: () -> Unit
) {
    val sliderValue = mapState.rotation / 360f

    Column(modifier.fillMaxSize()) {
        MapUI(
            modifier.weight(2f),
            state = mapState
        )
        Row {
            Button(onClick = onRotate, Modifier.padding(8.dp)) {
                Text(text = "Rotate 90°")
            }
            Slider(
                value = sliderValue,
                valueRange = 0f..0.9999f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onValueChange = { v -> mapState.rotation = v * 360f })
        }
    }
}