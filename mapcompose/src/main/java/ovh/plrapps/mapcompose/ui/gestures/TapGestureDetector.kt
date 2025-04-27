package ovh.plrapps.mapcompose.ui.gestures

import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.math.abs

private val NoPressGesture: suspend PressGestureScope.(Offset) -> Unit = { }

/**
 * A modified version of [detectTapGestures] from the framework, with the following differences:
 * - can take [shouldConsumeTap] callback which is invoked to check whether a tap should be consumed.
 * - can take [shouldConsumeLongPress] callback which is invoked to check whether a long-press should
 * be consumed.
 * When [shouldConsumeTap] returns true, [onTap] isn't invoked and the gesture ends there without
 * waiting for [ViewConfiguration.doubleTapMinTimeMillis].
 * When a long-press gesture is detected, [shouldConsumeLongPress] is invoked, and [onLongPress] is
 * invoked only when the long-press isn't consumed.
 * - takes a [onDoubleTapZoom] callback for one finger zooming by double tapping but not releasing
 * on the second tap, and then sliding the finger up to zoom out, or down to zoom in.
 * Consequently, this gesture detector doesn't try to detect a long-press after the
 * second tap, and a double-tap can no-longer timeout.
 */
internal suspend fun PointerInputScope.detectTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onDoubleTapZoom: (centroid: Offset, zoom: Float) -> Unit,
    onDoubleTapZoomFling: (centroid: Offset, velocity: Float) -> Unit,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null,
    shouldConsumeTap: ((Offset) -> Boolean)? = null,
    shouldConsumeLongPress: ((Offset) -> Boolean) ? = null
) = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectTapGestures)

    val flingZoomThreshold = 1f
    val flingZoomVelocityFactor = 400  // lower value for faster fling

    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        launch {
            pressScope.reset()
        }
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
                launch {
                    pressScope.cancel() // tap-up was canceled
                }
            } else {
                upOrCancel.consume()
                launch {
                    pressScope.release()
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            val longPressConsumed = shouldConsumeLongPress?.invoke(down.position) ?: false
            if (!longPressConsumed) {
                onLongPress?.invoke(down.position)
            }
            consumeUntilUp()
            pressScope.release()
        }

        if (upOrCancel != null) {
            // tap was successful.
            val tapConsumed = shouldConsumeTap?.invoke(upOrCancel.position) ?: false
            if (tapConsumed) return@awaitEachGesture

            if (onDoubleTap == null) {
                onTap?.invoke(upOrCancel.position) // no need to check for double-tap.
            } else {
                // check for second tap
                val secondDown = awaitSecondDown(upOrCancel)

                if (secondDown == null) { // no valid second tap started
                    onTap?.invoke(upOrCancel.position)
                } else {
                    // Second tap down detected
                    launch {
                        pressScope.reset()
                    }
                    if (onPress !== NoPressGesture) {
                        launch { pressScope.onPress(secondDown.position) }
                    }

                    // Now, either double-tap or zoom gesture. This is where we deviate
                    // from the framework : no timeout to detect long-press.
                    val secondUp = waitForUpOrCancellation()
                    if (secondUp != null) {
                        secondUp.consume()
                        launch {
                            pressScope.release()
                        }
                        onDoubleTap(secondUp.position)
                    } else {
                        val zoomVelocityTracker = VelocityTracker()
                        var pan = Offset.Zero
                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.fastAny { it.isConsumed }
                            if (!canceled) {
                                val panChange = event.calculatePan()
                                pan += panChange
                                val zoom = (size.height + panChange.y * density) / size.height
                                val uptime = event.changes.maxByOrNull { it.uptimeMillis }?.uptimeMillis ?: 0L
                                zoomVelocityTracker.addPosition(uptime, pan)
                                onDoubleTapZoom(secondDown.position, zoom)

                                event.changes.fastForEach {
                                    if (it.positionChanged()) {
                                        it.consume()
                                    }
                                }
                            }
                        } while (!canceled && event.changes.fastAny { it.pressed })

                        launch {
                            pressScope.cancel()
                        }

                        /* Depending on the velocity, we might trigger a fling */
                        zoomVelocityTracker.calculateVelocity()
                        val velocity = runCatching {
                            zoomVelocityTracker.calculateVelocity()
                        }.getOrDefault(Velocity.Zero).y

                        if (abs(velocity) > flingZoomThreshold) {
                            onDoubleTapZoomFling(
                                secondDown.position,
                                velocity / flingZoomVelocityFactor
                            )
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
        event.changes.fastForEach { it.consume() }
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
    suspend fun reset() {
        mutex.lock()
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