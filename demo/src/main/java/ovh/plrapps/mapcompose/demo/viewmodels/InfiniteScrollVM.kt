package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class InfiniteScrollVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeWorldTileStreamProvider(application.applicationContext)

    val state = MapState(5, 8192, 8192) {
        scale(0.1)
        infiniteScrollX(true)
    }.apply {
        addLayer(tileStreamProvider)
        shouldLoopScale = true
        enableRotation()
    }

}

private fun makeWorldTileStreamProvider(appContext: Context) =
    TileStreamProvider { row, col, zoomLvl ->
        try {
            appContext.assets?.open("tiles/world/$zoomLvl/$row/$col.jpg")
        } catch (e: Exception) {
            null
        }
    }