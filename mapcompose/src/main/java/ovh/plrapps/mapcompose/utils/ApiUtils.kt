package ovh.plrapps.mapcompose.utils

import kotlinx.coroutines.delay

internal suspend fun withRetry(maxRetry: Int, intervalMs: Long, block: suspend () -> Boolean) {
    var cnt = 0
    var res = block()
    while (!res && cnt < maxRetry) {
        delay(intervalMs)
        res = block()
        cnt++
    }
}