package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.ui.widgets.Marker
import ovh.plrapps.mapcompose.ui.state.MapState

class VisibleAreaPaddingVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    val state = MapState(4, 4096, 4096) {
        scale(1.2f)
    }.apply {
        enableRotation()
        addLayer(tileStreamProvider)
        addMarker("m0", 0.5, 0.5) { Marker() }
    }
}
