package ovh.plrapps.mapcompose.testapp.core.ui.nav

import androidx.annotation.StringRes
import ovh.plrapps.mapcompose.testapp.R

const val HOME = "home"

enum class NavDestinations(@StringRes val title: Int) {
    LAYERS_SWITCH(R.string.layers_switch_test),
    CLUSTERING(R.string.clustering_test)
}