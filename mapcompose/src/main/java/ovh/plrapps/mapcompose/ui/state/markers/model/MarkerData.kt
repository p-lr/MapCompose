package ovh.plrapps.mapcompose.ui.state.markers.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.ui.state.markers.DragInterceptor
import java.util.*

internal class MarkerData(
    val id: String,
    x: Double, y: Double,
    val relativeOffset: Offset,
    val absoluteOffset: Offset,
    zIndex: Float,
    clickable: Boolean,
    isConstrainedInBounds: Boolean,
    clickableAreaScale: Offset,
    clickableAreaCenterOffset: Offset,
    val renderingStrategy: RenderingStrategy,
    val type: MarkerType,
    val c: @Composable () -> Unit
) {
    var x: Double by mutableDoubleStateOf(x)
    var y: Double by mutableDoubleStateOf(y)
    var isDraggable by mutableStateOf(false)
    var dragInterceptor: DragInterceptor? by mutableStateOf(null)
    var isClickable: Boolean by mutableStateOf(clickable)
    var clickableAreaScale by mutableStateOf(clickableAreaScale)
    var clickableAreaCenterOffset by mutableStateOf(clickableAreaCenterOffset)
    var zIndex: Float by mutableFloatStateOf(zIndex)
    var isConstrainedInBounds by mutableStateOf(isConstrainedInBounds)

    var measuredWidth = 0
    var measuredHeight = 0
    var xPlacement: Int? = null
    var yPlacement: Int? = null
    val uuid: UUID = UUID.randomUUID()

    fun contains(x: Int, y: Int): Boolean {
        val (centerX, centerY) = getCenter() ?: return false

        val deltaX = measuredWidth * clickableAreaScale.x / 2
        val deltaY = measuredHeight * clickableAreaScale.y / 2

        return (x >= centerX - deltaX && x <= centerX + deltaX
                && y >= centerY - deltaY && y <= centerY + deltaY)
    }

    fun getCenter(): Offset? {
        val xPos = xPlacement ?: return null
        val yPos = yPlacement ?: return null

        val centerX = xPos + measuredWidth / 2 + measuredWidth * clickableAreaCenterOffset.x
        val centerY = yPos + measuredHeight / 2 + measuredHeight * clickableAreaCenterOffset.y
        return Offset(centerX, centerY)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MarkerData

        return (id == other.id && x == other.x && y == other.y && uuid == other.uuid)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}