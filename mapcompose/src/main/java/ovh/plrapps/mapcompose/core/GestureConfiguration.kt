package ovh.plrapps.mapcompose.core

import android.view.ViewConfiguration

/**
 * Configuration of various gestures.
 * Scroll fling friction is controlled by [ViewConfiguration.getScrollFriction].
 */
class GestureConfiguration {
    /**
     * The friction multiplier of the zoom fling, indicating how quickly the animation should stop.
     * This should be greater than 0, with a default value of 1.5f. Minimum allowed value is 0.5f.
     */
    var flingZoomFriction: Float = 1.5f
        set(value) {
            field = value.coerceAtLeast(0.5f)
        }
}