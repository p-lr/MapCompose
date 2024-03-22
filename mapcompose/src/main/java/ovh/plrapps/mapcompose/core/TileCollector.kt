package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.pow


/**
 * The engine of MapCompose. The view-model uses two channels to communicate with the [TileCollector]:
 * * one to send [TileSpec]s (a [SendChannel])
 * * one to receive [TileSpec]s (a [ReceiveChannel])
 *
 * The [TileCollector] encapsulates all the complexity that transforms a [TileSpec] into a [Tile].
 * ```
 *                                              _____________________________________________________________________
 *                                             |                           TileCollector             ____________    |
 *                                  tiles      |                                                    |  ________  |   |
 *              ---------------- [*********] <----------------------------------------------------- | | worker | |   |
 *             |                               |                                                    |  --------  |   |
 *             ↓                               |                                                    |  ________  |   |
 *  _____________________                      |                                   tileSpecs        | | worker | |   |
 * | TileCanvasViewModel |                     |    _____________________  <---- [**********] <---- |  --------  |   |
 *  ---------------------  ----> [*********] ----> | tileCollectorKernel |                          |  ________  |   |
 *                                tileSpecs    |    ---------------------  ----> [**********] ----> | | worker | |   |
 *                                             |                                   tileSpecs        |  --------  |   |
 *                                             |                                                    |____________|   |
 *                                             |                                                      worker pool    |
 *                                             |                                                                     |
 *                                              ---------------------------------------------------------------------
 * ```
 * This architecture is an example of Communicating Sequential Processes (CSP).
 *
 * @author p-lr on 22/06/19
 */
internal class TileCollector(
    private val workerCount: Int,
    private val bitmapConfiguration: BitmapConfiguration,
    private val tileSize: Int
) {
    @Volatile
    var isIdle: Boolean = true

    /**
     * Sets up the tile collector machinery. The architecture is inspired from
     * [Kotlin Conf 2018](https://www.youtube.com/watch?v=a3agLJQ6vt8).
     * It support back-pressure, and avoids deadlock in CSP taking into account recommendations of
     * this [article](https://medium.com/@elizarov/deadlocks-in-non-hierarchical-csp-e5910d137cc),
     * which is from the same author.
     *
     * @param [tileSpecs] channel of [TileSpec], which capacity should be [Channel.RENDEZVOUS].
     * @param [tilesOutput] channel of [Tile], which should be set as [Channel.RENDEZVOUS].
     */
    suspend fun collectTiles(
        tileSpecs: ReceiveChannel<TileSpec>,
        tilesOutput: SendChannel<Tile>,
        layers: List<Layer>,
        bitmapPool: BitmapPool
    ) = coroutineScope {
        val tilesToDownload = Channel<TileSpec>(capacity = Channel.RENDEZVOUS)
        val tilesDownloadedFromWorker = Channel<TileSpec>(capacity = 1)

        repeat(workerCount) {
            worker(
                tilesToDownload,
                tilesDownloadedFromWorker,
                tilesOutput,
                layers,
                bitmapPool
            )
        }
        tileCollectorKernel(tileSpecs, tilesToDownload, tilesDownloadedFromWorker)
    }

    private fun CoroutineScope.worker(
        tilesToDownload: ReceiveChannel<TileSpec>,
        tilesDownloaded: SendChannel<TileSpec>,
        tilesOutput: SendChannel<Tile>,
        layers: List<Layer>,
        bitmapPool: BitmapPool
    ) = launch(dispatcher) {

        val layerIds = layers.map { it.id }
        val bitmapLoadingOptionsForLayer = layerIds.associateWith {
            BitmapFactory.Options().apply {
                inPreferredConfig = bitmapConfiguration.bitmapConfig
            }
        }
        val bitmapForLayer = layerIds.associateWith {
            Bitmap.createBitmap(tileSize, tileSize, bitmapConfiguration.bitmapConfig)
        }
        val canvas = Canvas()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        suspend fun getBitmapFromPoolOrCreate(subSamplingRatio: Int): Bitmap {
            val subSampledSize = tileSize / subSamplingRatio
            val allocationByteCount = subSampledSize * subSampledSize * bitmapConfiguration.bytesPerPixel
            return bitmapPool.get(allocationByteCount) ?: Bitmap.createBitmap(subSampledSize, subSampledSize, bitmapConfiguration.bitmapConfig)
        }

        fun getBitmap(
            subSamplingRatio: Int,
            layer: Layer,
            inputStream: InputStream,
            inBitmapForced: Bitmap? = null
        ): BitmapForLayer {
            val bitmapLoadingOptions =
                bitmapLoadingOptionsForLayer[layer.id] ?: return BitmapForLayer(null, layer)

            bitmapLoadingOptions.inMutable = true
            bitmapLoadingOptions.inBitmap = inBitmapForced ?: bitmapForLayer[layer.id]
            bitmapLoadingOptions.inSampleSize = subSamplingRatio

            return inputStream.use {
                val bitmap = runCatching {
                    BitmapFactory.decodeStream(inputStream, null, bitmapLoadingOptions)
                }.getOrNull()
                BitmapForLayer(bitmap, layer)
            }
        }

        for (spec in tilesToDownload) {
            if (layers.isEmpty()) {
                tilesDownloaded.send(spec)
                continue
            }

            val subSamplingRatio = 2.0.pow(spec.subSample).toInt()
            val bitmapForLayers = layers.mapIndexed { index, layer ->
                async {
                    val i = layer.tileStreamProvider.getTileStream(spec.row, spec.col, spec.zoom)
                    if (i != null) {
                        getBitmap(
                            subSamplingRatio = subSamplingRatio,
                            layer = layer,
                            inputStream = i,
                            /* Attempt to reuse an existing bitmap for the first layer */
                            inBitmapForced = if (index == 0) getBitmapFromPoolOrCreate(
                                subSamplingRatio
                            ) else null
                        )
                    } else BitmapForLayer(null, layer)
                }
            }.awaitAll()

            val resultBitmap = bitmapForLayers.firstOrNull()?.bitmap ?: run {
                tilesDownloaded.send(spec)
                /* When the decoding failed or if there's nothing to decode, then send back the Tile
                 * just as in normal processing, so that the actor which submits tiles specs to the
                 * collector knows that this tile has been processed and does not immediately
                 * re-sends the same spec. */
                tilesOutput.send(
                    Tile(
                        spec.zoom,
                        spec.row,
                        spec.col,
                        spec.subSample,
                        layerIds,
                        layers.map { it.alpha }
                    )
                )
                null
            } ?: continue // If the decoding of the first layer failed, skip the rest

            canvas.setBitmap(resultBitmap)

            for (result in bitmapForLayers.drop(1)) {
                paint.alpha = (255f * result.layer.alpha).toInt()
                if (result.bitmap == null) continue
                canvas.drawBitmap(result.bitmap, 0f, 0f, paint)
            }

            val tile = Tile(
                spec.zoom,
                spec.row,
                spec.col,
                spec.subSample,
                layerIds,
                layers.map { it.alpha }
            ).apply {
                this.bitmap = resultBitmap
            }
            tilesOutput.send(tile)
            tilesDownloaded.send(spec)
        }
    }

    private fun CoroutineScope.tileCollectorKernel(
        tileSpecs: ReceiveChannel<TileSpec>,
        tilesToDownload: SendChannel<TileSpec>,
        tilesDownloadedFromWorker: ReceiveChannel<TileSpec>,
    ) = launch(Dispatchers.Default) {

        val specsBeingProcessed = mutableListOf<TileSpec>()

        while (true) {
            select<Unit> {
                tilesDownloadedFromWorker.onReceive {
                    specsBeingProcessed.remove(it)
                    isIdle = specsBeingProcessed.isEmpty()
                }
                tileSpecs.onReceive {
                    if (it !in specsBeingProcessed) {
                        /* Add it to the list of specs being processed */
                        specsBeingProcessed.add(it)
                        isIdle = false

                        /* Now download the tile */
                        tilesToDownload.send(it)
                    }
                }
            }
        }
    }

    /**
     * Attempts to stop all actively executing tasks, halts the processing of waiting tasks.
     */
    fun shutdownNow() {
        executor.shutdownNow()
    }

    /**
     * When using a [LinkedBlockingQueue], the core pool size mustn't be 0, or the active thread
     * count won't be greater than 1. Previous versions used a [SynchronousQueue], which could have
     * a core pool size of 0 and a growing count of active threads. However, a [Runnable] could be
     * rejected when no thread were available. Starting from kotlinx.coroutines 1.4.0, this cause
     * the associated coroutine to be cancelled. By using a [LinkedBlockingQueue], we avoid rejections.
     */
    private val executor = ThreadPoolExecutor(
        workerCount, workerCount,
        60L, TimeUnit.SECONDS, LinkedBlockingQueue()
    ).apply {
        allowCoreThreadTimeOut(true)
    }
    private val dispatcher = executor.asCoroutineDispatcher()
}

internal data class BitmapConfiguration(val bitmapConfig: Config, val bytesPerPixel: Int)

private data class BitmapForLayer(val bitmap: Bitmap?, val layer: Layer)