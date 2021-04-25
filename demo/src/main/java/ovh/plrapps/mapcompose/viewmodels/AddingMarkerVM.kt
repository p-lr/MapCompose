package ovh.plrapps.mapcompose.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onMarkerMove
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class AddingMarkerVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)

    var markerCount by mutableStateOf(0)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
            onMarkerMove { id, x, y, _, _ ->
                println("move $id $x $y")
            }
            onMarkerClick { id, x, y ->
                println("tap $id $x $y")
            }
            enableRotation()
            scale = 0f // zoom-out to minimum scale
        }
    )

    fun addMarker() = markerCount++
}