package ovh.plrapps.mapcompose.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.utils.map

fun <T> Flow<T>.throttle(wait: Long) = channelFlow {
    val channel = Channel<T>(capacity = Channel.CONFLATED)
    coroutineScope {
        launch {
            collect {
                channel.send(it)
            }
        }
        launch {
            for (e in channel) {
                send(e)
                delay(wait)
            }
        }
    }
}

fun <T, M> StateFlow<T>.map(
    coroutineScope : CoroutineScope,
    mapper : (value : T) -> M
) : StateFlow<M> = map { mapper(it) }.stateIn(
    coroutineScope,
    SharingStarted.Eagerly,
    mapper(value)
)