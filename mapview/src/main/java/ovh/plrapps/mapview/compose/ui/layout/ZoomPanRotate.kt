package ovh.plrapps.mapview.compose.ui.layout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import ovh.plrapps.mapview.compose.ui.gestures.detectGestures

@Composable
internal fun ZoomPanRotate(
    modifier: Modifier = Modifier,
    gestureListener: GestureListener,
    layoutSizeChangeListener: LayoutSizeChangeListener,
    paddingX: Int,
    paddingY: Int,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    Layout(
        content = content,
        modifier
            .pointerInput(Unit) {
                detectGestures(
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        gestureListener.onRotationDelta(gestureRotate)
                        gestureListener.onScaleRatio(gestureZoom, centroid)
                        gestureListener.onScrollDelta(pan)
                    },
                    onTouchDown = gestureListener::onTap,
                    onFling = { velocity -> gestureListener.onFling(velocity) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset -> gestureListener.onDoubleTap(offset) }
                )
            }
            .onSizeChanged {
                layoutSizeChangeListener.onSizeChanged(scope, it)
            }
            .fillMaxSize(),
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            // Measure each children
            measurable.measure(constraints.copy(minHeight = 0))
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Place children in the parent layout
            placeables.forEach { placeable ->
                placeable.place(x = paddingX, y = paddingY)
            }
        }
    }
}

internal interface GestureListener {
    fun onScaleRatio(scaleRatio: Float, centroid: Offset)
    fun onRotationDelta(rotationDelta: Float)
    fun onScrollDelta(scrollDelta: Offset)
    fun onFling(velocity: Velocity)
    fun onTap()
    fun onDoubleTap(offSet: Offset)
}

internal interface LayoutSizeChangeListener {
    fun onSizeChanged(composableScope: CoroutineScope, size: IntSize)
}
