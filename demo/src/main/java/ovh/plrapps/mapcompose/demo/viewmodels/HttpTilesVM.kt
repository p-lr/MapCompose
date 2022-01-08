package ovh.plrapps.mapcompose.demo.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

    val state: MapState by mutableStateOf(
        /* Notice how we increase the worker count when performing HTTP requests */
        MapState(4, 4096, 4096, workerCount = 16).apply {
            addLayer(tileStreamProvider)
            scale = 0f
            shouldLoopScale = true
        }
    )
}

/**
 * A [TileStreamProvider] which performs HTTP requests.
 */
private fun makeTileStreamProvider() =
    TileStreamProvider { row, col, zoomLvl ->
        try {
            val url = URL("https://plrapps.ovh:8080/mapcompose-tile/$zoomLvl/$row/$col.jpg")
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            BufferedInputStream(connection.inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }