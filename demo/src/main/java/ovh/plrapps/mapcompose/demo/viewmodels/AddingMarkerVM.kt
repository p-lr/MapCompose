package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.core.Layer
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class AddingMarkerVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)

    var markerCount by mutableStateOf(0)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096).apply {
            setLayer(Layer("main", tileStreamProvider))
            onMarkerMove { id, x, y, _, _ ->
                println("move $id $x $y")
            }
            onMarkerClick { id, x, y ->
                println("marker click $id $x $y")
            }
            onTap { x, y ->
                println("on tap $x $y")
            }
            enableRotation()
            scale = 0f // zoom-out to minimum scale
        }
    )

    fun addMarker() = markerCount++
}