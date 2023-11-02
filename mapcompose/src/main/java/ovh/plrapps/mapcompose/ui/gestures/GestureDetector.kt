package ovh.plrapps.mapcompose.ui.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

/**
 * A modified version of [detectTransformGestures] from the framework, which adds fling and
 * two-fingers tap support.
 */
internal suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onTouchDown: () -> Unit,
    onTwoFingersTap: (centroid: Offset) -> Unit,
    onFling: (velocity: Velocity) -> Unit,
    onFlingZoom: (centroid: Offset, velocity: Float) -> Unit
) {
    val flingVelocityThreshold = 200.dp.toPx().pow(2)
    val flingVelocityMaxRange = -8000f..8000f

    val flingZoomThreshold = 1f
    val flingZoomMaxVelocity = 2500f

    val twoFingersReleaseTolerance = 150 // in ms

    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        onTouchDown()
        val panVelocityTracker = VelocityTracker()
        val zoomVelocityTracker = VelocityTracker1D(isDataDifferential = false)
        var canceled: Boolean
        var centroidTwoFingers = Offset.Unspecified
        var lastTwoFingersDown = 0L
        var lastTime = 0L
        do {
            val event = awaitPointerEvent()
            canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()
                pan += panChange
                zoom *= zoomChange
                rotation += rotationChange

                if (!pastTouchSlop) {
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val uptime =
                        event.changes.maxByOrNull { it.uptimeMillis }?.uptimeMillis ?: 0L
                    lastTime = uptime
                    panVelocityTracker.addPosition(uptime, pan)

                    /* For the fling velocity, only take into account the centroid size when the
                     * two fingers are down */
                    if (event.changes.size == 2 && event.changes.fastAll { it.pressed }) {
                        val size = event.calculateCentroidSize(useCurrent = true)
                        zoomVelocityTracker.addDataPoint(uptime, size)
                        lastTwoFingersDown = uptime
                    }

                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }

                /* When releasing from two fingers tap, only one of the two pointers is pressed.
                 * Note that this only detects the release of the two fingers. */
                if (event.changes.size == 2
                    && event.changes.fastAny { it.pressed }
                    && event.changes.fastAny { !it.pressed }
                ) {
                    centroidTwoFingers = event.calculateCentroidIgnorePressed()
                    event.changes.forEach { it.consume() }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })

        // If changes where consumed in another gesture, no need to go further.
        if (canceled) {
            return@awaitEachGesture
        }

        // If there where some zooming involved, there might be some zoom fling.
        // Then, no need to go further since we'll next check for two-fingers tap and fling.
        if (zoom != 1f && pastTouchSlop) {
            val velocity = runCatching {
                zoomVelocityTracker.calculateVelocity()
            }.getOrDefault(0f)

            if (abs(velocity) > flingZoomThreshold
                && centroidTwoFingers != Offset.Unspecified
                // Tolerate a slight delay between the release of the first and second finger
                && (lastTime - lastTwoFingersDown) < twoFingersReleaseTolerance
            ) {
                onFlingZoom(centroidTwoFingers, velocity / flingZoomMaxVelocity)
            }

            return@awaitEachGesture
        }

        // In addition to not zooming, if there where no pan or the fingers didn't move enough
        // to trigger a zoom or pan, it might be a two fingers tap.
        if (pan == Offset.Zero || !pastTouchSlop) {
            if (centroidTwoFingers != Offset.Unspecified) {
                onTwoFingersTap(centroidTwoFingers)
            }
        } else {
            // No zoom with pan: it might be a fling
            val velocity = runCatching {
                panVelocityTracker.calculateVelocity()
            }.getOrDefault(Velocity.Zero)
            val velocitySquared = velocity.x.pow(2) + velocity.y.pow(2)
            val velocityCapped = Velocity(
                velocity.x.coerceIn(flingVelocityMaxRange),
                velocity.y.coerceIn(flingVelocityMaxRange)
            )

            if (velocitySquared > flingVelocityThreshold) {
                onFling(velocityCapped)
            }
        }
    }
}

/**
 * Returns the centroid when releasing two fingers. One of the changes isn't pressed while the other
 * one is still pressed.
 */
private fun PointerEvent.calculateCentroidIgnorePressed(): Offset {
    var centroid = Offset.Zero
    var centroidWeight = 0

    changes.fastForEach { change ->
        val position = change.position
        centroid += position
        centroidWeight++
    }
    return if (centroidWeight == 0) {
        Offset.Unspecified
    } else {
        centroid / centroidWeight.toFloat()
    }
}
