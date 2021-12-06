package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.selects.select
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * The engine of the MapView. The view-model uses two channels to communicate with the [TileCollector]:
 * * one to send [WorkerSpec]s (a [SendChannel])
 * * one to receive [WorkerSpec]s (a [ReceiveChannel])
 *
 * The [TileCollector] encapsulates all the complexity that transforms a [TileSpec] into a [Tile].
 * ```
 *                                              _____________________________________________________________________
 *                                             |                           TileCollector             ____________    |
 *                                  tiles      |                                                    |  ________  |   |
 *              ---------------- [*********] <----------------------------------------------------- | | worker | |   |
 *             |                               |                                                    |  --------  |   |
 *             ↓                               |                                                    |  ________  |   |
 *  _____________________                      |                                  workerSpecs       | | worker | |   |
 * | TileCanvasViewModel |                     |    _____________________  <---- [**********] <---- |  --------  |   |
 *  ---------------------  ----> [*********] ----> | tileCollectorKernel |                          |  ________  |   |
 *                                tileSpecs    |    ---------------------  ----> [**********] ----> | | worker | |   |
 *                                             |                                  workerSpecs       |  --------  |   |
 *                                             |                                                    |____________|   |
 *                                             |                                                      worker pool    |
 *                                             |                                                                     |
 *                                              ---------------------------------------------------------------------
 * ```
 * This architecture is an example of Communicating Sequential Processes (CSP).
 *
 * @author peterLaurence on 22/06/19
 */
internal class TileCollector(
    private val workerCount: Int,
    private val bitmapConfig: Bitmap.Config
) {

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
        bitmapFlow: Flow<Bitmap>
    ) = coroutineScope {
        val layerIds = layers.map { it.id }
        val tilesToDownload = Channel<WorkerSpec>(capacity = Channel.RENDEZVOUS)
        val tilesDownloadedFromWorker = Channel<WorkerSpec>(capacity = layerIds.size)

        repeat(workerCount) {
            worker(
                tilesToDownload,
                tilesDownloadedFromWorker,
                tilesOutput,
                layers.associateBy { it.id },
                bitmapFlow
            )
        }
        tileCollectorKernel(tileSpecs, tilesToDownload, tilesDownloadedFromWorker, layerIds)
    }

    private fun CoroutineScope.worker(
        tilesToDownload: ReceiveChannel<WorkerSpec>,
        tilesDownloaded: SendChannel<WorkerSpec>,
        tilesOutput: SendChannel<Tile>,
        layersForId: Map<String, Layer>,
        bitmapFlow: Flow<Bitmap>
    ) = launch(dispatcher) {

        val bitmapLoadingOptions = BitmapFactory.Options()
        bitmapLoadingOptions.inPreferredConfig = bitmapConfig

        for (workerSpec in tilesToDownload) {
            val spec = workerSpec.tileSpec
            val tileStreamProvider = layersForId[workerSpec.layerId]?.tileStreamProvider
            if (tileStreamProvider == null) {
                tilesDownloaded.send(workerSpec)
                continue
            }

            val i = tileStreamProvider.getTileStream(spec.row, spec.col, spec.zoom)

            if (spec.subSample > 0) {
                bitmapLoadingOptions.inBitmap = null
                bitmapLoadingOptions.inScaled = true
                bitmapLoadingOptions.inSampleSize = spec.subSample
            } else {
                bitmapLoadingOptions.inScaled = false
                bitmapLoadingOptions.inBitmap = bitmapFlow.singleOrNull()
                bitmapLoadingOptions.inSampleSize = 0
            }

            try {
                val bitmap = BitmapFactory.decodeStream(i, null, bitmapLoadingOptions) ?: continue
                val tile = Tile(spec.zoom, spec.row, spec.col, spec.subSample, workerSpec.layerId).apply {
                    this.bitmap = bitmap
                }
                tilesOutput.send(tile)
            } catch (e: OutOfMemoryError) {
                // no luck
            } catch (e: Exception) {
                // maybe retry
            } finally {
                tilesDownloaded.send(workerSpec)
                i?.close()
            }
        }
    }

    private fun CoroutineScope.tileCollectorKernel(
        tileSpecs: ReceiveChannel<TileSpec>,
        tilesToDownload: SendChannel<WorkerSpec>,
        tilesDownloadedFromWorker: ReceiveChannel<WorkerSpec>,
        layerIds: List<String>
    ) = launch(Dispatchers.Default) {

        val specsBeingProcessed = mutableMapOf<TileSpec, MutableList<String>>()

        while (true) {
            select<Unit> {
                tilesDownloadedFromWorker.onReceive {
                    val ids = specsBeingProcessed[it.tileSpec] ?: return@onReceive
                    ids.remove(it.layerId)
                    if (ids.isEmpty()) {
                        specsBeingProcessed.remove(it.tileSpec)
                    }
                }
                tileSpecs.onReceive {
                    if (it !in specsBeingProcessed.keys) {
                        /* Add it to the list of locations being processed */
                        specsBeingProcessed[it] = layerIds.toMutableList()

                        /* Download tiles for each layer */
                        for (layerId in layerIds) {
                            tilesToDownload.send(WorkerSpec(it, layerId))
                        }
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

private data class WorkerSpec(val tileSpec: TileSpec, val layerId: String)