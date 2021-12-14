package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.state.InitialValues
import ovh.plrapps.mapcompose.ui.state.MapState

class SimpleDemoVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)

    val state: MapState by mutableStateOf(
        MapState(
            4, 4096, 4096,
            initialValues = InitialValues().scroll(0.5, 0.5).scale(1.2f)
        ).apply {
            addLayer(tileStreamProvider)
            shouldLoopScale = true
            enableRotation()
        }
    )
}