package ovh.plrapps.mapcompose.core

/**
 * A simple pool of objects.
 * This class isn't thread-safe.
 */
internal class Pool<T>(private val threshold: Int = 100) {
    val size: Int
        get() = pool.size

    private val pool = ArrayDeque<T>()

    fun get(): T? = pool.removeFirstOrNull()
    fun put(o: T) {
        if (size < threshold) pool.add(o)
    }
}