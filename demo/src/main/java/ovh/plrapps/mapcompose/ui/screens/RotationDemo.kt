package ovh.plrapps.mapcompose.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.viewmodels.RotationDemoViewModel

@Composable
fun RotationDemo(modifier: Modifier = Modifier, viewModel: RotationDemoViewModel) {
//    val viewModel: RotationDemoViewModel = viewModel("y")

    MapUI(
        modifier
            .fillMaxSize()
            .background(Color.White),
        state = viewModel.state
    )
}