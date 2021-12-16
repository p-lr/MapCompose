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
import ovh.plrapps.mapcompose.api.rotateTo
import ovh.plrapps.mapcompose.api.rotation
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.core.throttle
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.MinimumScaleMode
import ovh.plrapps.mapcompose.utils.AngleDegree
import ovh.plrapps.mapcompose.utils.toRad

/**
 * The state of the map. All public APIs are extensions functions or extension properties of this
 * class.
 *
 * @param levelCount The number of levels in the pyramid.
 * @param fullWidth The width in pixels of the map at scale 1f.
 * @param fullHeight The height in pixels of the map at scale 1f.
 */
class MapState(
    levelCount: Int,
    fullWidth: Int,
    fullHeight: Int,
    initialValues: InitialValues = InitialValues()
) : ZoomPanRotateStateListener {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal val zoomPanRotateState = ZoomPanRotateState(
        fullWidth = fullWidth,
        fullHeight = fullHeight,
        stateChangeListener = this,
        minimumScaleMode = initialValues.minimumScaleMode,
        maxScale = initialValues.maxScale,
        scale = initialValues.scale,
        rotation = initialValues.rotation,
        isRotationEnabled = initialValues.isRotationEnabled
    )
    internal val markerState = MarkerState()
    internal val pathState = PathState()
    internal val visibleTilesResolver =
        VisibleTilesResolver(
            levelCount = levelCount,
            fullWidth = fullWidth,
            fullHeight = fullHeight,
            tileSize = initialValues.tileSize,
            magnifyingFactor = initialValues.magnifyingFactor
        ) {
            zoomPanRotateState.scale
        }
    internal val tileCanvasState = TileCanvasState(
        scope,
        initialValues.tileSize,
        visibleTilesResolver,
        initialValues.workerCount,
        initialValues.highFidelityColors
    )

    private val throttledTask = scope.throttle(wait = 18) {
        renderVisibleTiles()
    }
    private val viewport = Viewport()
    internal var preloadingPadding: Int = initialValues.preloadingPadding
    internal val tileSize by mutableStateOf(initialValues.tileSize)
    internal var stateChangeListener: (MapState.() -> Unit)? = null
    internal var touchDownCb: (() -> Unit)? = null
    internal var mapBackground by mutableStateOf(Color.White)
    internal var isFilteringBitmap: () -> Boolean by mutableStateOf(
        { initialValues.isFilteringBitmap(this) }
    )
    private var consumeDynamicInitialValues: () -> Unit = {
        consumeDynamicInitialValues = {}
        applyDynamicInitialValues(initialValues)
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
        consumeDynamicInitialValues()

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
        val padding = preloadingPadding
        return viewport.apply {
            left = zoomPanRotateState.scrollX.toInt() - padding - zoomPanRotateState.padding.x
            top = zoomPanRotateState.scrollY.toInt() - padding - zoomPanRotateState.padding.y
            right = left + zoomPanRotateState.layoutSize.width + padding * 2
            bottom = top + zoomPanRotateState.layoutSize.height + padding * 2
            angleRad = zoomPanRotateState.rotation.toRad()
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
    internal var scale: Float = 1f
    internal var minimumScaleMode: MinimumScaleMode = Fit
    internal var maxScale: Float = 2f
    internal var rotation: AngleDegree = 0f
    internal var magnifyingFactor = 0
    internal var tileSize: Int = 256
    internal var workerCount: Int = Runtime.getRuntime().availableProcessors() - 1
    internal var highFidelityColors: Boolean = true
    internal var isRotationEnabled: Boolean = false
    internal var preloadingPadding: Int = 0
    internal var isFilteringBitmap: (MapState) -> Boolean = { true }

    /**
     * Initial scroll position. Defaults to centering on the provided scroll destination.
     *
     * @param x The normalized X position on the map, in range [0..1]
     * @param y The normalized Y position on the map, in range [0..1]
     * @param screenOffset Offset of the screen relatively to its dimension. Default is
     * Offset(-0.5f, -0.5f), so moving the screen by half the width left and by half the height top,
     * effectively centering on the scroll destination.
     */
    fun scroll(x: Double, y: Double, screenOffset: Offset = Offset(-0.5f, -0.5f)) = apply {
        this.screenOffset = screenOffset
        this.x = x
        this.y = y
    }

    /**
     * Initial scale. Defaults to 1f.
     */
    fun scale(scale: Float) = apply {
        this.scale = scale
    }

    /**
     * [MinimumScaleMode]. Defaults to [Fit].
     */
    fun minimumScaleMode(minimumScaleMode: MinimumScaleMode) = apply {
        this.minimumScaleMode = minimumScaleMode
    }

    /**
     * Maximum allowed scale. Defaults to 2f.
     */
    fun maxScale(maxScale: Float) = apply {
        this.maxScale = maxScale
    }

    /**
     * Initial rotation. Defaults to 0° (no rotation).
     */
    fun rotation(rotation: AngleDegree) = apply {
        this.rotation = rotation
    }

    /**
     * Alters the level at which tiles are picked for a given scale. By default, the level
     * immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
     * [magnifyingFactor] of 0. The value 1 will result in picking the current level at a given
     * scale, which will be at a relative scale between 1.0 and 2.0
     */
    fun magnifyingFactor(magnifyingFactor: Int) = apply {
        this.magnifyingFactor = magnifyingFactor.coerceAtLeast(0)
    }

    /**
     * The size in pixels of tiles, which are expected to be square. Defaults to 256.
     */
    fun tileSize(tileSize: Int) = apply {
        this.tileSize = tileSize
    }

    /**
     * The thread count used to fetch tiles. Defaults to the number of cores minus one, which
     * works well for tiles in the file system or in a local database. However, that number
     * should be increased to 16 or more for remote tiles (HTTP requests).
     */
    fun workerCount(workerCount: Int) = apply {
        this.workerCount = workerCount
    }

    /**
     * By default, bitmaps are loaded using ARGB_8888, which is best suited for most usages.
     * However, if you're only loading images without alpha channel and high fidelity color isn't
     * a requirement, RGB_565 can be used instead for less memory usage (by setting this to false).
     * Beware, however, that some types of images can't be loaded using RGB_565 (such as PNGs with
     * alpha channel). Unless you know what you're doing, let this parameter be true.
     */
    fun highFidelityColors(enabled: Boolean) = apply {
        this.highFidelityColors = enabled
    }

    /**
     * Controls if the map can be rotated by user gestures. The map can always be programmatically
     * rotated using APIs such as [rotateTo] or [rotation].
     */
    fun rotationEnabled(enabled: Boolean) = apply {
        this.isRotationEnabled = enabled
    }

    /**
     * By default, only visible tiles are loaded. By adding a preloadingPadding additional tiles
     * will be loaded, which can be used to produce a seamless tile loading effect.
     */
    fun preloadingPadding(preloadingPadding: Int) = apply {
        this.preloadingPadding = preloadingPadding.coerceAtLeast(0)
    }

    /**
     * Controls whether Bitmap filtering is enabled when drawing tiles. This is enabled by default.
     * Disabling it is useful to achieve nearest-neighbor scaling, for cases when the art style of
     * the displayed image benefits from it.
     * @see [android.graphics.Paint.setFilterBitmap]
     */
    fun bitmapFilteringEnabled(enabled: Boolean) = apply {
        bitmapFilteringEnabled { enabled }
    }

    /**
     * A version of [bitmapFilteringEnabled] which allows for dynamic control of bitmap filtering
     * depending on the current [MapState].
     */
    fun bitmapFilteringEnabled(predicate: (state: MapState) -> Boolean) = apply {
        isFilteringBitmap = predicate
    }
}