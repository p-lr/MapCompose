@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.demo.ui.MainDestinations
import ovh.plrapps.mapcompose.demo.viewmodels.AnimationDemoVM
import ovh.plrapps.mapcompose.ui.MapUI


@Composable
fun AnimationDemo(
    viewModel: AnimationDemoVM = viewModel(),
    onRestart: () -> Unit = viewModel::startAnimation
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MainDestinations.ANIMATION_DEMO.title) },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()) {
            MapUI(
                Modifier.weight(1f),
                state = viewModel.state
            )
            Button(onClick = {
                onRestart()
            }, Modifier.padding(8.dp)) {
                Text(text = "Start")
            }
        }
    }
}
