package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ovh.plrapps.mapcompose.core.GestureConfiguration
import ovh.plrapps.mapcompose.core.Viewport
import ovh.plrapps.mapcompose.core.VisibleTilesResolver
import ovh.plrapps.mapcompose.core.throttle
import ovh.plrapps.mapcompose.ui.gestures.model.HitType
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.MinimumScaleMode
import ovh.plrapps.mapcompose.ui.state.markers.MarkerRenderState
import ovh.plrapps.mapcompose.ui.state.markers.MarkerState
import ovh.plrapps.mapcompose.utils.AngleDegree
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
 * @param initialValuesBuilder A builder for [InitialValues] which are applied during [MapState]
 * initialization. Note that the provided lambda should not start any coroutines.
 */
class MapState(
    levelCount: Int,
    fullWidth: Int,
    fullHeight: Int,
    tileSize: Int = 256,
    workerCount: Int = Runtime.getRuntime().availableProcessors() - 1,
    initialValuesBuilder: InitialValues.() -> Unit = {}
) : ZoomPanRotateStateListener {
    private val initialValues = InitialValues().apply(initialValuesBuilder)
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal val zoomPanRotateState = ZoomPanRotateState(
        fullWidth = fullWidth,
        fullHeight = fullHeight,
        stateChangeListener = this,
        minimumScaleMode = initialValues.minimumScaleMode,
        maxScale = initialValues.maxScale,
        scale = initialValues.scale,
        rotation = initialValues.rotation,
        gestureConfiguration = initialValues.gestureConfiguration
    )
    internal val markerRenderState = MarkerRenderState()
    internal val markerState = MarkerState(scope, markerRenderState)
    internal val pathState = PathState(fullWidth, fullHeight)
    internal val visibleTilesResolver =
        VisibleTilesResolver(
            levelCount = levelCount,
            fullWidth = fullWidth,
            fullHeight = fullHeight,
            tileSize = tileSize,
            magnifyingFactor = initialValues.magnifyingFactor
        ) {
            zoomPanRotateState.scale
        }
    internal val tileCanvasState = TileCanvasState(
        scope,
        tileSize,
        visibleTilesResolver,
        workerCount,
        initialValues.highFidelityColors
    )

    private val throttledTask = scope.throttle(wait = 18) {
        renderVisibleTiles()
    }
    private val viewport = Viewport()
    internal var preloadingPadding: Int = initialValues.preloadingPadding
    internal val tileSize by mutableIntStateOf(tileSize)
    internal var stateChangeListener: (MapState.() -> Unit)? = null
    internal var touchDownCb: (() -> Unit)? = null
    internal var tapCb: LayoutTapCb? = null
    internal var longPressCb: LayoutTapCb? = null
    internal var mapBackground by mutableStateOf(Color.Transparent)
    internal var isFilteringBitmap: () -> Boolean by mutableStateOf(
        { initialValues.isFilteringBitmap(this) }
    )
    private var consumeLateInitialValues: () -> Unit = {
        consumeLateInitialValues = {}
        applyLateInitialValues(initialValues)
    }

    /**
     * Cancels all internal tasks.
     * After this call, this [MapState] is unusable.
     */
    @Suppress("unused")
    fun shutdown() {
        scope.cancel()
        tileCanvasState.shutdown()
    }

    override fun onStateChanged() {
        consumeLateInitialValues()

        renderVisibleTilesThrottled()
        stateChangeListener?.invoke(this)
    }

    override fun onTouchDown() {
        touchDownCb?.invoke()
    }

    override fun onPress() {
        markerRenderState.removeAllAutoDismissCallouts()
    }

    override fun onLongPress(x: Double, y: Double) {
        longPressCb?.invoke(x, y)
    }

    override fun onTap(x: Double, y: Double) {
        tapCb?.invoke(x, y)
    }

    override fun detectsTap(): Boolean = tapCb != null

    override fun detectsLongPress(): Boolean = longPressCb != null

    override fun interceptsTap(x: Double, y: Double, xPx: Int, yPx: Int): Boolean {
        val markerHandled = markerState.onHit(xPx, yPx, hitType = HitType.Click)
        val pathHandled = if (!markerHandled) {
            pathState.onHit(x, y, zoomPanRotateState.scale, hitType = HitType.Click)
        } else false

        return markerHandled || pathHandled
    }

    override fun interceptsLongPress(x: Double, y: Double, xPx: Int, yPx: Int): Boolean {
        val markerHandled = markerState.onHit(xPx, yPx, hitType = HitType.LongPress)
        val pathHandled = if (!markerHandled) {
            pathState.onHit(x, y, zoomPanRotateState.scale, hitType = HitType.LongPress)
        } else false

        return markerHandled || pathHandled
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
            left = zoomPanRotateState.scrollX.toInt() - padding
            top = zoomPanRotateState.scrollY.toInt() - padding
            right = left + zoomPanRotateState.layoutSize.width + padding * 2
            bottom = top + zoomPanRotateState.layoutSize.height + padding * 2
            angleRad = zoomPanRotateState.rotation.toRad()
        }
    }

    /**
     * Apply "late" initial values - e.g, those which depend on the layout size.
     * For the moment, the scroll is the only one.
     */
    private fun applyLateInitialValues(initialValues: InitialValues) {
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
@Suppress("unused")
class InitialValues internal constructor() {
    internal var x = 0.5
    internal var y = 0.5
    internal var screenOffset: Offset = Offset(-0.5f, -0.5f)
    internal var scale: Float = 1f
    internal var minimumScaleMode: MinimumScaleMode = Fit
    internal var maxScale: Float = 2f
    internal var rotation: AngleDegree = 0f
    internal var magnifyingFactor = 0
    internal var highFidelityColors: Boolean = true
    internal var preloadingPadding: Int = 0
    internal var isFilteringBitmap: (MapState) -> Boolean = { true }
    internal var gestureConfiguration: GestureConfiguration = GestureConfiguration()

    /**
     * Init the scroll position. Defaults to centering on the provided scroll destination.
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
     * Set the initial scale. Defaults to 1f.
     */
    fun scale(scale: Float) = apply {
        this.scale = scale
    }

    /**
     * Set the [MinimumScaleMode]. Defaults to [Fit].
     */
    fun minimumScaleMode(minimumScaleMode: MinimumScaleMode) = apply {
        this.minimumScaleMode = minimumScaleMode
    }

    /**
     * Set the maximum allowed scale. Defaults to 2f.
     */
    fun maxScale(maxScale: Float) = apply {
        this.maxScale = maxScale
    }

    /**
     * Set the initial rotation. Defaults to 0° (no rotation).
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
     * By default, only visible tiles are loaded. By adding a preloadingPadding additional tiles
     * will be loaded, which can be used to produce a seamless tile loading effect.
     *
     * @param padding in pixels
     */
    fun preloadingPadding(padding: Int) = apply {
        this.preloadingPadding = padding.coerceAtLeast(0)
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

    /**
     * Customize gestures.
     */
    fun configureGestures(gestureConfigurationBlock: GestureConfiguration.() -> Unit) {
        this.gestureConfiguration.gestureConfigurationBlock()
    }
}

internal typealias LayoutTapCb = (x: Double, y: Double) -> Unit