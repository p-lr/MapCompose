package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.onTouchDown
import ovh.plrapps.mapcompose.api.rotateTo
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider
import ovh.plrapps.mapcompose.demo.ui.widgets.Marker
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * This demo shows how animations can be chained one after another.
 * Since animations APIs are suspending functions, this is easy to do.
 */
class AnimationDemoVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)
    private var job: Job? = null
    private val spec = TweenSpec<Float>(2000, easing = FastOutSlowInEasing)

    val state = MapState(4, 4096, 4096).apply {
        addLayer(tileStreamProvider)
        shouldLoopScale = true
        enableRotation()
        addMarker("m0", 0.5, 0.5) { Marker() }
        addMarker("m1", 0.78, 0.78) { Marker() }
        addMarker("m2", 0.79, 0.79) { Marker() }
        addMarker("m3", 0.785, 0.72) { Marker() }
        onTouchDown {
            job?.cancel()
        }
        viewModelScope.launch {
            scrollTo(0.5, 0.5, 2f, SnapSpec())
        }
    }

    fun startAnimation() {
        /* Cancel ongoing animation */
        job?.cancel()

        /* Start a new one */
        with(state) {
            job = viewModelScope.launch {
                scrollTo(0.0, 0.0, 2f, spec, screenOffset = Offset.Zero)
                scrollTo(0.8, 0.8, 2f, spec)
                rotateTo(180f, spec)
                scrollTo(0.5, 0.5, 0.5f, spec)
                scrollTo(0.5, 0.5, 2f, TweenSpec(800, easing = FastOutSlowInEasing))
                rotateTo(0f, TweenSpec(1000, easing = FastOutSlowInEasing))
            }
        }
    }
}