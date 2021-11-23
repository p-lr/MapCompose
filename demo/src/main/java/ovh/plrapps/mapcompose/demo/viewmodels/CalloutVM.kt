package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
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
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.ui.widgets.Callout
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.Point

class CalloutVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)

    /* Define the markers data (id and position) */
    private val markers = listOf(
        MarkerInfo("Callout #1", Point(0.45, 0.6)),
        MarkerInfo("Callout #2", Point(0.24, 0.1)),
        MarkerInfo(TAP_TO_DISMISS_ID, Point(0.4, 0.3))
    )

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
            /* Add all markers */
            for (marker in markers) {
                addMarker(marker.id, marker.position) {
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
            onMarkerClick { id, position ->
                var shouldAnimate by mutableStateOf(true)
                addCallout(
                    id, position,
                    absoluteOffset = Offset(0f, -130f),
                    autoDismiss = id != TAP_TO_DISMISS_ID,
                    clickable = id == TAP_TO_DISMISS_ID
                ) {
                    Callout(position, title = id, shouldAnimate) {
                        shouldAnimate = false
                    }
                }
            }

            /**
             * Register a click listener on callouts. We don't need to remove the other callouts
             * because they automatically dismiss on touch down.
             */
            onCalloutClick { id, _ ->
                if (id == TAP_TO_DISMISS_ID) removeCallout(TAP_TO_DISMISS_ID)
            }
        }
    )
}

private data class MarkerInfo(val id: String, val position: Point)
private const val TAP_TO_DISMISS_ID = "Tap me to dismiss"