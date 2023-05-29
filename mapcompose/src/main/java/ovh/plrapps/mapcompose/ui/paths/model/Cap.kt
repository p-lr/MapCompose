package ovh.plrapps.mapcompose.ui.paths.model

enum class Cap  {
    /** The stroke ends with the path, and does not project beyond it. */
    Butt,

    /**
     * The stroke projects out as a semicircle, with the center at the end of the path.
     */
    Round,

    /**
     * The stroke projects out as a square, with the center at the end of the path.
     */
    Square
}