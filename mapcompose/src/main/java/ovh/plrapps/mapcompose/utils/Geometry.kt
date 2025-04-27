package ovh.plrapps.mapcompose.utils

import kotlin.math.hypot

internal fun getDistance(x: Double, y: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
    val a = x - x1
    val b = y - y1
    val c = x2 - x1
    val d = y2 - y1

    val lenSq = c * c + d * d
    val param = if (lenSq != 0.0) {
        val dot = a * c + b * d
        dot / lenSq
    } else {
        -1.0
    }

    val (xx, yy) = when {
        param < 0.0 -> x1 to y1
        param > 1.0 -> x2 to y2
        else -> x1 + param * c to y1 + param * d
    }

    val dx = x - xx
    val dy = y - yy
    return hypot(dx, dy)
}

internal fun getNearestPoint(
    x: Double,
    y: Double,
    x1: Double,
    y1: Double,
    x2: Double,
    y2: Double
): Point {
    val a = x - x1
    val b = y - y1
    val c = x2 - x1
    val d = y2 - y1

    val lenSq = c * c + d * d
    val param = if (lenSq != 0.0) {
        val dot = a * c + b * d
        dot / lenSq
    } else {
        -1.0
    }

    val (xx, yy) = when {
        param < 0.0 -> x1 to y1
        param > 1.0 -> x2 to y2
        else -> x1 + param * c to y1 + param * d
    }

    return Point(xx, yy)
}

internal fun isInsideBox(x: Double, y: Double, xMin: Double, xMax: Double, yMin: Double, yMax: Double): Boolean {
    return x in xMin..xMax && y in yMin..yMax
}

internal fun getDistanceFromBox(x: Double, y: Double, xMin: Double, xMax: Double, yMin: Double, yMax: Double): Double {
    return when {
        x < xMin -> getDistance(x, y, xMin, yMin, xMin, yMax)
        x > xMax -> getDistance(x, y, xMax, yMin, xMax, yMax)
        y < yMin -> getDistance(x, y, xMin, yMin, xMax, yMin)
        y > yMax -> getDistance(x, y, xMin, yMax, xMax, yMax)
        else -> 0.0 // inside
    }
}