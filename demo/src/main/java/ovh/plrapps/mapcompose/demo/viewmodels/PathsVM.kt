package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.ui.widgets.Callout
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

            /**
             * To demonstrate path click, add a callout.
             */
            onPathClick { id, x, y ->
                var shouldAnimate by mutableStateOf(true)
                addCallout(
                    id, x, y,
                    absoluteOffset = Offset(0f, -20f),
                ) {
                    Callout(x, y, title = id, shouldAnimate) {
                        shouldAnimate = false
                    }
                }
            }

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

            addPath(trackName, color = color, clickable = true) {
                for (line in lines) {
                    val values = line.split(',').map(String::toDouble)
                    addPoint(values[0], values[1])
                }
            }
        }
    }
}