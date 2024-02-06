package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class AddingMarkerVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    private var markerCount = 0

    val state = MapState(4, 4096, 4096) {
        scale(0f) // zoom-out to minimum scale
    }.apply {
        addLayer(tileStreamProvider)
        onMarkerMove { id, x, y, _, _ ->
            println("move $id $x $y")
        }
        onMarkerClick { id, x, y ->
            println("marker click $id $x $y")
        }
        onMarkerLongPress { id, x, y ->
            println("on marker long press $id $x $y")
        }
        onTap { x, y ->
            println("on tap $x $y")
        }
        onLongPress { x, y ->
            println("on long press $x $y")
        }
        enableRotation()
        setScrollOffsetRatio(0.5f, 0.5f)
    }


    fun addMarker() {
        state.addMarker("marker$markerCount", 0.5, 0.5) {
            Icon(
                painter = painterResource(id = R.drawable.map_marker),
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color(0xCC2196F3)
            )
        }
        state.enableMarkerDrag("marker$markerCount")
        markerCount++
    }
}