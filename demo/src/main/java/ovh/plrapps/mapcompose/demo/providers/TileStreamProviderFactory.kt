package ovh.plrapps.mapcompose.demo.providers

import android.content.Context
import ovh.plrapps.mapcompose.core.TileStreamProvider

fun makeTileStreamProvider(appContext: Context) =
    TileStreamProvider { row, col, zoomLvl ->
        try {
            appContext.assets?.open("tiles/mont_blanc_layered/$zoomLvl/$row/$col.jpg")
        } catch (e: Exception) {
            null
        }
    }
