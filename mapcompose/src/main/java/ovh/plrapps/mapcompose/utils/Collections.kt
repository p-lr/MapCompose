package ovh.plrapps.mapcompose.utils

fun <T> MutableCollection<T>.removeFirst(predicate: (T) -> Boolean): Boolean {
    var removed = false
    val it = iterator()
    while (it.hasNext()) {
        if (predicate(it.next())) {
            it.remove()
            removed = true
            break
        }
    }
    return removed
}