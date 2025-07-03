@file:OptIn(ExperimentalCoroutinesApi::class)

package ovh.plrapps.mapcompose.state

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.plrapps.mapcompose.core.Layer
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.ui.state.TileCanvasState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TileCanvasStateTest {

    /**
     * This test checks that the correct list of tiles is sent for rendering when `infiniteScrollX`
     * is set to `true`.
     */
    @Test
    fun infiniteScrollTest() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val scaleProvider = object : VisibleTilesResolver.ScaleProvider {
            override fun getScale(): Double {
                return 1.0
            }
        }
        val tileCanvasState = TileCanvasState(
            parentScope = backgroundScope,
            visibleTilesResolver = VisibleTilesResolver(
                levelCount = 4,
                fullWidth = 1024,
                fullHeight = 1024,
                infiniteScrollX = true,
                scaleProvider = scaleProvider
            ),
            workerCount = 4,
            tileSize = 256,
            highFidelityColors = true
        )

        tileCanvasState.setLayers(
            listOf(
                Layer(
                    id = "id",
                    tileStreamProvider = TileStreamProvider { _, _, _ ->
                        return@TileStreamProvider null // actual bitmaps don't matter in this test
                    }
                )
            )
        )


        launch(Dispatchers.Default) {
            /* Overflow on the left */
            tileCanvasState.setViewport(Viewport(-1024, 0, 256, 512))
            delay(500)  // wait for tile production

            assertEquals(8, tileCanvasState.tilesToRender.size)
            var tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 0 }
            assertEquals(-1..0, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 0 }
            assertEquals(-1..0, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 1 }
            assertEquals(-1..-1, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 1 }
            assertEquals(-1..-1, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 2 }
            assertEquals(-1..-1, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 3 }
            assertEquals(-1..-1, tile?.phases)

            /* Overflow on the right */
            tileCanvasState.setViewport(Viewport(768, 0, 1024 + 1024 + 1024, 512))
            delay(500)  // wait for tile production

            assertEquals(8, tileCanvasState.tilesToRender.size)
            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 3 }
            assertEquals(0..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 3 }
            assertEquals(0..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 1 }
            assertEquals(1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 1 }
            assertEquals(1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 2 }
            assertEquals(1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 3 }
            assertEquals(0..2, tile?.phases)

            /* Overflow on both left and right */
            tileCanvasState.setViewport(Viewport(-1024, 0, 1024 + 1024 + 1024, 512))
            delay(500)  // wait for tile production

            assertEquals(8, tileCanvasState.tilesToRender.size)
            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 3 }
            assertEquals(-1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 3 }
            assertEquals(-1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 1 }
            assertEquals(-1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 1 }
            assertEquals(-1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 0 && it.col == 2 }
            assertEquals(-1..2, tile?.phases)

            tile = tileCanvasState.tilesToRender.firstOrNull { it.row == 1 && it.col == 3 }
            assertEquals(-1..2, tile?.phases)
        }
    }
}