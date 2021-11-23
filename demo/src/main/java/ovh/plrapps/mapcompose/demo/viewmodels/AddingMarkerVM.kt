package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class AddingMarkerVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)

    var markerCount by mutableStateOf(0)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
            onMarkerMove { id, position, _ ->
                println("move $id $position")
            }
            onMarkerClick { id, position ->
                println("marker click $id $position")
            }
            onTap { position ->
                println("on tap $position")
            }
            enableRotation()
            scale = 0f // zoom-out to minimum scale
        }
    )

    fun addMarker() = markerCount++
}