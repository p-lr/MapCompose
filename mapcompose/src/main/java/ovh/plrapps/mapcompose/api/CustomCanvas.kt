package ovh.plrapps.mapcompose.api

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import ovh.plrapps.mapcompose.demo.ui.state.MapState

@Composable
fun CustomCanvas(
    modifier: Modifier,
    mapState: MapState,
    drawBlock: DrawScope.() -> Unit
) {
    Canvas(
        modifier = modifier
    ) {
        withTransform({
            /* Geometric transformations seem to be applied in reversed order of declaration */
            translate(left = -mapState.scroll.x, top = -mapState.scroll.y)
            rotate(
                degrees = mapState.rotation,
                pivot = Offset(
                    x = mapState.centroidX.toFloat() * mapState.fullSize.width * mapState.scale,
                    y = mapState.centroidY.toFloat() * mapState.fullSize.height * mapState.scale
                )
            )
            scale(scale = mapState.scale, Offset.Zero)
        }, drawBlock)
    }
}