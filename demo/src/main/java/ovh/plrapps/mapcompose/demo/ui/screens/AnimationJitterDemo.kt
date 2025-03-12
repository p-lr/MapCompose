@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.demo.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.demo.ui.MainDestinations
import ovh.plrapps.mapcompose.demo.viewmodels.AnimationJitterDemoVM
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState


@Composable
fun AnimationJitterDemo(
    viewModel: AnimationJitterDemoVM = viewModel(),
    onRestart: () -> Unit = viewModel::startAnimation
) {
    Log.i("AnimationJitterDemo", "AnimationJitterDemo")
    AnimationJitterDemo(viewModel.state, onRestart)
}

@Composable
fun AnimationJitterDemo(
    state: MapState,
    onRestart: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MainDestinations.ANIMATION_JITTER_DEMO.title) },
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            MapUI(
                Modifier.fillMaxSize(1f),
                state = state
            ) {
                MapFromImage(state = state)
            }
            Button(onClick = {
                onRestart()
            }, Modifier.padding(8.dp)) {
                Text(text = "Start")
            }
        }
    }
}

@SuppressLint("UseKtx")
@Composable
private fun MapFromImage(
    state: MapState,
) {
    var imageBitmap = remember {
        Bitmap.createBitmap(5000, 5000, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.LightGray.toArgb())
        }.asImageBitmap()
    }

    DefaultCanvas(
        modifier = Modifier.zIndex(-1f), // map "image" is below other layers
        mapState = state,
    ) {
        drawImage(
            image = imageBitmap,
        )
    }
}
