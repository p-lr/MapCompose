package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.demo.ui.widgets.Callout
import ovh.plrapps.mapcompose.demo.viewmodels.CalloutVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun CalloutDemo(
    modifier: Modifier = Modifier,
    viewModel: CalloutVM
) {
    /* Add a callout on marker click */
    viewModel.state.apply {
        onMarkerClick { id, x, y ->
            var shouldAnimate by mutableStateOf(true)
            addCallout(
                id, x, y,
                absoluteOffset = Offset(0f, -130f),
                autoDismiss = id != "Tap me to dismiss"
            ) {
                Callout(x, y, title = id, shouldAnimate) {
                    shouldAnimate = false
                }
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        MapUI(
            modifier.weight(2f),
            state = viewModel.state
        )
    }
}
