package ovh.plrapps.mapcompose.ui.layout

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import ovh.plrapps.mapcompose.ui.gestures.detectGestures
import ovh.plrapps.mapcompose.ui.gestures.detectTapGestures

@Composable
internal fun ZoomPanRotate(
    modifier: Modifier = Modifier,
    gestureListener: GestureListener,
    layoutSizeChangeListener: LayoutSizeChangeListener,
    padding: IntOffset,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    Layout(
        content = content,
        modifier
            .pointerInput(gestureListener.isListeningForGestures()) {
                if (!gestureListener.isListeningForGestures()) return@pointerInput
                detectGestures(
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        gestureListener.onRotationDelta(gestureRotate)
                        gestureListener.onScaleRatio(gestureZoom, centroid)
                        gestureListener.onScrollDelta(pan)
                    },
                    onTouchDown = gestureListener::onTouchDown,
                    onFling = { velocity -> gestureListener.onFling(velocity) }
                )
            }
            .pointerInput(gestureListener.isListeningForGestures()) {
                if (!gestureListener.isListeningForGestures()) return@pointerInput
                detectTapGestures(
                    onTap = { offset -> gestureListener.onTap(offset) },
                    onDoubleTap = { offset -> gestureListener.onDoubleTap(offset) },
                    onPress = { gestureListener.onPress() },
                    onLongPress = { offset -> gestureListener.onLongPress(offset) },
                    shouldConsumeTap = { offset -> gestureListener.shouldConsumeTapGesture(offset) }
                )
            }
            .onSizeChanged {
                layoutSizeChangeListener.onSizeChanged(scope, it)
            }
            .fillMaxSize(),
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            // Measure each children
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Place children in the parent layout
            placeables.forEach { placeable ->
                placeable.place(x = padding.x, y = padding.y)
            }
        }
    }
}

internal interface GestureListener {
    fun onScaleRatio(scaleRatio: Float, centroid: Offset)
    fun onRotationDelta(rotationDelta: Float)
    fun onScrollDelta(scrollDelta: Offset)
    fun onFling(velocity: Velocity)
    fun onTouchDown()
    fun onPress()
    fun onTap(focalPt: Offset)
    fun onDoubleTap(focalPt: Offset)
    fun onLongPress(focalPt: Offset)
    fun isListeningForGestures(): Boolean
    fun shouldConsumeTapGesture(focalPt: Offset): Boolean
}

internal interface LayoutSizeChangeListener {
    fun onSizeChanged(composableScope: CoroutineScope, size: IntSize)
}
