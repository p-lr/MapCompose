package ovh.plrapps.mapcompose.viewmodels

import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.ui.state.MapComposeState

class MapComposeViewModel() : ViewModel() {
    val state: MapComposeState by mutableStateOf(
        MapComposeState(25856, 13056).also {
            it.shouldLoopScale = true
        }
    )

    // Simulate adding and removing a custom composable into the ZoomPanRotate layout
    init {
        viewModelScope.launch {
            delay(3000)
            state.addComposable(12) {
                Button(onClick = { println("hi!") }, Modifier.size(200.dp)) {}
            }

            delay(2000)
            state.removeComposable(12)
        }
    }
}