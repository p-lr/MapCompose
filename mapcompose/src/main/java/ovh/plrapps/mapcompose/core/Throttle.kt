package ovh.plrapps.mapview.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
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
fun CoroutineScope.throttle(wait: Long, block: () -> Unit): SendChannel<Unit> {

    val channel = Channel<Unit>(capacity = 1)
    val flow = channel.receiveAsFlow()
    launch {
        flow.collect {
            block()
            delay(wait)
        }
    }
    return channel
}