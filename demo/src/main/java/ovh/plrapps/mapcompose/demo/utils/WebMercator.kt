package ovh.plrapps.mapcompose.demo.utils

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

/**
 * Given a [latitude] and [longitude], get the corresponding relative coordinates (values between
 * 0.0 and 1.0).
 * It is assumed that the map uses the Web Mercator projection.
 */
fun lonLatToNormalized(latitude: Double, longitude: Double): Pair<Double, Double> {
    val earthRadius = 6_378_137.0 // in meters
    val latRad = latitude * PI / 180.0
    val lngRad = longitude * PI / 180.0

    // Web Mercator projected coordiantes
    val X = earthRadius * lngRad
    val Y = earthRadius * ln(tan((PI / 4.0) + (latRad / 2.0)))

    // Relative coordinates for MapCompose
    val piR = earthRadius * PI
    val normalizedX = (X + piR) / (2.0 * piR)
    val normalizedY = (piR - Y) / (2.0 * piR)

    return Pair(normalizedX, normalizedY)
}