package ovh.plrapps.mapcompose.ui.view

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import ovh.plrapps.mapcompose.core.*
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState

@Composable
internal fun TileCanvas(
    modifier: Modifier,
    zoomPRState: ZoomPanRotateState,
    visibleTilesResolver: VisibleTilesResolver,
    tileSize: Int,
    alphaTick: Float,
    colorFilterProvider: ColorFilterProvider?,
    tilesToRender: List<Tile>,
    isFilteringBitmap: () -> Boolean,
) {
    val dest = remember { Rect() }
    val src = remember { Rect() }
    val paint: Paint = remember {
        Paint().apply {
            isAntiAlias = false
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        withTransform({
            /* Geometric transformations seem to be applied in reversed order of declaration */
            rotate(
                degrees = zoomPRState.rotation,
                pivot = Offset(
                    x = zoomPRState.pivotX.toFloat(),
                    y = zoomPRState.pivotY.toFloat()
                )
            )
            translate(left = -zoomPRState.scrollX, top = -zoomPRState.scrollY)
            scale(scale = zoomPRState.scale, Offset.Zero)
        }) {
            paint.isFilterBitmap = isFilteringBitmap()

            for (tile in tilesToRender) {
                val scaleForLevel = visibleTilesResolver.getScaleForLevel(tile.zoom)
                    ?: continue
                val tileScaled = (tileSize / scaleForLevel).toInt()
                val l = tile.col * tileScaled
                val t = tile.row * tileScaled
                val r = l + tileScaled
                val b = t + tileScaled
                dest.set(l, t, r, b)

                val paddingX = (zoomPRState.tilePaddingX * scaleForLevel).toInt()
                val paddingY = (zoomPRState.tilePaddingY * scaleForLevel).toInt()
                val stripLeft = paddingX in tile.col * tileSize .. (tile.col + 1) * tileSize
                val stripRight = (zoomPRState.fullWidth - zoomPRState.tilePaddingX) in tile.col * tileScaled .. (tile.col + 1) * tileScaled
                val stripTop = paddingY in tile.row * tileSize .. (tile.row + 1) * tileSize
                val stripBottom = (zoomPRState.fullHeight - zoomPRState.tilePaddingY) in tile.row * tileScaled .. (tile.row + 1) * tileScaled
                val sub = if (stripLeft || stripRight || stripTop || stripBottom) {
                    val stripX = paddingX % tileSize
                    val stripY = paddingY % tileSize
                    src.set(
                        if (stripLeft) stripX else 0,
                        if (stripTop) stripY else 0,
                        if (stripRight) tileSize - stripX else tileSize,
                        if (stripBottom) tileSize - stripY else tileSize
                    )
                    dest.set(
                        if (stripLeft) (l + stripX * tileScaled.toFloat() / tileSize).toInt() else l,
                        if (stripTop) (t + stripY * tileScaled.toFloat() / tileSize).toInt() else t,
                        if (stripRight) (r - stripX * tileScaled.toFloat() / tileSize).toInt() else r,
                        if (stripBottom) (b - stripY * tileScaled.toFloat() / tileSize).toInt() else b
                    )
                    src
                } else null

                val colorFilter = colorFilterProvider?.getColorFilter(tile.row, tile.col, tile.zoom)

                paint.alpha = (tile.alpha * 255).toInt()
                paint.colorFilter = colorFilter?.asAndroidColorFilter()

                drawIntoCanvas {
                    it.nativeCanvas.drawBitmap(tile.bitmap, sub, dest, paint)
                }

                /* If a tile isn't fully opaque, increase its alpha state by the alpha tick */
                if (tile.alpha < 1f) {
                    tile.alpha = (tile.alpha + alphaTick).coerceAtMost(1f)
                }
            }
        }
    }
}