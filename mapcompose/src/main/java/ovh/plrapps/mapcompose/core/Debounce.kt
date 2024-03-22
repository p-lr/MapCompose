package ovh.plrapps.mapcompose.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * So long as the returned [SendChannel] receives [T] elements, the provided [block] function isn't
 * executed until a time-span of [timeoutMillis] elapses.
 * When [block] is executed, it's provided with the last [T] value sent to the channel.
 */
fun <T> CoroutineScope.debounce(
    timeoutMillis: Long,
    block: suspend (T) -> Unit
): SendChannel<T> {
    val channel = Channel<T>(capacity = Channel.CONFLATED)
    val flow = channel.receiveAsFlow().debounce(timeoutMillis)
    launch {
        flow.collect {
            block(it)
        }
    }

    return channel
}