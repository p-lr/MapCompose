package ovh.plrapps.mapcompose.api

import androidx.compose.ui.unit.IntOffset
import ovh.plrapps.mapcompose.ui.state.VisibleAreaPadding
import ovh.plrapps.mapcompose.utils.AngleDegree
import ovh.plrapps.mapcompose.utils.rotateX
import ovh.plrapps.mapcompose.utils.rotateY
import ovh.plrapps.mapcompose.utils.toRad

/**
 * When scrolling to a given position, the viewport needs to be offset by taking into account the
 * [VisibleAreaPadding]. This is needed for apis when scrolling is involved.
 */
internal fun VisibleAreaPadding.getOffsetForScroll(rotation: AngleDegree): IntOffset {
    val angle = -rotation.toRad()
    val offsetX = rotateX((left - right) / 2.0, (top - bottom) / 2.0, angle)
    val offsetY = rotateY((left - right) / 2.0, (top - bottom) / 2.0, angle)
    return IntOffset(offsetX.toInt(), offsetY.toInt())
}

/**
 * Get the sum of margins when rotated. This is useful when we need to calculate the remaining space
 * available on screen after applying the visible area padding.
 */
internal fun VisibleAreaPadding.getRotatedMargin(rotation: AngleDegree): IntOffset {
    val angle = -rotation.toRad()
    val paddingX = rotateX((left + right).toDouble(), (top + bottom).toDouble(), angle)
    val paddingY = rotateY((left + right).toDouble(), (top + bottom).toDouble(), angle)
    return IntOffset(paddingX.toInt(), paddingY.toInt())
}
