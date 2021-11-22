package ovh.plrapps.mapcompose.utils

import ovh.plrapps.mapcompose.api.BoundingBox

internal fun BoundingBox.scaleAxis(xAxisMultiplier: Double): BoundingBox {
    return BoundingBox(xLeft * xAxisMultiplier, yTop, xRight * xAxisMultiplier, yBottom)
}

internal fun BoundingBox.rotate(center: Point, angle: AngleRad): BoundingBox {
    val topLeft = Point(xLeft, yTop)
    val topRight = Point(xRight, yTop)
    val bottomLeft = Point(xLeft, yBottom)
    val bottomRight = Point(xRight, yBottom)

    val points = listOf(topLeft, topRight, bottomLeft, bottomRight)
    val rotatedPoints = rotateCentered(points, center, angle)

    val left = rotatedPoints.minOf { it.x }
    val top = rotatedPoints.minOf { it.y }
    val right = rotatedPoints.maxOf { it.x }
    val bottom = rotatedPoints.maxOf { it.y }

    return BoundingBox(left, top, right, bottom)
}
