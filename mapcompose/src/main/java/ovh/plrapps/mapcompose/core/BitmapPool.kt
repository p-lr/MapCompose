package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.concurrent.ConcurrentHashMap

/**
 * A pool of bitmaps, internally split by allocation byte count.
 * This class is thread-safe.
 */
internal class BitmapPool {
    private val pool: ConcurrentHashMap<Int, Channel<Bitmap>> = ConcurrentHashMap<Int, Channel<Bitmap>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun get(allocationByteCount: Int): Bitmap? {
        if (pool[allocationByteCount]?.isEmpty == true) {
            return null
        }
        return pool[allocationByteCount]?.tryReceive()?.getOrNull()
    }

    fun put(b: Bitmap) {
        val allocationByteCount = b.allocationByteCount
        /* Since we can't use pool.computeIfAbsent() on api below 24, we're using manual
         * synchronization */
        if (!pool.containsKey(allocationByteCount)) {
            synchronized(pool) {
                if (!pool.containsKey(allocationByteCount)) {
                    pool[allocationByteCount] = Channel(UNLIMITED)
                }
            }
        }

        pool[allocationByteCount]?.trySend(b)
    }
}