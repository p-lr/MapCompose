package ovh.plrapps.mapcompose.utils

import kotlin.math.cos
import kotlin.math.sin

typealias AngleDegree = Float
typealias AngleRad = Float

fun AngleDegree.toRad(): AngleRad = this * 0.017453292519943295f  // this * PI / 180.0

/**
 * Constrain the angle to have values between 0f and 360f.
 */
fun AngleDegree.modulo(): AngleDegree {
    val mod = this % 360f
    return if (mod < 0) {
        mod + 360f
    } else mod
}

fun rotate(point: Point, angleRad: AngleRad): Point {
    val x = point.x * cos(angleRad) - point.y * sin(angleRad)
    val y = point.x * sin(angleRad) + point.y * cos(angleRad)
    return Point(x, y)
}

fun rotateCentered(points: List<Point>, center: Point, angleRad: AngleRad): List<Point> {
    return points.map { rotateCentered(it, center, angleRad) }
}

fun rotateCentered(point: Point, center: Point, angleRad: AngleRad): Point {
    val x = center.x + (point.x - center.x) * cos(angleRad) - (point.y - center.y) * sin(angleRad)
    val y = center.y + (point.x - center.x) * sin(angleRad) + (point.y - center.y) * cos(angleRad)
    return Point(x, y)
}

fun rotateCentered(point: Point, center: Point, angleRad: AngleRad, xAxisScale: Float = 1f): Point {
    return rotateCentered(point.scaleX(1 / xAxisScale), center, angleRad).scaleX(xAxisScale)
}
