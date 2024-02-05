package ovh.plrapps.mapcompose.testapp.features.layerswitch

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.replaceLayer
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class LayerSwitchViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }

    private var type = 0
    private val tileStreamProvider = makeTileStreamProvider(appContext, type)
    private var currentLayerId: String? = null

    val state: MapState = MapState(4, 4096, 4096, workerCount = 64).apply {
        shouldLoopScale = true
        enableRotation()
        viewModelScope.launch {
            scrollTo(0.5, 0.5, 1f)
        }
        currentLayerId = addLayer(tileStreamProvider)
    }

    init {
        viewModelScope.launch {
            while (true) {
                delay(2000)
                changeMapType()
                delay(200)
                changeMapType()
            }
        }
    }

    private fun changeMapType() {
        type = ((0..2) - type).random()
        val tileStreamProvider = makeTileStreamProvider(appContext, type)
        currentLayerId?.also { id ->
            currentLayerId = state.replaceLayer(id, tileStreamProvider)
        }
    }

    private fun makeTileStreamProvider(appContext: Context, type: Int): TileStreamProvider {
        /* Pay attention to how type is captured and immutable in the context of the TileStreamProvider */
        return TileStreamProvider { row, col, _ ->
            runCatching {
                Thread.sleep((100L..200L).random())
                appContext.assets?.open("tiles/test/tile_${type}_${col}_$row.png")
            }.getOrNull()
        }
    }
}