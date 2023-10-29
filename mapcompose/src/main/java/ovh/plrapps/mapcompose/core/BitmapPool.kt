package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.concurrent.ConcurrentHashMap

/**
 * A pool of bitmaps, internally split by bitmap width.
 * This class is thread-safe.
 */
internal class BitmapPool {
    private val pool: ConcurrentHashMap<Int, Channel<Bitmap>> = ConcurrentHashMap<Int, Channel<Bitmap>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun get(bitmapWidth: Int): Bitmap? {
        if (pool[bitmapWidth]?.isEmpty == true) {
            return null
        }
        return pool[bitmapWidth]?.tryReceive()?.getOrNull()
    }

    fun put(b: Bitmap) {
        val bitmapWidth = b.width
        /* Since we can't use pool.computeIfAbsent() on api below 24, we're using manual
         * synchronization */
        if (!pool.containsKey(bitmapWidth)) {
            synchronized(pool) {
                if (!pool.containsKey(bitmapWidth)) {
                    pool[bitmapWidth] = Channel(UNLIMITED)
                }
            }
        }

        pool[bitmapWidth]?.trySend(b)
    }
}