package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.demo.viewmodels.AddingMarkerVM
import ovh.plrapps.mapcompose.ui.MapUI
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddingMarkerDemo(
    modifier: Modifier = Modifier,
    viewModel: AddingMarkerVM = viewModel(),
) {
    Column(modifier.fillMaxSize()) {
        MapUI(
            modifier.weight(2f),
            state = viewModel.state
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                viewModel.addMarker()
            }, Modifier.padding(8.dp)) {
                Text(text = "Add marker")
            }

            Text("Drag markers with finger")
        }
    }
}