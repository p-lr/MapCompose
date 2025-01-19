package ovh.plrapps.mapcompose.utils

import ovh.plrapps.mapcompose.api.VisibleArea
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

fun VisibleArea.intersects(other: VisibleArea): Boolean {
    if (
        other.contains(p1x, p1y) ||
        other.contains(p2x, p2y) ||
        other.contains(p3x, p3y) ||
        other.contains(p4x, p4y)
    ) return true

    if (
        contains(other.p1x, other.p1y) ||
        contains(other.p2x, other.p2y) ||
        contains(other.p3x, other.p3y) ||
        contains(other.p4x, other.p4y)
    ) return true

    if (
        segmentsIntersect(p1x, p1y, p3x, p3y, other.p1x, other.p1y, other.p3x, other.p3y)
    ) return true

    return false
}

/**
 * Checks whether the two segments [p1;p2] and [p3;p4] intersect.
 */
private fun segmentsIntersect(
    p1x: Double,
    p1y: Double,
    p2x: Double,
    p2y: Double,
    p3x: Double,
    p3y: Double,
    p4x: Double,
    p4y: Double
): Boolean {
    val o1 = orientation(p1x, p1y, p2x, p2y, p3x, p3y)
    val o2 = orientation(p1x, p1y, p2x, p2y, p4x, p4y)
    val o3 = orientation(p3x, p3y, p4x, p4y, p1x, p1y)
    val o4 = orientation(p3x, p3y, p4x, p4y, p2x, p2y)

    // General case
    if (o1 != o2 && o3 != o4) return true

    // p1, q1 and p2 are collinear and p2 lies on segment [p1;q1]
    if (o1 == 0 && onSegment(p1x, p1y, p3x, p3y, p2x, p2y)) return true

    // p1, q1 and q2 are collinear and q2 lies on segment [p1;q1]
    if (o2 == 0 && onSegment(p1x, p1y, p4x, p4y, p2x, p2y)) return true

    // p2, q2 and p1 are collinear and p1 lies on segment [p2;q2]
    if (o3 == 0 && onSegment(p3x, p3y, p1x, p1y, p4x, p4y)) return true

    // p2, q2 and q1 are collinear and q1 lies on segment [p2;q2]
    if (o4 == 0 && onSegment(p3x, p3y, p2x, p2y, p4x, p4y)) return true

    return false
}

/**
 * Given three collinear points p1, p2, p3, check if point p2 lies on line segment [p1;p2]]
 */
private fun onSegment(
    p1x: Double,
    p1y: Double,
    p2x: Double,
    p2y: Double,
    p3x: Double,
    p3y: Double
): Boolean {
    return p2x <= max(p1x, p3x) && p2x >= min(p1x, p3x) && p2y <= max(p1y, p3y) && p2y >= min(p1y, p3y)
}

/**
 * Get the orientation of ordered triplet (p1, p2, p3).
 * @returns 0 when p1, p2 and p3 are collinear
 *          1 when Clockwise
 *          2 when Counterclockwise
 */
private fun orientation(
    p1x: Double,
    p1y: Double,
    p2x: Double,
    p2y: Double,
    p3x: Double,
    p3y: Double
): Int {
    val p = (p2y - p1y) * (p3x - p2x) - (p2x - p1x) * (p3y - p2y)
    return if (p == 0.0) 0 else if (p > 0) 1 else 2
}