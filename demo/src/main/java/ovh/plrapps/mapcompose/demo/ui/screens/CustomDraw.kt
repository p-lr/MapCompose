package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import ovh.plrapps.mapcompose.api.CustomCanvas
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.demo.viewmodels.CustomDrawVM
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState


@Composable
fun CustomDraw(
    modifier: Modifier = Modifier, viewModel: CustomDrawVM
) {
    MapUI(modifier, state = viewModel.state) {
        CustomSquare(
            modifier = modifier,
            mapState = viewModel.state,
            position = Offset.Zero,
            color = Color(0xfff44336),
            isScaling = true
        )
        CustomSquare(
            modifier = modifier,
            mapState = viewModel.state,
            position = Offset(
                viewModel.state.fullSize.width / 2f - 300f,
                viewModel.state.fullSize.height / 2f - 300f
            ),
            color = Color(0xff5c6bc0),
            isScaling = true
        )
        CustomSquare(
            modifier = modifier,
            mapState = viewModel.state,
            position = Offset(
                viewModel.state.fullSize.width / 2f,
                viewModel.state.fullSize.height / 2f
            ),
            color = Color(0xff087f23),
            isScaling = false
        )
    }
}

/**
 * Here, we define a custom square with various inputs such as [position], [color], and [isScaling].
 * Our custom composable is based on [CustomCanvas], which is provided by the MapCompose library.
 * Since [CustomCanvas] moves, scales, and rotates with the map, so does our custom square composable.
 */
@Composable
fun CustomSquare(
    modifier: Modifier,
    mapState: MapState,
    position: Offset,
    color: Color,
    isScaling: Boolean
) {
    CustomCanvas(
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