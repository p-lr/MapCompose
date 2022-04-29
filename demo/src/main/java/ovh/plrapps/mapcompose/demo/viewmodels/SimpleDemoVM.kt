package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class SimpleDemoVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096) {
            scroll(0.5, 0.5)
            scale(1.2f)
        }.apply {
            addLayer(tileStreamProvider)
            shouldLoopScale = true
            enableRotation()
        }
    )
}