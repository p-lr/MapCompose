package ovh.plrapps.mapcompose.demo.utils

import kotlin.random.Random.Default.nextDouble

fun randomDouble(center: Double, radius: Double) : Double {
    return nextDouble(from = center - radius, until = center + radius)
}
