package ovh.plrapps.mapcompose.demo.viewmodels

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addLazyLoader
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import kotlin.random.Random

/**
 * Shows how to define and use a marker lazy-loader.
 */
class MarkersLazyLoadingVM(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider =
        ovh.plrapps.mapcompose.demo.providers.makeTileStreamProvider(application.applicationContext)

    val state = MapState(4, 8192, 8192) {
        minimumScaleMode(Forced(1.0))
        scale(1.0)
        maxScale(4.0)
        scroll(0.5, 0.5)
    }.apply {
        addLayer(tileStreamProvider)
        shouldLoopScale = true
    }

    init {
        /* Add a marker lazy loader. In this example, we use "default" for the id */
        state.addLazyLoader("default")

        repeat(200) { i ->
            val x = Random.nextDouble()
            val y = Random.nextDouble()

            /* Notice how we set the rendering strategy to lazy loading with the same id */
            state.addMarker(
                "marker-$i", x, y,
                renderingStrategy = RenderingStrategy.LazyLoading(lazyLoaderId = "default")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.map_marker),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xEE2196F3)
                )
            }
        }

        /* We can still add regular markers */
        state.addMarker(
            "marker-regular", 0.5, 0.5,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.map_marker),
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color(0xEEF44336)
            )
        }

        state.onMarkerClick { id, x, y ->
            println("marker click $id $x $y")
        }
    }
}