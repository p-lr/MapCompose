package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A pool of bitmaps, internally split by allocation byte count.
 * This class is thread-safe.
 */
internal class BitmapPool {
    private val mutex = Mutex()
    private val pool = mutableMapOf<Int, Channel<Bitmap>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun get(allocationByteCount: Int): Bitmap? {
        mutex.withLock {
            if (pool[allocationByteCount]?.isEmpty == true) {
                return null
            }
            return pool[allocationByteCount]?.tryReceive()?.getOrNull()
        }
    }

    suspend fun put(b: Bitmap) {
        mutex.withLock {
            val allocationByteCount = b.allocationByteCount

            if (!pool.containsKey(allocationByteCount)) {
                pool[allocationByteCount] = Channel(UNLIMITED)
            }

            pool[allocationByteCount]?.trySend(b)
        }
    }
}