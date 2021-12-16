package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.core.throttle
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.MinimumScaleMode
import ovh.plrapps.mapcompose.utils.toRad

/**
 * The state of the map. All public APIs are extensions functions or extension properties of this
 * class.
 *
 * @param levelCount The number of levels in the pyramid.
 * @param fullWidth The width in pixels of the map at scale 1f.
 * @param fullHeight The height in pixels of the map at scale 1f.
 * @param tileSize The size in pixels of tiles, which are expected to be squared. Defaults to 256.
 * @param workerCount The thread count used to fetch tiles. Defaults to the number of cores minus
 * one, which works well for tiles in the file system or in a local database. However, that number
 * should be increased to 16 or more for remote tiles (HTTP requests).
 * @param highFidelityColors By default, bitmaps are loaded using ARGB_8888, which is best suited
 * for most usages. However, if you're only loading images without alpha channel and high fidelity
 * color isn't a requirement, RGB_565 can be used instead for less memory usage.
 * Beware, however, that some types of images can't be loaded using RGB_565 (such as PNGs with alpha
 * channel). Unless you know what you're doing, let this parameter to true.
 */
class MapState(
    levelCount: Int,
    fullWidth: Int,
    fullHeight: Int,
    tileSize: Int = 256,
    workerCount: Int = Runtime.getRuntime().availableProcessors() - 1,
    highFidelityColors: Boolean = true,
    initialValues: InitialValues? = null
) : ZoomPanRotateStateListener {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal val zoomPanRotateState = ZoomPanRotateState(fullWidth, fullHeight, this)
    internal val markerState = MarkerState()
    internal val pathState = PathState()
    internal val visibleTilesResolver =
        VisibleTilesResolver(levelCount, fullWidth, fullHeight, tileSize) {
            zoomPanRotateState.scale
        }
    internal val tileCanvasState = TileCanvasState(
        scope,
        tileSize,
        visibleTilesResolver,
        workerCount,
        highFidelityColors
    )

    private val throttledTask = scope.throttle(wait = 18) {
        renderVisibleTiles()
    }
    private val viewport = Viewport()
    internal var padding: Int = 0
    internal val tileSize by mutableStateOf(tileSize)
    internal var stateChangeListener: (MapState.() -> Unit)? = null
    internal var touchDownCb: (() -> Unit)? = null
    internal var mapBackground by mutableStateOf(Color.White)
    internal var isFilteringBitmap: () -> Boolean by mutableStateOf({ true })
    private var initialValues: InitialValues? = null

    init {
        this.initialValues = initialValues
        if (initialValues != null) {
            applyStaticInitialValues(initialValues)
        }
    }

    /**
     * Cancels all internal tasks.
     * After this call, this [MapState] is unusable.
     */
    fun shutdown() {
        scope.cancel()
        tileCanvasState.shutdown()
    }

    override fun onStateChanged() {
        /* Consume initial values */
        initialValues?.also {
            initialValues = null
            applyDynamicInitialValues(it)
        }

        renderVisibleTilesThrottled()
        stateChangeListener?.invoke(this)
    }

    override fun onTouchDown() {
        touchDownCb?.invoke()
    }

    override fun onPressUnconsumed() {
        markerState.removeAllAutoDismissCallouts()
    }

    internal fun renderVisibleTilesThrottled() {
        throttledTask.trySend(Unit)
    }

    private suspend fun renderVisibleTiles() {
        val viewport = updateViewport()
        tileCanvasState.setViewport(viewport)
    }

    private fun updateViewport(): Viewport {
        val padding = padding
        return viewport.apply {
            left = zoomPanRotateState.scrollX.toInt() - padding - zoomPanRotateState.padding.x
            top = zoomPanRotateState.scrollY.toInt() - padding - zoomPanRotateState.padding.y
            right = left + zoomPanRotateState.layoutSize.width + padding * 2
            bottom = top + zoomPanRotateState.layoutSize.height + padding * 2
            angleRad = zoomPanRotateState.rotation.toRad()
        }
    }

    /**
     * Apply "static" initial values - e.g, those which don't depend on the layout size.
     * These are the scale, rotation, minimum scale mode, and magnifying factor.
     */
    private fun applyStaticInitialValues(initialValues: InitialValues) {
        visibleTilesResolver.magnifyingFactor = initialValues.magnifyingFactor

        initialValues.minimumScaleMode?.also {
            zoomPanRotateState.minimumScaleMode = it
        }

        initialValues.maxScale?.also {
            zoomPanRotateState.maxScale = it
        }

        initialValues.scale?.also { scale ->
            zoomPanRotateState.setScale(scale, notify = false)
        }

        initialValues.rotation?.also { rotation ->
            zoomPanRotateState.setRotation(rotation, notify = false)
        }
    }

    /**
     * Apply "dynamic" initial values - e.g, those which depend on the layout size.
     * For the moment, the scroll is the only one.
     */
    private fun applyDynamicInitialValues(initialValues: InitialValues) {
        with(zoomPanRotateState) {
            val offsetX = initialValues.screenOffset.x * layoutSize.width
            val offsetY = initialValues.screenOffset.y * layoutSize.height

            val destScrollX = (initialValues.x * fullWidth * scale + offsetX).toFloat()
            val destScrollY = (initialValues.y * fullHeight * scale + offsetY).toFloat()

            setScroll(destScrollX, destScrollY)
        }
    }
}

/**
 * Builder for initial values.
 * Changes made after the `MapState` instance creation take precedence over initial values.
 * In the following example, the init scale will be 4f since the max scale is later set to 4f.
 *
 * ```
 * MapState(4, 4096, 4096,
 *   initialValues = InitialValues().scale(8f)
 * ).apply {
 *   addLayer(tileStreamProvider)
 *   maxScale = 4f
 * }
 * ```
 */
class InitialValues {
    internal var x = 0.0
    internal var y = 0.0
    internal var screenOffset: Offset = Offset.Zero
    internal var scale: Float? = null
    internal var minimumScaleMode: MinimumScaleMode? = null
    internal var maxScale: Float? = null
    internal var rotation: Float? = null
    internal var magnifyingFactor = 0

    /**
     * Init the scroll position. Defaults to centering on the provided scroll destination.
     *
     * @param x The normalized X position on the map, in range [0..1]
     * @param y The normalized Y position on the map, in range [0..1]
     * @param screenOffset Offset of the screen relatively to its dimension. Default is
     * Offset(-0.5f, -0.5f), so moving the screen by half the width left and by half the height top,
     * effectively centering on the scroll destination.
     */
    fun scroll(x: Double, y: Double, screenOffset: Offset = Offset(-0.5f, -0.5f)): InitialValues {
        this.screenOffset = screenOffset
        this.x = x
        this.y = y
        return this
    }

    /**
     * Set the initial scale. If necessary, the initial [MinimumScaleMode] can be specified (the
     * default is [Fit]).
     */
    fun scale(
        scale: Float,
        minimumScaleMode: MinimumScaleMode = minimumScaleModeDefault,
        maxScale: Float = maxScaleDefault
    ): InitialValues {
        this.minimumScaleMode = minimumScaleMode
        this.maxScale = maxScale
        this.scale = scale
        return this
    }

    fun rotation(rotation: Float): InitialValues {
        this.rotation = rotation
        return this
    }

    /**
     * Alters the level at which tiles are picked for a given scale. By default, the level
     * immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
     * [magnifyingFactor] of 0. The value 1 will result in picking the current level at a given scale,
     * which will be at a relative scale between 1.0 and 2.0
     */
    fun magnifyingFactor(magnifyingFactor: Int): InitialValues {
        this.magnifyingFactor = magnifyingFactor.coerceAtLeast(0)
        return this
    }
}