package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.rotation
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.smoothRotateTo
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.ui.state.MapState

class RotationVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
            enableRotation()
            scale = 0f
        }
    )

    fun onRotate() {
        state.smoothRotateTo(state.rotation + 90f)
    }
}