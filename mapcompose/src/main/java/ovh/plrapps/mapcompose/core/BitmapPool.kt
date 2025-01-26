package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * A pool of bitmaps, internally split by allocation byte count.
 * This class is thread-safe.
 */
internal class BitmapPool(coroutineContext: CoroutineContext) {
    private val mutex = Mutex()
    private val pool = mutableMapOf<Int, Channel<Bitmap>>()
    private val receiveChannel = Channel<Bitmap>(capacity = UNLIMITED)
    private val scope = CoroutineScope(coroutineContext)

    init {
        scope.launch {
            for (b in receiveChannel) {
                mutex.withLock {
                    val allocationByteCount = b.allocationByteCount

                    if (!pool.containsKey(allocationByteCount)) {
                        pool[allocationByteCount] = Channel(UNLIMITED)
                    }

                    pool[allocationByteCount]?.trySend(b)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun get(allocationByteCount: Int): Bitmap? {
        mutex.withLock {
            if (pool[allocationByteCount]?.isEmpty == true) {
                return null
            }
            return pool[allocationByteCount]?.tryReceive()?.getOrNull()
        }
    }

    /**
     * Don't make this method a suspending call. It causes ConcurrentModificationExceptions because
     * some collection iteration become interleaved.
     */
    fun put(b: Bitmap) {
        receiveChannel.trySend(b)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() = scope.launch {
        mutex.withLock {
            coroutineScope {
                pool.forEach { (k, v) ->
                    val channel= pool[k]
                    if (channel != null) {
                        launch {
                            for (b in channel) {
                                b.recycle()
                                if (channel.isEmpty) {
                                    cancel()
                                }
                            }
                        }
                    }
                }
            }
            scope.cancel()
        }
    }
}