@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.ui.MainDestinations
import ovh.plrapps.mapcompose.demo.viewmodels.AddingMarkerVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun AddingMarkerDemo(
    modifier: Modifier = Modifier,
    viewModel: AddingMarkerVM = viewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MainDestinations.ADDING_MARKERS.title) },
            )
        }
    ) { padding ->
        Column(
            modifier
                .padding(padding)
                .fillMaxSize()) {
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
}