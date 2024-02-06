package ovh.plrapps.mapcompose.demo.ui

const val HOME = "home"

enum class MainDestinations(val title: String) {
    MAP_ALONE("Simple map"),
    LAYERS_DEMO("Layers demo"),
    MAP_WITH_ROTATION_CONTROLS("Map with rotation controls"),
    ADDING_MARKERS("Adding markers"),
    CENTERING_ON_MARKER("Centering on marker"),
    PATHS("Map with paths"),
    CUSTOM_DRAW("Map with custom drawings"),
    CALLOUT_DEMO("Callout (tap markers)"),
    ANIMATION_DEMO("Animation demo"),
    OSM_DEMO("Open Street Map demo"),
    HTTP_TILES_DEMO("Remote HTTP tiles"),
    VISIBLE_AREA_PADDING("Visible area padding"),
    MARKERS_CLUSTERING("Markers clustering"),
    MARKERS_LAZY_LOADING("Markers lazy loading")
}
