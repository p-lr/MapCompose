package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class CalloutVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)
    private val markers = listOf(
        MarkerInfo("Callout #1", 0.14, 0.17),
        MarkerInfo("Callout #2", 0.24, 0.1),
        MarkerInfo("Tap me to dismiss", 0.4, 0.3)
    )

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
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

            scale = 1f
        }
    )
}

private data class MarkerInfo(val id: String, val x: Double, val y: Double)