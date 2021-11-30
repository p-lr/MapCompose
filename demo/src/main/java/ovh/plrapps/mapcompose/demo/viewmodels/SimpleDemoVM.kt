package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.core.Layer
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class SimpleDemoVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext, "mont_blanc")

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
            shouldLoopScale = true
            enableRotation()
            viewModelScope.launch {
                scrollTo(0.5, 0.5, 1f)
            }
        }
    )

    init {
        viewModelScope.launch {
            delay(4000)
            val newProvider = makeTileStreamProvider(appContext, "mont_blanc_satellite")
            state.setLayers(listOf(Layer("new", newProvider)))
            delay(4000)
            state.removeLayers()
//            state.setPrimaryLayer(newProvider)
//            state.setLayers(listOf(Layer("new", tileStreamProvider)))
        }
    }

    private fun makeTileStreamProvider(appContext: Context, folder: String) =
        TileStreamProvider { row, col, zoomLvl ->
            try {
                appContext.assets?.open("tiles/$folder/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
}