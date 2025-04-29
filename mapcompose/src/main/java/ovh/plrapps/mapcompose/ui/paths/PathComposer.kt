package ovh.plrapps.mapcompose.ui.paths

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.ui.layout.grid
import ovh.plrapps.mapcompose.ui.paths.model.Cap
import ovh.plrapps.mapcompose.ui.paths.model.PatternItem
import ovh.plrapps.mapcompose.ui.state.DrawablePathState
import ovh.plrapps.mapcompose.ui.state.PathState
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import ovh.plrapps.mapcompose.utils.Point
import kotlin.math.abs
import kotlin.math.ceil

@Composable
internal fun PathComposer(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    pathState: PathState
) {
    var drawOrder = 0
    for (path in pathState.pathState.values.sortedBy { it.zIndex }) {
        key(path.id) {
            path.drawOrder.update { drawOrder++ }
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

    /* Scroll values may not be represented accurately using floats (a float has 7 significant
     * decimal digits, so any number above ~10M isn't represented accurately).
     * Since the translate function of the Canvas works with floats, we perform a change of
     * referential so that we only need to translate the canvas by an amount which can be
     * precisely represented as a float.
     * For paths, we also need to be mindful not to change the referential too often. */
    val origin by produceState(
        initialValue = IntOffset.Zero,
        key1 = zoomPRState.scale,
        key2 = zoomPRState.scrollX,
        key3 = zoomPRState.scrollY
    ) {
        val scale = zoomPRState.scale

        val formerX0 = value.x
        val formerY0 = value.y
        val x0 = ((ceil(zoomPRState.scrollX / grid) * grid) / scale).toInt()
        val y0 = ((ceil(zoomPRState.scrollY / grid) * grid) / scale).toInt()

        val shouldUpdate = (abs(x0 - formerX0) * scale > grid) ||
                (abs(y0 - formerY0) * scale > grid)

        if (shouldUpdate) {
            value = IntOffset(x0, y0)
        }
    }

    /* When epsilon changes, a new path is generated. */
    val epsilon by remember {
        derivedStateOf {
            val scale = zoomPRState.scale
            val simplify = drawablePathState.simplify
            if (simplify == 0f) {
                0.0
            } else {
                simplify / scale
            }
        }
    }

    val pathWithOrigin by produceState<PathWithOrigin?>(
        /* Only affects the very first value.
         * During the computation of a new value, the state holds the last computed value. */
        initialValue = null,
        keys = arrayOf(
            pathData,
            offsetAndCount,
            epsilon,
            origin,
            drawablePathState.simplify
        )
    ) {
        val x0 = origin.x
        val y0 = origin.y
        val ep = epsilon
        value = withContext(Dispatchers.Default) {
            generatePath(
                pathData = pathData,
                offset = offsetAndCount.x,
                count = offsetAndCount.y,
                epsilon = ep,
                x0 = x0,
                y0 = y0,
                onNewDecimatedPath = { drawablePathState.currentDecimatedPath.value = it }
            )
        }
    }

    val path = pathWithOrigin ?: return

    val widthPx = with(LocalDensity.current) {
        drawablePathState.width.toPx()
    }

    val dashPathEffect = remember(drawablePathState.pattern, widthPx, zoomPRState.scale) {
        drawablePathState.pattern?.let {
            makePathEffect(it, strokeWidthPx = widthPx, scale = zoomPRState.scale.toFloat())
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
            strokeWidth = (widthPx / zoomPRState.scale).toFloat()
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
            rotate(
                degrees = zoomPRState.rotation,
                pivot = Offset(
                    x = (zoomPRState.pivotX).toFloat(),
                    y = (zoomPRState.pivotY).toFloat()
                )
            )
            translate(
                left = (-zoomPRState.scrollX + path.origin.x * zoomPRState.scale).toFloat(),
                top = (-zoomPRState.scrollY + path.origin.y * zoomPRState.scale).toFloat()
            )
            scale(scale = zoomPRState.scale.toFloat(), Offset.Zero)
        }) {
            with(drawablePathState) {
                if (visible) {
                    drawIntoCanvas {
                        if (drawablePathState.fillColor != null) {
                            it.nativeCanvas.drawPath(path.path, fillPaint)
                        }
                        it.nativeCanvas.drawPath(path.path, paint)
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
    internal val data: List<Point>,
    internal val boundingBox: Pair<Point, Point>     // topLeft, bottomRight
) {
    val size: Int
        get() = data.size
}

@Suppress("unused")
class PathDataBuilder internal constructor(
    private val fullWidth: Int,
    private val fullHeight: Int
) {
    private val points = mutableListOf<Point>()
    private var xMin: Double? = null
    private var xMax: Double? = null
    private var yMin: Double? = null
    private var yMax: Double? = null

    /**
     * Add a point to the path. Values are relative coordinates (in range [0f..1f]).
     */
    @Synchronized
    fun addPoint(x: Double, y: Double) = apply {
        points.add(createPoint(x, y))
    }

    /**
     * Add points to the path. Values are relative coordinates (in range [0f..1f]).
     */
    @Synchronized
    fun addPoints(points: List<Pair<Double, Double>>) = apply {
        this.points += points.map { (x, y) -> createPoint(x, y) }
    }

    private fun createPoint(x: Double, y: Double): Point {
        return Point(x * fullWidth, y * fullHeight).also {
            updateBoundingBox(it.x, it.y)
        }
    }

    private fun updateBoundingBox(x: Double, y: Double) {
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
            Pair(Point(_xMin, _yMin), Point(_xMax, _yMax))
        } else return null

        /**
         * Make a defensive copy (see PathData doc). We don't want structural modifications to
         * [points] to be visible from the [PathData] instance. */
        return PathData(points.toList(), bb)
    }
}

private fun generatePath(
    pathData: PathData,
    offset: Int,
    count: Int,
    epsilon: Double,
    x0: Int,
    y0: Int,
    onNewDecimatedPath: (decimatedPath: List<Point>) -> Unit
): PathWithOrigin {
    val p = Path()
    val subList = pathData.data.subList(offset, offset + count)
    val toRender = if (epsilon > 0f) {
        runCatching {
            val out = mutableListOf<Point>()
            ramerDouglasPeucker(subList, epsilon, out)
            onNewDecimatedPath(out)
            out
        }.getOrElse {
            subList
        }
    } else subList

    for ((i, point) in toRender.withIndex()) {
        if (i == 0) {
            p.moveTo((point.x - x0).toFloat(), (point.y - y0).toFloat())
        } else {
            p.lineTo((point.x - x0).toFloat(), (point.y - y0).toFloat())
        }
    }
    return PathWithOrigin(p, IntOffset(x0, y0))
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

private data class PathWithOrigin(val path: Path, val origin: IntOffset)

internal class DashPathEffectData(val intervals: FloatArray, val phase: Float)