package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.TweenSpec
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.snapScrollTo
import ovh.plrapps.mapcompose.demo.ui.widgets.Marker
import ovh.plrapps.mapcompose.ui.state.MapState

/**
 * This demo shows a strange "hop" when scroll is animated
 */
class AnimationJitterDemoVM(application: Application) : AndroidViewModel(application) {
    private var job: Job? = null

    val state = MapState(
        levelCount = 1,
        fullWidth = 5000,
        fullHeight = 5000,
    ).apply {
        scale = 1f
        addMarker("m0", 0.25, 0.25) { Marker() }
        addMarker("m1", 0.5, 0.5) { Marker() }
        addMarker("m2", 0.75, 0.75) { Marker() }
    }

    fun startAnimation() {
        /* Cancel ongoing animation */
        job?.cancel()

        /* Start a new one */
        with(state) {
            job = viewModelScope.launch {
                scale = 1f
                snapScrollTo(0.25, 0.25)

                scrollTo(
                    0.5,
                    0.5,
                    animationSpec = TweenSpec(2500, easing = EaseOutQuart), // take 2.5sec to animate so we can see the hop easily
                    destScale = 0.1f,
                )
            }
        }
    }
}
