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
import ovh.plrapps.mapcompose.core.ColorFilterProvider
import ovh.plrapps.mapcompose.core.Tile
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.ui.state.ZoomPanRotateState
import kotlin.math.ceil

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
    val paint: Paint = remember {
        Paint().apply {
            isAntiAlias = false
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        /* Scroll values may not be represented accurately using floats (a float has 7 significant
         * decimal digits, so any number above ~10M isn't represented accurately).
         * Since the translate function of the Canvas works with floats, we perform a change of
         * referential so that we only need to translate the canvas by an amount which can be
         * precisely represented as a float. */
        val x0 = ((ceil(zoomPRState.scrollX / grid) * grid) / zoomPRState.scale).toInt()
        val y0 = ((ceil(zoomPRState.scrollY / grid) * grid) / zoomPRState.scale).toInt()

        withTransform({
            /* Geometric transformations seem to be applied in reversed order of declaration */
            rotate(
                degrees = zoomPRState.rotation,
                pivot = Offset(
                    x = zoomPRState.pivotX.toFloat(),
                    y = zoomPRState.pivotY.toFloat()
                )
            )
            translate(
                left = (-zoomPRState.scrollX + x0 * zoomPRState.scale).toFloat(),
                top = (-zoomPRState.scrollY + y0 * zoomPRState.scale).toFloat()
            )
            scale(scale = zoomPRState.scale.toFloat(), Offset.Zero)
        }) {
            paint.isFilterBitmap = isFilteringBitmap()

            for (tile in tilesToRender) {
                if (tile.markedForSweep) continue
                val bitmap = tile.bitmap ?: continue
                val scaleForLevel = visibleTilesResolver.getScaleForLevel(tile.zoom)
                    ?: continue
                val tileScaled = (tileSize / scaleForLevel).toInt()
                val l = tile.col * tileScaled
                val t = tile.row * tileScaled
                val r = l + tileScaled
                val b = t + tileScaled
                /* The change of referential is done by offsetting coordinates by (x0, y0) */
                dest.set(l - x0, t - y0, r - x0, b - y0)

                val colorFilter = colorFilterProvider?.getColorFilter(tile.row, tile.col, tile.zoom)

                paint.alpha = (tile.alpha * 255).toInt()
                paint.colorFilter = colorFilter?.asAndroidColorFilter()

                drawIntoCanvas {
                    it.nativeCanvas.drawBitmap(bitmap, null, dest, paint)
                }

                /* If a tile isn't fully opaque, increase its alpha state by the alpha tick */
                if (tile.alpha < 1f) {
                    tile.alpha = (tile.alpha + alphaTick).coerceAtMost(1f)
                } else {
                    tile.overlaps?.markedForSweep = true
                    tile.overlaps = null
                }
            }
        }
    }
}

/* We assume no device has a screen wider than this */
private const val grid = 65536