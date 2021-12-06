package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setPrimaryLayer
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class TestPrimaryLayerSwitchVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }

    private var type = 0
    private val tileStreamProvider = makeTileStreamProvider(appContext, type)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider, workerCount = 64).apply {
            shouldLoopScale = true
            enableRotation()
            viewModelScope.launch {
                scrollTo(0.5, 0.5, 1f)
            }
        }
    )

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
        state.setPrimaryLayer(tileStreamProvider)
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