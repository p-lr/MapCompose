package ovh.plrapps.mapcompose.demo.providers

import android.content.Context
import ovh.plrapps.mapcompose.core.TileStreamProvider
import java.io.InputStream

fun makeTileStreamProvider(appContext: Context) = object : TileStreamProvider {
    override suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        return try {
            appContext.assets?.open("tiles/mont_blanc/$zoomLvl/$row/$col.jpg")
        } catch (e: Exception) {
            null
        }
    }
}