package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setLayerOpacity
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class LayersVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider =
        makeTileStreamProvider(application.applicationContext, imageExt = ".jpg")
    private val slopesLayerProvider =
        makeTileStreamProvider(application.applicationContext, "ign-slopes-", imageExt = ".png")
    private val roadLayerProvider =
        makeTileStreamProvider(application.applicationContext, "ign-road-", imageExt = ".png")

    private var slopesId: String? = null
    private var roadId: String? = null

    val state = MapState(4, 8448, 8448).apply {
        shouldLoopScale = true
        enableRotation()
        viewModelScope.launch {
            scrollTo(0.4, 0.4, 1f)
        }

        addLayer(tileStreamProvider)
        slopesId = addLayer(slopesLayerProvider, initialOpacity = 0.6f)
        roadId = addLayer(roadLayerProvider, initialOpacity = 1f)
    }

    private fun makeTileStreamProvider(appContext: Context, layer: String = "", imageExt: String) =
        TileStreamProvider { row, col, zoomLvl ->
            try {
                appContext.assets?.open("tiles/mont_blanc_layered/$zoomLvl/$row/$layer$col$imageExt")
            } catch (e: Exception) {
                null
            }
        }

    fun setSlopesOpacity(opacity: Float) {
        slopesId?.also { id ->
            state.setLayerOpacity(id, opacity)
        }

    }

    fun setRoadOpacity(opacity: Float) {
        roadId?.also { id ->
            state.setLayerOpacity(id, opacity)
        }
    }
}