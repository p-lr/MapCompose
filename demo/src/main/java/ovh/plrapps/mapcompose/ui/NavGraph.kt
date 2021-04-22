package ovh.plrapps.mapcompose.ui

import android.os.Parcelable

const val HOME = "home"

enum class MainDestinations(val title: String) {
    MAP_ALONE("Simple map"),
    MAP_WITH_ROTATION_CONTROLS("Map with rotation controls")
}
