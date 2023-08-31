package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.demo.utils.pxToDp
import ovh.plrapps.mapcompose.demo.viewmodels.CustomDrawVM
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.log10
import kotlin.math.pow
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * This demo shows how to embed custom drawings inside [MapUI].
 */
@Composable
fun CustomDraw(
    modifier: Modifier = Modifier, viewModel: CustomDrawVM = viewModel()
) {
    Box {
        MapUI(modifier, state = viewModel.state) {
            Square(
                modifier = Modifier,
                mapState = viewModel.state,
                position = Offset(
                    viewModel.state.fullSize.width / 2f - 300f,
                    viewModel.state.fullSize.height / 2f - 300f
                ),
                color = Color(0xff5c6bc0),
                isScaling = true
            )
            Square(
                modifier = Modifier,
                mapState = viewModel.state,
                position = Offset(
                    viewModel.state.fullSize.width / 2f,
                    viewModel.state.fullSize.height / 2f
                ),
                color = Color(0xff087f23),
                isScaling = false
            )
            Line(
                modifier = Modifier,
                mapState = viewModel.state,
                color = Color(0xAAF44336),
                p1 = with(viewModel) {
                    Offset(
                        (p1x * state.fullSize.width).toFloat(),
                        (p1y * state.fullSize.height).toFloat()
                    )
                },
                p2 = with(viewModel) {
                    Offset(
                        (p2x * state.fullSize.width).toFloat(),
                        (p2y * state.fullSize.height).toFloat()
                    )
                }
            )
        }
        ScaleIndicator(
            controller = viewModel.scaleIndicatorController,
            lineColor = Color.Black
        )
    }
}

@Composable
fun ScaleIndicator(
    controller: ScaleIndicatorController,
    lineColor: Color
) {
    Box(Modifier.height(50.dp)) {
        Canvas(
            modifier = Modifier
                .alpha(0.8f)
                .padding(5.dp)
                .size(pxToDp(controller.widthPx).dp, 15.dp)
        ) {
            val width = controller.widthPx * controller.widthRatio
            val height = size.height
            drawLine(lineColor, Offset(0f, height / 2), Offset(width, height / 2), 2.dp.toPx())
            drawLine(
                lineColor,
                Offset(0f, 0f),
                Offset(0f, height),
                2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                lineColor,
                Offset(width, 0f),
                Offset(width, height),
                2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Text(
            text = controller.scaleText,
            color = Color.White,
            modifier = Modifier
                .padding(start = 16.dp, top = 20.dp)
                .background(color = Color(0x885D4037), shape = RoundedCornerShape(4.dp))
                .padding(start = 5.dp, end = 5.dp)
        )
    }
}

class ScaleIndicatorController(val widthPx: Int, initScale: Float) {
    var widthRatio by mutableFloatStateOf(0f)
    var scaleText by mutableStateOf("")

    private var snapScale: Float = initScale
    private var snapWidthRatio = 0f

    init {
        snapToNewValue(initScale)
    }

    fun onScaleChanged(scale: Float) {
        val ratio = scale / snapScale
        if (widthRatio * ratio in 0.5f..1f) {
            widthRatio = snapWidthRatio * ratio
        } else {
            snapToNewValue(scale)
        }
    }

    private fun snapToNewValue(scale: Float) {
        val distance = distanceForPx(widthPx, scale)
        val snap = computeSnapValue(distance) ?: return
        snapScale = scale
        widthRatio = snap.toFloat() / distance
        snapWidthRatio = widthRatio
        scaleText = formatDistance(snap)
    }

    /**
     * Computes the distance in meters, given a size in pixels.
     */
    private fun distanceForPx(nPx: Int, scale: Float): Int {
        // TODO: This a simplified calculation
        return (widthPx * 5 / scale).toInt()
    }

    private fun formatDistance(d: Int): String {
        return "$d m"
    }

    /**
     * A snap value is an entire multiple of power of 10, which is lower than [input].
     * The first digit of a snap value is either 1, 2, 3, or 5.
     * For example: 835 -> 500, 480 -> 300, 270 -> 200, 114 -> 100
     * The snap value is always greater than half of [input].
     */
    private fun computeSnapValue(input: Int): Int? {
        if (input <= 1) return null

        // Lowest entire power of 10
        val power = (log10(input.toDouble())).toInt()

        val power10 = 10.0.pow(power)
        val mostSignificantDigit = (input / power10).toInt()

        return when {
            mostSignificantDigit >= 5 -> 5 * power10
            mostSignificantDigit >= 3 -> 3 * power10
            mostSignificantDigit >= 2 -> 2 * power10
            else -> power10
        }.toInt()
    }
}


/**
 * Here, we define a square with various inputs such as [position], [color], and [isScaling].
 * Our custom composable is based on [DefaultCanvas], which is provided by the MapCompose library.
 * Since [DefaultCanvas] moves, scales, and rotates with the map, so does our custom square composable.
 */
@Composable
fun Square(
    modifier: Modifier,
    mapState: MapState,
    position: Offset,
    color: Color,
    isScaling: Boolean
) {
    DefaultCanvas(
        modifier = modifier,
        mapState = mapState
    ) {
        val side = if (isScaling) 300f else 300f / mapState.scale
        drawRect(
            color,
            topLeft = position,
            size = Size(side, side)
        )
    }
}

@Composable
fun Line(
    modifier: Modifier,
    mapState: MapState,
    color: Color,
    p1: Offset,
    p2: Offset
) {
    DefaultCanvas(
        modifier = modifier,
        mapState = mapState
    ) {
        drawLine(color, start = p1, end = p2, strokeWidth = 8f / mapState.scale)
    }
}