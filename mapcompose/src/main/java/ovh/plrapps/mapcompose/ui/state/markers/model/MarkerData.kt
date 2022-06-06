package ovh.plrapps.mapcompose.ui.state.markers.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import ovh.plrapps.mapcompose.ui.state.markers.DragInterceptor

internal class MarkerData(
    val id: String,
    x: Double, y: Double,
    val relativeOffset: Offset,
    val absoluteOffset: Offset,
    zIndex: Float,
    clickable: Boolean,
    clipShape: Shape?,
    isConstrainedInBounds: Boolean,
    clickableAreaScale: Offset,
    clickableAreaCenterOffset: Offset,
    val renderingStrategy: RenderingStrategy,
    val c: @Composable () -> Unit
) {
    var x: Double by mutableStateOf(x)
    var y: Double by mutableStateOf(y)
    var isDraggable by mutableStateOf(false)
    var dragInterceptor: DragInterceptor? by mutableStateOf(null)
    var isClickable: Boolean by mutableStateOf(clickable)
    var clickableAreaScale by mutableStateOf(clickableAreaScale)
    var clickableAreaCenterOffset by mutableStateOf(clickableAreaCenterOffset)
    var clipShape: Shape? by mutableStateOf(clipShape)
    var zIndex: Float by mutableStateOf(zIndex)
    var isConstrainedInBounds by mutableStateOf(isConstrainedInBounds)

    var measuredWidth = 0
    var measuredHeight = 0
    var xPlacement: Int? = null
    var yPlacement: Int? = null
    var data: Any? = null

    fun contains(x: Int, y: Int): Boolean {
        val xPos = xPlacement ?: return false
        val yPos = yPlacement ?: return false

        val centerX = xPos + measuredWidth / 2 + measuredWidth * clickableAreaCenterOffset.x
        val deltaX = measuredWidth * clickableAreaScale.x / 2
        val centerY = yPos + measuredHeight / 2 + measuredHeight * clickableAreaCenterOffset.y
        val deltaY = measuredHeight * clickableAreaScale.y / 2

        return (x >= centerX - deltaX && x <= centerX + deltaX
                && y >= centerY - deltaY && y <= centerY + deltaY)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MarkerData

        if (id != other.id) return false
        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}