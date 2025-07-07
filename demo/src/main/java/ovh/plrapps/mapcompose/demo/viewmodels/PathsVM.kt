package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.onPathClick
import ovh.plrapps.mapcompose.api.onPathLongPress
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.ui.widgets.Callout
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem.Dash
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem.Gap
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * In this sample, we add "tracks" to the map. The tracks are rendered as paths using MapCompose.
 */
class PathsVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    val state = MapState(4, 8192, 8192).apply {
        addLayer(tileStreamProvider)
        shouldLoopScale = true
        enableRotation()

        /**
         * Demonstrates path click.
         */
        onPathClick { id, x, y ->
            var shouldAnimate by mutableStateOf(true)
            addCallout(
                id, x, y,
                absoluteOffset = DpOffset(0.dp, (-10).dp),
            ) {
                Callout(x, y, title = "Click on $id", shouldAnimate) {
                    shouldAnimate = false
                }
            }
        }

        /**
         * Demonstrates path long-press.
         */
        onPathLongPress { id, x, y ->
            var shouldAnimate by mutableStateOf(true)
            addCallout(
                id, x, y,
                absoluteOffset = DpOffset(0.dp, (-10).dp),
            ) {
                Callout(x, y, title = "Long-press on $id", shouldAnimate) {
                    shouldAnimate = false
                }
            }
        }

        viewModelScope.launch {
            scrollTo(0.72, 0.3)
        }
    }


    init {
        /* Add tracks */
        addTrack("track1", Color(0xFF448AFF))
        addTrack("track2", Color(0xFFFFFF00))
        addTrack("track3", pattern = listOf(Dash(8.dp), Gap(4.dp)))

        // filled polygon
        with(state) {
            addPath(
                id = "filled polygon",
                color = Color.Green,
                fillColor = Color.Green.copy(alpha = .6f),
            ) {
                // Pentagon
                addPoint(0.2009, 0.17878)
                addPoint(0.08909, 0.2151)
                addPoint(0.01999, 0.12)
                addPoint(0.08909, 0.02489)
                addPoint(0.2009, 0.06122)
            }
        }
    }

    /**
     * In this sample, we retrieve track points from text files in the assets.
     * To add a path, use the [addPath] api. From inside the builder block, you can add individual
     * points or a list of points.
     * Here, since we're getting points from a sequence, we add them on the fly using [PathDataBuilder.addPoint].
     */
    private fun addTrack(
        trackName: String,
        color: Color? = null,
        pattern: List<PatternItem>? = null,
        clickable: Boolean = true
    ) {
        with(state) {
            val lines = getApplication<Application>().applicationContext.assets?.open(
                "tracks/$trackName.txt"
            )?.bufferedReader()?.lineSequence()
                ?: return@with

            addPath(
                id = trackName, color = color, clickable = clickable, pattern = pattern, offset = 1
            ) {
                for (line in lines) {
                    val values = line.split(',').map(String::toDouble)
                    addPoint(values[0], values[1])
                }
            }
        }
    }
}