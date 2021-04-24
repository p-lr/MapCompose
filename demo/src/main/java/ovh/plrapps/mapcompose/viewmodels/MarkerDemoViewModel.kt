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
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.InputStream

class MarkerDemoViewModel(application: Application) : AndroidViewModel(application) {
    val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = object : TileStreamProvider {
        override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
            return try {
                appContext.assets?.open("tiles/mont_blanc/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
    }

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
        }
    )

    fun addMarker() = markerCount++
}