package ovh.plrapps.mapcompose.utils

import androidx.compose.ui.geometry.Offset

@JvmInline
value class Point(val offset: Offset) {
    val x get() = offset.x
    val y get() = offset.y

    constructor(x: Float, y: Float) : this(Offset(x, y))
    constructor(x: Double, y: Double) : this(Offset(x.toFloat(), y.toFloat()))
    constructor(x: Int, y: Int) : this(Offset(x.toFloat(), y.toFloat()))

    fun scaleX(multiplier: Float) = Point(x * multiplier, y)
    fun coerceIn(min: Float, max: Float) = Point(x.coerceIn(min, max), y.coerceIn(min, max))
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun div(other: Point) = Point(x / other.x, y / other.y)
    operator fun times(other: Point) = Point(x * other.x, y * other.y)
    operator fun times(other: Float) = Point(x * other, y * other)
}
