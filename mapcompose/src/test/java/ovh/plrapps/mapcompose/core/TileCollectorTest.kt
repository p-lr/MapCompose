package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

/**
 * Test the [TileCollector.collectTiles] engine. The following assertions are tested:
 * * If [TileSpec]s are send to the input channel, corresponding [Tile]s are received from the
 * output channel (from the [TileCollector.collectTiles] point of view).
 * * The [Bitmap] of the [Tile]s produced should be consistent with the output of the flow
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TileCollectorTest {

    private val tileSize = 256

    companion object {
        private var assetsDir: File? = null

        init {
            try {
                val mapviewDirURL = TileCollectorTest::class.java.classLoader!!.getResource("tiles")
                assetsDir = File(mapviewDirURL.toURI())
            } catch (e: Exception) {
                println("No tiles directory found.")
            }

        }
    }

    @Test
    fun fullTest() = runTest {
        assertNotNull(assetsDir)
        val imageFile = File(assetsDir, "10.jpg")
        assertTrue(imageFile.exists())

        /* Setup the channels */
        val visibleTileLocationsChannel = Channel<TileSpec>(capacity = Channel.RENDEZVOUS)
        val tilesOutput = Channel<Tile>(capacity = Channel.RENDEZVOUS)

        val tileStreamProvider = TileStreamProvider { _, _, _ -> FileInputStream(imageFile) }

        val bitmapReference = try {
            val inputStream = FileInputStream(imageFile)
            BitmapFactory.decodeStream(inputStream, null, null)
        } catch (e: Exception) {
            fail()
            error("Could not decode image")
        }


        val layers = listOf(
            Layer("default", tileStreamProvider)
        )

        /* Start collecting tiles */
        val tileCollector = TileCollector(1, optimizeForLowEndDevices = false, tileSize)
        val tileCollectorJob = launch {
            tileCollector.collectTiles(visibleTileLocationsChannel, tilesOutput, layers)
        }

        fun CoroutineScope.consumeTiles(tileChannel: ReceiveChannel<Tile>) = launch {
            var receivedTiles = 0
            for (tile in tileChannel) {
                println("received tile ${tile.zoom}-${tile.row}-${tile.col}")
                assertTrue(tile.bitmap?.sameAs(bitmapReference) ?: false)
                receivedTiles += 1

                if (tile.zoom == 6 && tile.row == 6 && tile.col == 6) {
                    println("received poison pill")
                    assertEquals(7, receivedTiles)
                    cancel()
                    tileCollectorJob.cancel()
                }
            }
        }

        /* Start consuming tiles */
        consumeTiles(tilesOutput)

        launch {
            val locations1 = listOf(
                TileSpec(0, 0, 0),
                TileSpec(0, 1, 1),
                TileSpec(0, 2, 1)
            )
            for (spec in locations1) {
                visibleTileLocationsChannel.send(spec)
            }

            val locations2 = listOf(
                TileSpec(1, 0, 0),
                TileSpec(1, 1, 1),
                TileSpec(1, 2, 1),
                TileSpec(6, 6, 6),  // poison pill
            )

            for (spec in locations2) {
                visibleTileLocationsChannel.send(spec)
            }
        }
        Unit
    }
}