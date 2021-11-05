package ovh.plrapps.mapcompose.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Limit the rate at which a [block] is called.
 * The [block] execution is triggered upon reception of [Unit] from the returned [SendChannel].
 *
 * @param wait The time in ms between each [block] call.
 *
 * @author peterLaurence
 */
fun CoroutineScope.throttle(wait: Long, block: suspend () -> Unit): SendChannel<Unit> {

    val channel = Channel<Unit>(capacity = 1)
    val flow = channel.receiveAsFlow()
    launch {
        flow.collectLatest {
            block()
            delay(wait)
        }
    }
    return channel
}