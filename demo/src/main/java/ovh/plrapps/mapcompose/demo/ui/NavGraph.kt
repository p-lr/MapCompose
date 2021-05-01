package ovh.plrapps.mapcompose.demo.ui

const val HOME = "home"

enum class MainDestinations(val title: String) {
    MAP_ALONE("Simple map"),
    MAP_WITH_ROTATION_CONTROLS("Map with rotation controls"),
    ADDING_MARKERS("Adding markers"),
    CENTERING_ON_MARKER("Centering on marker"),
    PATHS("Map with paths")
}
