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

fun rotateX(x: Double, y: Double, angleRad: AngleRad): Double {
    return x * cos(angleRad) - y * sin(angleRad)
}

fun rotateY(x: Double, y: Double, angleRad: AngleRad): Double {
    return x * sin(angleRad) + y * cos(angleRad)
}

fun rotateCentered(points: List<Point>, center: Point, angleRad: AngleRad): List<Point> {
    return points.map { rotateCentered(it, center, angleRad) }
}

fun rotateCentered(point: Point, center: Point, angleRad: AngleRad): Point {
    return Point(rotateCenteredX(point, center, angleRad), rotateCenteredY(point, center, angleRad))
}

fun rotateCenteredX(point: Point, center: Point, angleRad: AngleRad): Double {
    return rotateCenteredX(point.x, point.y, center.x, center.y, angleRad)
}

fun rotateCenteredY(point: Point, center: Point, angleRad: AngleRad): Double {
    return rotateCenteredY(point.x, point.y, center.x, center.y, angleRad)
}

fun rotateCenteredX(x: Double, y: Double, centerX: Double, centerY: Double, angleRad: AngleRad): Double {
    return centerX + (x - centerX) * cos(angleRad) - (y - centerY) * sin(angleRad)
}

fun rotateCenteredY(x: Double, y: Double, centerX: Double, centerY: Double, angleRad: AngleRad): Double {
    return centerY + (x - centerX) * sin(angleRad) + (y - centerY) * cos(angleRad)
}
