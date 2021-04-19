package ovh.plrapps.mapcompose.core

import ovh.plrapps.mapcompose.utils.AngleRad


/**
 * Denotes an area on the screen. Values are in pixels.
 */
data class Viewport(
    var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0,
    var angleRad: AngleRad = 0f
)