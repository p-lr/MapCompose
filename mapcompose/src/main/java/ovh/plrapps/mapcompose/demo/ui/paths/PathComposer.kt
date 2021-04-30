package ovh.plrapps.mapcompose.demo.ui.paths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import ovh.plrapps.mapcompose.demo.ui.state.DrawablePathState
import ovh.plrapps.mapcompose.demo.ui.state.PathState
import ovh.plrapps.mapcompose.demo.ui.state.ZoomPanRotateState

@Composable
internal fun PathComposer(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    pathState: PathState
) {
    for (path in pathState.pathState.values) {
        PathCanvas(modifier, zoomPRState, path)
    }
}

@Composable
internal fun PathCanvas(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    drawablePathState: DrawablePathState
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        withTransform({
            /* Geometric transformations seem to be applied in reversed order of declaration */
            translate(left = -zoomPRState.scrollX, top = -zoomPRState.scrollY)
            rotate(
                degrees = zoomPRState.rotation,
                pivot = Offset(
                    x = zoomPRState.centroidX.toFloat() * zoomPRState.fullWidth * zoomPRState.scale,
                    y = zoomPRState.centroidY.toFloat() * zoomPRState.fullHeight * zoomPRState.scale
                )
            )
            scale(scale = zoomPRState.scale, Offset.Zero)
        }) {
            with(drawablePathState) {
                val paint = paint.apply {
                    strokeWidth = width.value / zoomPRState.scale
                }
                if (visible) {
                    drawIntoCanvas {
                        it.nativeCanvas.drawLines(
                            pathData.data,
                            offset,
                            // Safety
                            if (offset + count > pathData.data.size) {
                                pathData.data.size - offset
                            } else count,
                            paint
                        )
                    }
                }
            }
        }
    }
}

class PathData internal constructor(
    internal val data: FloatArray
)

class PathDataBuilder internal constructor(
    private val fullWidth: Int,
    private val fullHeight: Int
) {
    private val points = mutableListOf<Offset>()

    /**
     * Add a point to the path. Values are relative coordinates (in range [0f..1f]).
     */
    fun addPoint(x: Double, y: Double) {
        points.add(
            Offset((x * fullWidth).toFloat(), (y * fullHeight).toFloat())
        )
    }

    fun build(): PathData? {
        /* If there is only one point, the path has no sense */
        if (points.size < 2) return null

        val size = points.size * 4 - 4
        val lines = FloatArray(size)

        var i = 0
        var init = true
        for (point in points) {
            if (init) {
                lines[i] = point.x
                lines[i + 1] = point.y
                init = false
                i += 2
            } else {
                lines[i] = point.x
                lines[i + 1] = point.y
                if (i + 2 >= size) break
                lines[i + 2] = lines[i]
                lines[i + 3] = lines[i + 1]
                i += 4
            }
        }

        return PathData(lines)
    }
}

