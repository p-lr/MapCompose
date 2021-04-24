package ovh.plrapps.mapcompose.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onMarkerMove
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.viewmodels.MarkerDemoViewModel

@Composable
fun MarkerDemo(modifier: Modifier = Modifier, viewModel: MarkerDemoViewModel) {
    val markerCount = viewModel.markerCount

    Column(modifier.fillMaxSize()) {
        MapUI(
            modifier.weight(2f),
            state = viewModel.state
        )
        Button(onClick = {
            with(viewModel.state) {
                addMarker("marker$markerCount", 0.5, 0.5) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red)
                            .size(50.dp)
                    )
                }
                enableMarkerDrag("marker$markerCount")
                viewModel.addMarker()
            }
        }, Modifier.padding(8.dp)) {
            Text(text = "Add marker")
        }
    }
}