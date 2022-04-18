package ovh.plrapps.mapcompose.utils

import ovh.plrapps.mapcompose.api.VisibleArea
import kotlin.math.abs

fun VisibleArea.contains(x: Double, y: Double): Boolean {
    val fullArea =
        triangleArea(p1x, p1y, p2x, p2y, p3x, p3y) + triangleArea(p1x, p1y, p4x, p4y, p3x, p3y)
    val t1 = triangleArea(x, y, p1x, p1y, p2x, p2y)
    val t2 = triangleArea(x, y, p2x, p2y, p3x, p3y)
    val t3 = triangleArea(x, y, p3x, p3y, p4x, p4y)
    val t4 = triangleArea(x, y, p1x, p1y, p4x, p4y)

    return abs(fullArea - (t1 + t2 + t3 + t4)) < 1E-8
}

private fun triangleArea(
    x1: Double,
    y1: Double,
    x2: Double,
    y2: Double,
    x3: Double,
    y3: Double
): Double {
    return abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0)
}