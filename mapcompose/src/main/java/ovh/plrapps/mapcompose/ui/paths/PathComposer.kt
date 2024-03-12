package ovh.plrapps.mapcompose.ui.paths

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.ui.paths.model.Cap
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem
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
                scale = zoomPRState.scale,
            )
        }
        drawablePathState.lastRenderedPath = value
    }

    val widthPx = with(LocalDensity.current) {
        drawablePathState.width.toPx()
    }

    val dashPathEffect = remember(drawablePathState.pattern, widthPx, zoomPRState.scale) {
        drawablePathState.pattern?.let {
            makePathEffect(it, strokeWidthPx = widthPx, scale = zoomPRState.scale)
        }
    }

    val paint = remember(
        dashPathEffect,
        drawablePathState.color,
        drawablePathState.cap,
        widthPx,
        zoomPRState.scale
    ) {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            this.color = drawablePathState.color.toArgb()
            strokeCap = when (drawablePathState.cap) {
                Cap.Butt -> Paint.Cap.BUTT
                Cap.Round -> Paint.Cap.ROUND
                Cap.Square -> Paint.Cap.SQUARE
            }
            pathEffect = dashPathEffect
            strokeWidth = widthPx / zoomPRState.scale
        }
    }

    val fillPaint = remember(
        drawablePathState.fillColor,
    ) {
        Paint().apply {
            style = Paint.Style.FILL
            this.color = drawablePathState.fillColor?.toArgb() ?: Color.Transparent.toArgb()
        }
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
                if (visible) {
                    drawIntoCanvas {
                        if (drawablePathState.fillColor != null) {
                            it.nativeCanvas.drawPath(path, fillPaint)
                        }
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
    internal val boundingBox: Pair<Offset, Offset>?     // topLeft, bottomRight
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
    private var xMin: Float? = null
    private var xMax: Float? = null
    private var yMin: Float? = null
    private var yMax: Float? = null

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

    private fun createOffset(x: Double, y: Double): Offset {
        return Offset((x * fullWidth).toFloat(), (y * fullHeight).toFloat()).also {
            updateBoundingBox(it.x, it.y)
        }
    }

    private fun updateBoundingBox(x: Float, y: Float) {
        xMin = xMin?.coerceAtMost(x) ?: x
        xMax = xMax?.coerceAtLeast(x) ?: x
        yMin = yMin?.coerceAtMost(y) ?: y
        yMax = yMax?.coerceAtLeast(y) ?: y
    }

    @Synchronized
    fun build(): PathData? {
        /* If there is only one point, the path has no sense */
        if (points.size < 2) return null

        val _xMin = xMin
        val _xMax = xMax
        val _yMin = yMin
        val _yMax = yMax

        val bb = if (_xMin != null && _xMax != null && _yMin != null && _yMax != null) {
            Pair(Offset(_xMin, _yMin), Offset(_xMax, _yMax))
        } else null

        /**
         * Make a defensive copy (see PathData doc). We don't want structural modifications to
         * [points] to be visible from the [PathData] instance. */
        return PathData(points.toList(), bb)
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

internal fun makePathEffect(pattern: List<PatternItem>, strokeWidthPx: Float, scale: Float): DashPathEffect? {
    val data = makeIntervals(pattern, strokeWidthPx, scale) ?: return null
    return DashPathEffect(data.intervals, data.phase)
}

internal fun concatGap(pattern: List<PatternItem>): List<PatternItem> {
    return buildList {
        var gap = 0f
        for (item in pattern) {
            if (item is PatternItem.Gap) {
                gap += item.lengthPx
            } else {
                if (gap > 0f) {
                    add(PatternItem.Gap(gap))
                }
                gap = 0f
                add(item)
            }
        }
        if (gap > 0f) {
            add(PatternItem.Gap(gap))
        }
    }
}

internal fun makeIntervals(pattern: List<PatternItem>, strokeWidthPx: Float, scale: Float): DashPathEffectData? {
    if (pattern.isEmpty()) return null

    // First, concat gaps
    val concat = concatGap(pattern)

    var phase = 0f
    val firstItem = concat.firstOrNull() ?: return null
    val trimmed = if (firstItem is PatternItem.Gap) {
        phase = firstItem.lengthPx
        /* If first item is a gap, remember it as phase and move it to then end of the pattern and
         * re-concat since the original last item may also be a gap. */
        concatGap(concat.subList(1, concat.size) + firstItem)
    } else {
        concat
    }

    // If the pattern only contained a gap, ignore the pattern
    if (trimmed.isEmpty()) return null

    fun MutableList<Float>.addOffInterval(prev: PatternItem) {
        if (prev is PatternItem.Gap) {
            add((strokeWidthPx + prev.lengthPx) / scale)
        } else {
            add(strokeWidthPx / scale)
        }
    }

    val intervals: FloatArray = buildList {
        var previousItem: PatternItem? = null
        // At this stage, trimmed starts either with a Dot or a Dash
        for (item in trimmed) {
            val toAdd = when (item) {
                is PatternItem.Dash -> item.lengthPx / scale
                PatternItem.Dot -> 1f
                is PatternItem.Gap -> null
            }

            if (toAdd != null) {
                /* If previous item isn't null, then we're adding a value at an odd index */
                previousItem?.also { prev ->
                    addOffInterval(prev)
                }
                add(toAdd)
            }
            previousItem = item
        }

        previousItem?.also { prev ->
            addOffInterval(prev)
        }
    }.toFloatArray()

    return DashPathEffectData(intervals, phase)
}

internal class DashPathEffectData(val intervals: FloatArray, val phase: Float)