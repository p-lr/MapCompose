package ovh.plrapps.mapcompose.demo.ui.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

suspend fun PointerInputScope.detectGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onTouchDown: () -> Unit,
    onFling: (velocity: Velocity) -> Unit
) {
    val flingVelocityThreshold = 200.dp.toPx().pow(2)
    val flingVelocityMaxRange = -8000f..8000f

    forEachGesture {
        awaitPointerEventScope {
            var rotation = 0f
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var lockedToPanZoom = false

            awaitFirstDown(requireUnconsumed = false)
            onTouchDown()
            val velocityTracker = VelocityTracker()
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.positionChangeConsumed() }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    var uptime = 0L
                    val panChange = event.calculatePan { uptime_ ->
                        uptime = uptime_
                    }
                    pan += panChange

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange

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
                        velocityTracker.addPosition(
                            uptime,
                            pan
                        )

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
                                it.consumeAllChanges()
                            }
                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })

            val velocity = velocityTracker.calculateVelocity()
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

fun PointerEvent.calculatePan(uptimeConsumer: (Long) -> Unit): Offset {
    val currentCentroid = calculateCurrentCentroid(uptimeConsumer)
    if (currentCentroid == Offset.Unspecified) {
        return Offset.Zero
    }
    val previousCentroid = calculateCentroid(useCurrent = false)
    return currentCentroid - previousCentroid
}

fun PointerEvent.calculateCurrentCentroid(
    uptimeConsumer: (Long) -> Unit
): Offset {
    var centroid = Offset.Zero
    var centroidWeight = 0
    var mostRecentUptime = 0L

    changes.fastForEach { change ->
        if (change.uptimeMillis > mostRecentUptime) {
            mostRecentUptime = change.uptimeMillis
        }
        if (change.pressed && change.previousPressed) {
            val position = change.position
            centroid += position
            centroidWeight++
        }
    }
    uptimeConsumer(mostRecentUptime)
    return if (centroidWeight == 0) {
        Offset.Unspecified
    } else {
        centroid / centroidWeight.toFloat()
    }
}
