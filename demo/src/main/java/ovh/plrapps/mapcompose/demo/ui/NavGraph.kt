package ovh.plrapps.mapcompose.demo.ui

const val HOME = "home"

enum class MainDestinations(val title: String) {
    MAP_ALONE("Simple map"),
    MAP_WITH_ROTATION_CONTROLS("Map with rotation controls"),
    ADDING_MARKERS("Adding markers"),
    CENTERING_ON_MARKER("Centering on marker"),
    PATHS("Map with paths"),
    CUSTOM_DRAW("Map with custom drawings"),
    CALLOUT_DEMO("Callout (tap markers)")
}
