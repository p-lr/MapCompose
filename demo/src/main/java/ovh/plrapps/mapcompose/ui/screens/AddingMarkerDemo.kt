package ovh.plrapps.mapcompose.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.R
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.viewmodels.AddingMarkerVM

@Composable
fun AddingMarkerDemo(modifier: Modifier = Modifier, viewModel: AddingMarkerVM) {
    val markerCount = viewModel.markerCount

    Column(modifier.fillMaxSize()) {
        MapUI(
            modifier.weight(2f),
            state = viewModel.state
        )
        Button(onClick = {
            with(viewModel.state) {
                addMarker("marker$markerCount", 0.5, 0.5) {
                    Icon(
                        painter = painterResource(id = R.drawable.map_marker),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xCC2196F3)
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