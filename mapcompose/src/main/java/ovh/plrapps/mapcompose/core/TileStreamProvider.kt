package ovh.plrapps.mapcompose.core

import java.io.InputStream

/**
 * The tile provider is user-provided to the MapView. It must be supplied as part of the configuration
 * of MapCompose.
 *
 * MapCompose leverages bitmap pooling to reduce the pressure on the garbage collector. However,
 * there's no tile caching by default - this is an implementation detail of the supplied
 * [TileStreamProvider].
 *
 * If [getTileStream] returns null, the tile is simply ignored by the tile processing machinery.
 * The library does not handle exceptions thrown from [getTileStream]. Such errors are treated as
 * unrecoverable failures.
 */
fun interface TileStreamProvider {
    suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream?
}