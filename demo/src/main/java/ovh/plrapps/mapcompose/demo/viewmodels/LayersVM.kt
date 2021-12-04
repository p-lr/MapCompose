package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.core.Layer
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class LayersVM (application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext, "mont_blanc")
    private val satelliteProvider = makeTileStreamProvider(appContext, "mont_blanc_satellite")
    private val ignV2Provider = makeTileStreamProvider(appContext, "mont_blanc_ignv2")

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
            shouldLoopScale = true
            enableRotation()
            viewModelScope.launch {
                scrollTo(0.5, 0.5, 1f)
            }
        }.apply {
            setLayers(listOf(
                Layer(satelliteId, satelliteProvider),
                Layer(ignV2Id, ignV2Provider, 0.5f)
            ))
        }
    )

    private fun makeTileStreamProvider(appContext: Context, folder: String) =
        TileStreamProvider { row, col, zoomLvl ->
            try {
                appContext.assets?.open("tiles/$folder/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }

    fun setSatelliteOpacity(opacity: Float) {
        state.setLayerOpacity(satelliteId, opacity)
    }

    fun setIgnV2Opacity(opacity: Float) {
        state.setLayerOpacity(ignV2Id, opacity)
    }
}

private const val satelliteId = "satellite"
private const val ignV2Id = "ignV2"