package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * In this sample, we add "tracks" to the map. The tracks are rendered as paths using MapCompose.
 */
class PathsVM(application: Application) : AndroidViewModel(application) {
    private val appContext: Context by lazy {
        getApplication<Application>().applicationContext
    }
    private val tileStreamProvider = makeTileStreamProvider(appContext)

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096, tileStreamProvider).apply {
            shouldLoopScale = true
            enableRotation()
            scrollToAndCenter(0.72, 0.3)
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
     * To add a path, follow these steps:
     *
     * 1. Retrieve a [makePathDataBuilder] from the [MapState] instance, using [makePathDataBuilder]
     * 2. Add each point using [PathDataBuilder.addPoint]
     * 3. Build a [PathData] using [PathDataBuilder.build]
     * 4. Add the path to the map using [addPath]
     */
    private fun addTrack(trackName: String, color: Color? = null) {
        with(state) {
            val lines = appContext.assets?.open("tracks/$trackName.txt")?.bufferedReader()?.lines()
                ?: return@with
            val builder = makePathDataBuilder()
            for (line in lines) {
                val values = line.split(',')
                builder.addPoint(values[0].toDouble(), values[1].toDouble())
            }
            val data1 = builder.build() ?: return@with
            addPath(trackName, data1, color = color, width = 12.dp)
        }
    }
}