package ovh.plrapps.mapcompose.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.InputStream

class MapViewModel(application: Application) : AndroidViewModel(application) {
    val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    val tileStreamProvider = object : TileStreamProvider {
        override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
            return try {
                appContext.assets?.open("tiles/esp/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
    }

    val state: MapState by mutableStateOf(
        MapState(5, 8192, 8192, tileStreamProvider).also {
            it.shouldLoopScale = true
        }
    )
}