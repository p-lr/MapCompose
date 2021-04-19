package ovh.plrapps.mapcompose.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Screens : Parcelable {
    @Parcelize
    object Home : Screens()

    @Parcelize
    data class Demo(val name: String) : Screens()
}

enum class MainDestinations {
    HOME,
    MAP_ALONE
}
