package ovh.plrapps.mapcompose.ui.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.core.Tile
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    visibleTilesResolver: VisibleTilesResolver,
    tileSize: Int,
    alphaTick: Float,
    colorFilterProvider: ColorFilterProvider?,
    tilesToRender: List<Tile>
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
            for (withOverlap in listOf(true, false)) {
                for (tile in tilesToRender) {
                    val scaleForLevel = visibleTilesResolver.getScaleForLevel(tile.zoom)
                        ?: continue
                    val tileScaled = (tileSize / scaleForLevel).toInt()
                    val l = tile.col * tileScaled
                    val t = tile.row * tileScaled

                    val destOffset = if (withOverlap) {
                        IntOffset(l - 1, t - 1)
                    } else {
                        IntOffset(l, t)
                    }
                    val destSize = if (withOverlap) {
                        IntSize(tileScaled + 2, tileScaled + 2)
                    } else {
                        IntSize(tileScaled, tileScaled)
                    }

                    val colorFilter = colorFilterProvider?.getColorFilter(tile.row, tile.col, tile.zoom)

                    drawImage(
                        tile.bitmap.asImageBitmap(), dstOffset = destOffset, dstSize = destSize,
                        alpha = tile.alpha, colorFilter = colorFilter
                    )

                    /* If a tile isn't fully opaque, increase its alpha state by the alpha tick */
                    if (tile.alpha < 1f) {
                        tile.alpha = (tile.alpha + alphaTick).coerceAtMost(1f)
                    }
                }
            }
        }
    }
}