package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.onCalloutClick
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.removeCallout
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.ui.widgets.Callout
import ovh.plrapps.mapcompose.ui.state.MapState

class CalloutVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    /* Define the markers data (id and position) */
    private val markers = listOf(
        MarkerInfo("Callout #1", 0.45, 0.6),
        MarkerInfo("Callout #2", 0.24, 0.1),
        MarkerInfo("Callout #3", 0.25, 0.18),
        MarkerInfo(TAP_TO_DISMISS_ID, 0.4, 0.3)
    )

    val state = MapState(4, 4096, 4096).apply {
        addLayer(tileStreamProvider)

        /* Add all markers */
        for (marker in markers) {
            addMarker(marker.id, marker.x, marker.y) {
                Icon(
                    painter = painterResource(id = R.drawable.map_marker),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xCC2196F3)
                )
            }
        }

        scale = 0f

        /**
         * On marker click, add a callout. If the id is [TAP_TO_DISMISS_ID], set auto-dismiss
         * to false. For this particular id, we programmatically remove the callout on tap.
         */
        onMarkerClick { id, x, y ->
            var shouldAnimate by mutableStateOf(true)
            addCallout(
                id, x, y,
                absoluteOffset = Offset(0f, -130f),
                autoDismiss = id != TAP_TO_DISMISS_ID,
                clickable = id == TAP_TO_DISMISS_ID
            ) {
                Callout(x, y, title = id, shouldAnimate) {
                    shouldAnimate = false
                }
            }
        }

        /**
         * Register a click listener on callouts. We don't need to remove the other callouts
         * because they automatically dismiss on touch down.
         */
        onCalloutClick { id, _, _ ->
            if (id == TAP_TO_DISMISS_ID) removeCallout(TAP_TO_DISMISS_ID)
        }
    }

}

private data class MarkerInfo(val id: String, val x: Double, val y: Double)

private const val TAP_TO_DISMISS_ID = "Tap me to dismiss"