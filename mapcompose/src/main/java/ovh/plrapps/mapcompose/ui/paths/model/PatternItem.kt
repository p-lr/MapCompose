package ovh.plrapps.mapcompose.ui.paths.model

import androidx.compose.ui.unit.Dp

sealed interface PatternItem {
    data class Dash(val length: Dp): PatternItem
    data object Dot: PatternItem
    data class Gap(val length: Dp): PatternItem
}
