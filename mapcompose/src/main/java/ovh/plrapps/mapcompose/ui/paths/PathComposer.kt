package ovh.plrapps.mapcompose.ui.paths

import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.ui.state.DrawablePathState
import ovh.plrapps.mapcompose.ui.state.PathState
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState

@Composable
internal fun PathComposer(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    pathState: PathState
) {
    for (path in pathState.pathState.values.sortedBy { it.zIndex }) {
        key(path.id) {
            PathCanvas(modifier, zoomPRState, path)
        }
    }
}

@Composable
internal fun PathCanvas(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    drawablePathState: DrawablePathState
) {
    val offsetAndCount = drawablePathState.offsetAndCount
    val pathData = drawablePathState.pathData

    val path by produceState(
        initialValue = drawablePathState.lastRenderedPath,
        keys = arrayOf(
            pathData,
            offsetAndCount,
            zoomPRState.scale,
            drawablePathState.simplify
        )
    ) {
        value = withContext(Dispatchers.Default) {
            generatePath(
                pathData = pathData,
                offset = offsetAndCount.x,
                count = offsetAndCount.y,
                simplify = drawablePathState.simplify,
                scale = zoomPRState.scale
            )
        }
        drawablePathState.lastRenderedPath = value
    }

    val widthPx = with(LocalDensity.current) {
        drawablePathState.width.toPx()
    }

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
                    strokeWidth = widthPx / zoomPRState.scale
                }
                if (visible) {
                    drawIntoCanvas {
                        it.nativeCanvas.drawPath(path, paint)
                    }
                }
            }
        }
    }
}

/**
 * Once an instance of [PathData] is created, [data] shall not have structural modifications for
 * subList to work (see [List.subList] doc). */
class PathData internal constructor(
    internal val data: List<Offset>,
) {
    val size: Int
        get() = data.size
}

@Suppress("unused")
class PathDataBuilder internal constructor(
    private val fullWidth: Int,
    private val fullHeight: Int
) {
    private val points = mutableListOf<Offset>()

    /**
     * Add a point to the path. Values are relative coordinates (in range [0f..1f]).
     */
    @Synchronized
    fun addPoint(x: Double, y: Double) = apply {
        points.add(createOffset(x, y))
    }

    /**
     * Add points to the path. Values are relative coordinates (in range [0f..1f]).
     */
    @Synchronized
    fun addPoints(points: List<Pair<Double, Double>>) = apply {
        this.points += points.map { (x, y) -> createOffset(x, y) }
    }

    private fun createOffset(x: Double, y: Double) =
        Offset((x * fullWidth).toFloat(), (y * fullHeight).toFloat())

    @Synchronized
    fun build(): PathData? {
        /* If there is only one point, the path has no sense */
        if (points.size < 2) return null

        /**
         * Make a defensive copy (see PathData doc). We don't want structural modifications to
         * [points] to be visible from the [PathData] instance. */
        return PathData(points.toList())
    }
}

internal fun generatePath(pathData: PathData, offset: Int, count: Int, simplify: Float, scale: Float): Path {
    val p = Path()
    val epsilon = simplify / scale
    val subList = pathData.data.subList(offset, offset + count)
    val toRender = if (epsilon > 0f) {
        runCatching {
            val out = mutableListOf<Offset>()
            ramerDouglasPeucker(subList, epsilon, out)
            out
        }.getOrElse {
            subList
        }
    } else subList
    for ((i, point) in toRender.withIndex()) {
        if (i == 0) {
            p.moveTo(point.x, point.y)
        } else {
            p.lineTo(point.x, point.y)
        }
    }
    return p
}

