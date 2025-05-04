package ovh.plrapps.mapcompose.ui.markers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import ovh.plrapps.mapcompose.ui.layout.grid
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import ovh.plrapps.mapcompose.ui.state.markers.model.MarkerData
import ovh.plrapps.mapcompose.utils.rotateCenteredX
import ovh.plrapps.mapcompose.utils.rotateCenteredY
import ovh.plrapps.mapcompose.utils.toRad
import kotlin.math.ceil

@Composable
internal fun MarkerLayout(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    content: @Composable () -> Unit
) {
    /* Scroll values may not be represented accurately using floats (a float has 7 significant
     * decimal digits, so any number above ~10M isn't represented accurately).
     * Since the translate function of the Canvas works with floats, we perform a change of
     * referential so that we only need to translate the canvas by an amount which can be
     * precisely represented as a float. */
    val origin by remember {
        derivedStateOf {
            IntOffset(
                ((ceil(zoomPRState.scrollX / grid) * grid)).toInt(),
                ((ceil(zoomPRState.scrollY / grid) * grid)).toInt()
            )
        }
    }

    val density = LocalDensity.current
    Layout(
        content = content,
        modifier
            .graphicsLayer {
                translationX = (-zoomPRState.scrollX + origin.x).toFloat()
                translationY = (-zoomPRState.scrollY + origin.y).toFloat()
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
                    placeable.measuredWidth * data.relativeOffset.x + with(density) { data.absoluteOffset.x.toPx() }
                val heightOffset =
                    placeable.measuredHeight * data.relativeOffset.y + with(density) { data.absoluteOffset.y.toPx() }

                if (zoomPRState.rotation == 0f) {
                    val x = data.x * zoomPRState.fullWidth * zoomPRState.scale + widthOffset
                    val y = data.y * zoomPRState.fullHeight * zoomPRState.scale + heightOffset
                    data.xPlacement = x
                    data.yPlacement = y

                    placeable.place((x - origin.x).toInt(), (y - origin.y).toInt(), zIndex = data.zIndex)
                } else {
                    with(zoomPRState) {
                        val angleRad = rotation.toRad()
                        val xFullPx = data.x * fullWidth * scale
                        val yFullPx = data.y * fullHeight * scale
                        val centerX = centroidX * fullWidth * scale
                        val centerY = centroidY * fullHeight * scale

                        val x = rotateCenteredX(
                            xFullPx,
                            yFullPx,
                            centerX,
                            centerY,
                            angleRad
                        ) + widthOffset

                        val y = rotateCenteredY(
                            xFullPx,
                            yFullPx,
                            centerX,
                            centerY,
                            angleRad
                        ) + heightOffset

                        data.xPlacement = x
                        data.yPlacement = y
                        placeable.place(
                            (x - origin.x).toInt(),
                            (y - origin.y).toInt(),
                            zIndex = data.zIndex
                        )
                    }
                }
            }
        }
    }
}