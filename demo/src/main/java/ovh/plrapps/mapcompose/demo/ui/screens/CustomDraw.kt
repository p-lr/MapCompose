package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.demo.viewmodels.CustomDrawVM
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState


/**
 * This demo shows how to embed custom drawings inside [MapUI].
 */
@Composable
fun CustomDraw(
    modifier: Modifier = Modifier, viewModel: CustomDrawVM
) {
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