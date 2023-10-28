@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.*

/**
 * Given a [point] with known normalized coordinates, rotate it by [angleDegree] around the current
 * centroid.
 */
suspend fun MapState.rotatePoint(point: Point, angleDegree: AngleDegree): Point {
    return with(zoomPanRotateState) {
        awaitLayout()

        val xAxisScale = fullHeight / fullWidth.toDouble()
        val scaledCenterX = centroidX / xAxisScale

        val xR = rotateCenteredX(
            point.x / xAxisScale, point.y, scaledCenterX, centroidY, angleDegree.toRad()
        ) * xAxisScale
        val yR = rotateCenteredY(
            point.x / xAxisScale, point.y, scaledCenterX, centroidY, angleDegree.toRad()
        )

        Point(xR, yR)
    }
}