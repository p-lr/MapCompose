package ovh.plrapps.mapcompose.core

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

/**
 * A simple pool of objects.
 * This class is thread-safe.
 */
internal class Pool<T>(threshold: Int = 300) {
    private val _size = AtomicInteger(0)
    val size: Int
        get() = _size.get()

    private val pool = Channel<T>(threshold)

    fun get(): T? {
        return pool.tryReceive().getOrNull().also {
            if (it != null) _size.decrementAndGet()
        }
    }
    fun put(o: T) {
        pool.trySend(o).also {
            if (it.isSuccess) _size.incrementAndGet()
        }
    }
}