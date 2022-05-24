package ovh.plrapps.mapcompose.ui.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

private val NoPressGesture: suspend PressGestureScope.(Offset) -> Unit = { }

/**
 * Detects tap, double-tap, and long press gestures and calls [onTap], [onDoubleTap], and
 * [onLongPress], respectively, when detected. [onPress] is called when the press is detected
 * and the [PressGestureScope.tryAwaitRelease] and [PressGestureScope.awaitRelease] can be
 * used to detect when pointers have released or the gesture was canceled.
 * The first pointer down and final pointer up are consumed, and in the
 * case of long press, all changes after the long press is detected are consumed.
 *
 * When [onDoubleTap] is provided, the tap gesture is detected only after
 * the [ViewConfiguration.doubleTapMinTimeMillis] has passed and [onDoubleTap] is called if the
 * second tap is started before [ViewConfiguration.doubleTapTimeoutMillis]. If [onDoubleTap] is not
 * provided, then [onTap] is called when the pointer up has been received.
 *
 * If the first down event was consumed, the entire gesture will be skipped, including
 * [onPress]. If the first down event was not consumed, if any other gesture consumes the down or
 * up events, the pointer moves out of the input area, or the position change is consumed,
 * the gestures are considered canceled. [onDoubleTap], [onLongPress], and [onTap] will not be
 * called after a gesture has been canceled.
 */
suspend fun PointerInputScope.detectTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null,
    shouldConsumeTap: ((Offset) -> Boolean)? = null
) = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectTapGestures)

    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown()
            down.consumeDownChange()
            pressScope.reset()
            if (onPress !== NoPressGesture) launch {
                pressScope.onPress(down.position)
            }
            val longPressTimeout = onLongPress?.let {
                viewConfiguration.longPressTimeoutMillis
            } ?: (Long.MAX_VALUE / 2)
            var upOrCancel: PointerInputChange? = null
            try {
                // wait for first tap up or long press
                upOrCancel = withTimeout(longPressTimeout) {
                    waitForUpOrCancellation()
                }
                if (upOrCancel == null) {
                    pressScope.cancel() // tap-up was canceled
                } else {
                    upOrCancel.consumeDownChange()
                    pressScope.release()
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                onLongPress?.invoke(down.position)
                consumeUntilUp()
                pressScope.release()
            }

            if (upOrCancel != null) {
                // tap was successful.
                if (onDoubleTap == null || (shouldConsumeTap?.invoke(upOrCancel.position) == true)) {
                    onTap?.invoke(upOrCancel.position) // no need to check for double-tap.
                } else {
                    // check for second tap
                    val secondDown = awaitSecondDown(upOrCancel)

                    if (secondDown == null) {
                        onTap?.invoke(upOrCancel.position) // no valid second tap started
                    } else {
                        // Second tap down detected
                        pressScope.reset()
                        if (onPress !== NoPressGesture) {
                            launch { pressScope.onPress(secondDown.position) }
                        }

                        try {
                            // Might have a long second press as the second tap
                            withTimeout(longPressTimeout) {
                                val secondUp = waitForUpOrCancellation()
                                if (secondUp != null) {
                                    secondUp.consumeDownChange()
                                    pressScope.release()
                                    onDoubleTap(secondUp.position)
                                } else {
                                    pressScope.cancel()
                                    onTap?.invoke(upOrCancel.position)
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            // The first tap was valid, but the second tap is a long press.
                            // notify for the first tap
                            onTap?.invoke(upOrCancel.position)

                            // notify for the long press
                            onLongPress?.invoke(secondDown.position)
                            consumeUntilUp()
                            pressScope.release()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consumeAllChanges() }
    } while (event.changes.fastAny { it.pressed })
}

/**
 * Waits for [ViewConfiguration.doubleTapTimeoutMillis] for a second press event. If a
 * second press event is received before the time out, it is returned or `null` is returned
 * if no second press is received.
 */
private suspend fun AwaitPointerEventScope.awaitSecondDown(
    firstUp: PointerInputChange
): PointerInputChange? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    var change: PointerInputChange
    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
    do {
        change = awaitFirstDown()
    } while (change.uptimeMillis < minUptime)
    change
}

/**
 * Shortcut for cases when we only need to get press/click logic, as for cases without long press
 * and double click we don't require channelling or any other complications.
 */
internal suspend fun PointerInputScope.detectTapAndPress(
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null
) {
    val pressScope = PressGestureScopeImpl(this)
    forEachGesture {
        coroutineScope {
            pressScope.reset()
            awaitPointerEventScope {

                val down = awaitFirstDown().also { it.consumeDownChange() }

                if (onPress !== NoPressGesture) {
                    launch { pressScope.onPress(down.position) }
                }

                val up = waitForUpOrCancellation()
                if (up == null) {
                    pressScope.cancel() // tap-up was canceled
                } else {
                    up.consumeDownChange()
                    pressScope.release()
                    onTap?.invoke(up.position)
                }
            }
        }
    }
}

/**
 * [detectTapGestures]'s implementation of [PressGestureScope].
 */
private class PressGestureScopeImpl(
    density: Density
) : PressGestureScope, Density by density {
    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    /**
     * Called when a gesture has been canceled.
     */
    fun cancel() {
        isCanceled = true
        mutex.unlock()
    }

    /**
     * Called when all pointers are up.
     */
    fun release() {
        isReleased = true
        mutex.unlock()
    }

    /**
     * Called when a new gesture has started.
     */
    fun reset() {
        mutex.tryLock() // If tryAwaitRelease wasn't called, this will be unlocked.
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
        }
        return isReleased
    }
}