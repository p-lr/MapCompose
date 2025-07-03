package ovh.plrapps.mapcompose.demo.viewmodels

import androidx.lifecycle.ViewModel
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Shows how MapCompose behaves with remote HTTP tiles.
 */
class HttpTilesVM : ViewModel() {
    private val tileStreamProvider = makeTileStreamProvider()

    val state = MapState(
        levelCount = 4,
        fullWidth = 8192,
        fullHeight = 8192,
        workerCount = 16  // Notice how we increase the worker count when performing HTTP requests
    ).apply {
        addLayer(tileStreamProvider)
        scale = 0.0
        shouldLoopScale = true
    }
}

/**
 * A [TileStreamProvider] which performs HTTP requests.
 */
private fun makeTileStreamProvider() =
    TileStreamProvider { row, col, zoomLvl ->
        try {
            val url =
                URL("https://raw.githubusercontent.com/p-lr/MapCompose/master/demo/src/main/assets/tiles/mont_blanc_layered/$zoomLvl/$row/$col.jpg")
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            BufferedInputStream(connection.inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }