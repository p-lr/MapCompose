package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import ovh.plrapps.mapcompose.ui.state.MarkerData
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import ovh.plrapps.mapcompose.utils.rotateCenteredX
import ovh.plrapps.mapcompose.utils.rotateCenteredY
import ovh.plrapps.mapcompose.utils.toRad

@Composable
internal fun MarkerLayout(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier
            .graphicsLayer {
                translationX = -zoomPRState.scrollX
                translationY = -zoomPRState.scrollY
            }
            .background(Color.Transparent)
            .fillMaxSize()
    ) { measurables, constraints ->
        val placeableCst = constraints.copy(minHeight = 0, minWidth = 0)

        layout(constraints.maxWidth, constraints.maxHeight) {
            for (measurable in measurables) {
                val data = measurable.layoutId as? MarkerData ?: continue
                val placeable = measurable.measure(placeableCst)
                data.measuredWidth = placeable.measuredWidth
                data.measuredHeight = placeable.measuredHeight

                val widthOffset =
                    placeable.measuredWidth * data.relativeOffset.x + data.absoluteOffset.x
                val heightOffset =
                    placeable.measuredHeight * data.relativeOffset.y + data.absoluteOffset.y

                if (zoomPRState.rotation == 0f) {
                    placeable.place(
                        x = (data.x * zoomPRState.fullWidth * zoomPRState.scale + widthOffset).toInt(),
                        y = (data.y * zoomPRState.fullHeight * zoomPRState.scale + heightOffset).toInt(),
                        zIndex = data.zIndex
                    )
                } else {
                    with(zoomPRState) {
                        val angleRad = rotation.toRad()
                        val xFullPx = data.x * fullWidth * scale
                        val yFullPx = data.y * fullHeight * scale
                        val centerX = centroidX * fullWidth * scale
                        val centerY = centroidY * fullHeight * scale
                        placeable.place(
                            x = (rotateCenteredX(
                                xFullPx,
                                yFullPx,
                                centerX,
                                centerY,
                                angleRad
                            ) + widthOffset).toInt(),
                            y = (rotateCenteredY(
                                xFullPx,
                                yFullPx,
                                centerX,
                                centerY,
                                angleRad
                            ) + heightOffset).toInt(),
                            zIndex = data.zIndex
                        )
                    }
                }
            }
        }
    }
}