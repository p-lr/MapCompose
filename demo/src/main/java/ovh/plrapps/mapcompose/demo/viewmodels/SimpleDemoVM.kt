package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.maxScale
import ovh.plrapps.mapcompose.api.minScale
import ovh.plrapps.mapcompose.api.onDoubleTap
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class SimpleDemoVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    val state = MapState(4, 8448, 8448) {
        scale(1.2)
    }.apply {
        addLayer(tileStreamProvider)
        shouldLoopScale = true
        enableRotation()
        /**
         * Example [onDoubleTap] callback that implements a different zoom behavior
         */
        onDoubleTap { x, y ->
            viewModelScope.launch {
                if(scale < maxScale) {
                    scrollTo(x, y, maxScale)
                } else {
                    scrollTo(x, y, minScale)
                }
            }
        }
    }
}
