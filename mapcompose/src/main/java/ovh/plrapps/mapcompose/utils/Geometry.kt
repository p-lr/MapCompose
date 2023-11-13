package ovh.plrapps.mapcompose.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

internal fun getDistance(x: Float, y: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val a = x - x1
    val b = y - y1
    val c = x2 - x1
    val d = y2 - y1

    val lenSq = c * c + d * d
    val param = if (lenSq != 0f) {
        val dot = a * c + b * d
        dot / lenSq
    } else {
        -1f
    }

    val (xx, yy) = when {
        param < 0 -> x1 to y1
        param > 1 -> x2 to y2
        else -> x1 + param * c to y1 + param * d
    }

    val dx = x - xx
    val dy = y - yy
    return hypot(dx, dy)
}

internal fun getNearestPoint(
    x: Float,
    y: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float
): Offset {
    val a = x - x1
    val b = y - y1
    val c = x2 - x1
    val d = y2 - y1

    val lenSq = c * c + d * d
    val param = if (lenSq != 0f) {
        val dot = a * c + b * d
        dot / lenSq
    } else {
        -1f
    }

    val (xx, yy) = when {
        param < 0 -> x1 to y1
        param > 1 -> x2 to y2
        else -> x1 + param * c to y1 + param * d
    }

    return Offset(xx, yy)
}

internal fun isInsideBox(x: Float, y: Float, xMin: Float, xMax: Float, yMin: Float, yMax: Float): Boolean {
    return x in xMin..xMax && y in yMin..yMax
}

internal fun getDistanceFromBox(x: Float, y: Float, xMin: Float, xMax: Float, yMin: Float, yMax: Float): Float {
    return when {
        x < xMin -> getDistance(x, y, xMin, yMin, xMin, yMax)
        x > xMax -> getDistance(x, y, xMax, yMin, xMax, yMax)
        y < yMin -> getDistance(x, y, xMin, yMin, xMax, yMin)
        y > yMax -> getDistance(x, y, xMin, yMax, xMax, yMax)
        else -> 0f // inside
    }
}