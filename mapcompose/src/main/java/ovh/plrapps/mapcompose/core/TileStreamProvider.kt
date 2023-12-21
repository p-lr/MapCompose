package ovh.plrapps.mapcompose.core

import java.io.InputStream

/**
 * Defines how tiles should be fetched. It must be supplied as part of the configuration of
 * MapCompose.
 *
 * The [getTileStream] method implementation may suspend, but it isn't required (e.g, it isn't
 * required to switch context using withContext(Dispatcher.IO) { .. }) as MapCompose does that
 * already. The [getTileStream] method is declared using the suspend modifier, as it is sometimes
 * useful to provide an implementation which suspends.
 *
 * MapCompose leverages bitmap pooling to reduce the pressure on the garbage collector. However,
 * there's no tile caching by default - this is an implementation detail of the supplied
 * [TileStreamProvider].
 *
 * If [getTileStream] returns null, the tile won't be rendered.
 * The library does not handle exceptions thrown from [getTileStream]. Such errors are treated as
 * unrecoverable failures.
 */
fun interface TileStreamProvider {
    suspend fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream?
}