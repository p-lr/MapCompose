package ovh.plrapps.mapcompose.ui.layout

sealed class MinimumScaleMode

/**
 * Limit the minimum scale to no less than what would be required to fit inside the container.
 * This is the default mode.
 */
object Fit : MinimumScaleMode()

/**
 * Limit the minimum scale to no less than what would be required to fill the container.
 */
object Fill : MinimumScaleMode()

/**
 * Force a specific minimum scale.
 */
data class Forced(val scale: Float) : MinimumScaleMode()