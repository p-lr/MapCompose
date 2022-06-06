package ovh.plrapps.mapcompose.testapp.features.clustering

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.testapp.R
import ovh.plrapps.mapcompose.testapp.utils.randomDouble
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import kotlin.random.Random.Default.nextDouble

/**
 * In this sample, an experimental clustering algorithm is used to display 400 markers.
 * The lazy loading technique (removing a marker/cluster when it's not visible) is also used for
 * performance reasons.
 */
@OptIn(ExperimentalClusteringApi::class)
class MarkersClusteringViewModel(application: Application) : AndroidViewModel(application) {
    private val tileStreamProvider = makeTileStreamProvider(application.applicationContext)

    private fun makeTileStreamProvider(appContext: Context): TileStreamProvider {
        return TileStreamProvider { row, col, level ->
            runCatching {
                appContext.assets?.open("tiles/mont_blanc/$level/$row/$col.jpg")
            }.getOrNull()
        }
    }

    val state: MapState by mutableStateOf(
        MapState(4, 4096, 4096) {
            scale(0.81f)
            maxScale(8f)
        }.apply {
            addLayer(tileStreamProvider)
            enableRotation()
            shouldLoopScale = true
            onMarkerClick { id, x, y ->
                println("on marker click $id $x $y")
            }
        }
    )

    init {
        state.addClusterer("default") { n ->
            {
                /* Here we can customize the cluster style */
                Box(
                    modifier = Modifier
                        .background(
                            Color(0x992196F3),
                            shape = CircleShape
                        )
                        .size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = n.size.toString(), color = Color.White)
                }
            }
        }

        repeat(40) { i ->
            val cx = nextDouble()
            val cy = nextDouble()
            repeat(10) { j ->
                val x = randomDouble(cx, 0.03).coerceAtLeast(0.0)
                val y = randomDouble(cy, 0.03).coerceAtLeast(0.0)

                /* Notice how we set the cluster which we previously added */
                state.addMarker(
                    "marker-$i-$j", x, y,
                    renderingStrategy = RenderingStrategy.Clustering("default")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.map_marker),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xEE2196F3)
                    )
                }
            }
        }
    }
}
