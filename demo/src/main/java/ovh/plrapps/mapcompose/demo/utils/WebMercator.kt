package ovh.plrapps.mapcompose.demo.utils

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Given a [latitude] and [longitude], get the corresponding relative coordinates (values between
 * 0.0 and 1.0).
 * It is assumed that the map uses the Web Mercator projection.
 */
fun latLonToNormalized(latitude: Double, longitude: Double): Pair<Double, Double> {
    val latRad = latitude * PI / 180.0
    val lngRad = longitude * PI / 180.0

    // Web Mercator projected coordinates
    val X = earthRadius * lngRad
    val Y = earthRadius * ln(tan((PI / 4.0) + (latRad / 2.0)))

    // Relative coordinates for MapCompose
    val piR = earthRadius * PI
    val normalizedX = (X + piR) / (2.0 * piR)
    val normalizedY = (piR - Y) / (2.0 * piR)

    return Pair(normalizedX, normalizedY)
}

/**
 * Given relative coordinates in a world map (a square of size 2.0 * piR), get the corresponding
 * latitude and longitude.
 * It is assumed that the map uses the Web Mercator projection.
 */
fun normalizedToLatLon(normalizedX: Double, normalizedY: Double): Pair<Double, Double> {
    val piR = earthRadius * PI
    val mercatorX = normalizedX * (2.0 * piR) - piR
    val mercatorY = piR - normalizedY * (2.0 * piR)

    val num = mercatorX / earthRadius
    val num2 = num * 180.0 / PI
    val num3 = floor((num2 + 180) / 360.0f)

    val lng = num2 - (num3 * 360)
    val num4 = PI / 2 - (2.0 * atan(exp(-mercatorY / earthRadius)))
    val lat = num4 * 180.0 / PI
    return Pair(lat, lng)
}

private const val earthRadius = 6_378_137.0 // in meters