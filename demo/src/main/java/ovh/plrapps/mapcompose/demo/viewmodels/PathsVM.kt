package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * In this sample, we add "tracks" to the map. The tracks are rendered as paths using MapCompose.
 */
class PathsVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096).apply {
            addLayer(tileStreamProvider)
            shouldLoopScale = true
            enableRotation()
            viewModelScope.launch {
                scrollTo(0.72, 0.3)
            }
        }
    )

    init {
        /* Add tracks */
        addTrack("track1", Color(0xFF448AFF))
        addTrack("track2", Color(0xFFFFFF00))
        addTrack("track3") // 0xFF448AFF is the default color
    }

    /**
     * In this sample, we retrieve track points from text files in the assets.
     * To add a path, use the [addPath] api. From inside the builder block, you can add individual
     * points or a list of points.
     * Here, since we're getting points from a sequence, we add them on the fly using [PathDataBuilder.addPoint].
     */
    private fun addTrack(trackName: String, color: Color? = null) {
        with(state) {
            val lines = getApplication<Application>().applicationContext.assets?.open(
                "tracks/$trackName.txt"
            )?.bufferedReader()?.lineSequence()
                ?: return@with

            addPath(trackName, color = color, width = 12.dp) {
                for (line in lines) {
                    val values = line.split(',').map(String::toDouble)
                    addPoint(values[0], values[1])
                }
            }
        }
    }
}