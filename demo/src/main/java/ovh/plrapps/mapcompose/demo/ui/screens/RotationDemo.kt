package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.api.rotation
import ovh.plrapps.mapcompose.demo.viewmodels.RotationVM
import ovh.plrapps.mapcompose.ui.MapUI
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RotationDemo(modifier: Modifier = Modifier, viewModel: RotationVM = viewModel()) {
    val sliderValue = viewModel.state.rotation / 360f

    Column(modifier.fillMaxSize()) {
        MapUI(
            modifier.weight(2f),
            state = viewModel.state
        )
        Row {
            Button(onClick = { viewModel.onRotate() }, Modifier.padding(8.dp)) {
                Text(text = "Rotate 90°")
            }
            Slider(
                value = sliderValue,
                valueRange = 0f..0.9999f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onValueChange = { v -> viewModel.state.rotation = v * 360f })
        }

    }
}