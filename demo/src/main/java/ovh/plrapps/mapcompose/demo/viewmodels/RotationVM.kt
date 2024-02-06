package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.rotateTo
import ovh.plrapps.mapcompose.api.rotation
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.setScrollOffsetRatio
import ovh.plrapps.mapcompose.api.setStateChangeListener
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState

class RotationVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    val state = MapState(4, 4096, 4096).apply {
        addLayer(tileStreamProvider)
        enableRotation()
        setScrollOffsetRatio(0.3f, 0.3f)
        scale = 0f

        /* Not useful here, just showing how this API works */
        setStateChangeListener {
            println("scale: $scale, scroll: $scroll, rotation: $rotation")
        }
    }

    fun onRotate() {
        viewModelScope.launch {
            state.rotateTo(state.rotation + 90f)
        }
    }
}