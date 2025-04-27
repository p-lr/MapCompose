package ovh.plrapps.mapcompose.ui.paths

import ovh.plrapps.mapcompose.utils.Point
import kotlin.math.hypot


internal fun ramerDouglasPeucker(pointList: List<Point>, epsilon: Double, out: MutableList<Point>) {
    if (pointList.size < 2) throw IllegalArgumentException("Not enough points to simplify")

    // Find the point with the maximum distance from line between start and end
    var dmax = 0.0
    var index = 0
    val end = pointList.size - 1
    for (i in 1 until end) {
        val d = perpendicularDistance(pointList[i], pointList[0], pointList[end])
        if (d > dmax) { index = i; dmax = d }
    }

    // If max distance is greater than epsilon, recursively simplify
    if (dmax > epsilon) {
        val recResults1 = mutableListOf<Point>()
        val recResults2 = mutableListOf<Point>()
        val firstLine = pointList.take(index + 1)
        val lastLine  = pointList.drop(index)
        ramerDouglasPeucker(firstLine, epsilon, recResults1)
        ramerDouglasPeucker(lastLine, epsilon, recResults2)

        // build the result list
        out.addAll(recResults1.take(recResults1.size - 1))
        out.addAll(recResults2)
        if (out.size < 2) throw RuntimeException("Problem assembling output")
    }
    else {
        // Just return start and end points
        out.clear()
        out.add(pointList.first())
        out.add(pointList.last())
    }
}

private fun perpendicularDistance(pt: Point, lineStart: Point, lineEnd: Point): Double {
    var dx = lineEnd.x - lineStart.x
    var dy = lineEnd.y - lineStart.y

    // Normalize
    val mag = hypot(dx, dy)
    if (mag > 0.0) { dx /= mag; dy /= mag }
    val pvx = pt.x - lineStart.x
    val pvy = pt.y - lineStart.y

    // Get dot product (project pv onto normalized direction)
    val pvdot = dx * pvx + dy * pvy

    // Scale line direction vector and substract it from pv
    val ax = pvx - pvdot * dx
    val ay = pvy - pvdot * dy

    return hypot(ax, ay)
}