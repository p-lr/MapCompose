package ovh.plrapps.mapcompose.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

/**
 * Limit the rate at which a [block] is called.
 * The [block] execution is triggered upon reception of [Unit] from the returned [MutableSharedFlow].
 *
 * @param wait The time in ms between each [block] call.
 *
 * @author P.Laurence
 */
fun CoroutineScope.throttle(wait: Long, block: suspend () -> Unit): MutableSharedFlow<Unit> {
    val flow = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)

    launch {
        flow.sample(wait).collect {
            block()
            delay(wait)
        }
    }
    return flow
}