package ovh.plrapps.mapcompose.ui.paths.model

sealed interface PatternItem {
    data class Dash(val lengthPx: Float): PatternItem
    data object Dot: PatternItem
    data class Gap(val lengthPx: Float): PatternItem
}
