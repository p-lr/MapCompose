package ovh.plrapps.mapcompose.ui.state

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.*
import ovh.plrapps.mapcompose.core.GestureConfiguration
import ovh.plrapps.mapcompose.ui.layout.*
import ovh.plrapps.mapcompose.utils.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.*

internal class ZoomPanRotateState(
    val fullWidth: Int,
    val fullHeight: Int,
    private val stateChangeListener: ZoomPanRotateStateListener,
    minimumScaleMode: MinimumScaleMode,
    maxScale: Float,
    scale: Float,
    rotation: AngleDegree,
    gestureConfiguration: GestureConfiguration
) : GestureListener, LayoutSizeChangeListener {
    private var scope: CoroutineScope? = null
    private var onLayoutContinuations = mutableListOf<Continuation<Unit>>()

    /**
     * Suspends until the view is laid out. To do that, we use the [scope] as flag.
     *
     * _Contract_:
     * On layout change, [scope] and [layoutSize] are initialized, and queued continuations
     * are resumed.
     */
    internal suspend fun awaitLayout() {
        if (scope != null) return
        suspendCoroutine {
            onLayoutContinuations.add(it)
        }
    }

    internal var minimumScaleMode: MinimumScaleMode = minimumScaleMode
        set(value) {
            field = value
            recalculateMinScale()
        }

    private val areGesturesEnabled by derivedStateOf { isRotationEnabled || isScrollingEnabled || isZoomingEnabled }
    internal var isRotationEnabled by mutableStateOf(false)
    internal var isScrollingEnabled by mutableStateOf(true)
    internal var isZoomingEnabled by mutableStateOf(true)
    internal var isFlingZoomEnabled by mutableStateOf(true)

    /* Only source of truth. Don't mutate directly, use appropriate setScale(), setRotation(), etc. */
    internal var scale by mutableFloatStateOf(scale)
    internal var rotation: AngleDegree by mutableFloatStateOf(rotation)
    internal var scrollX by mutableFloatStateOf(0f)
    internal var scrollY by mutableFloatStateOf(0f)

    internal var pivotX: Double by mutableDoubleStateOf(0.0)
    internal var pivotY: Double by mutableDoubleStateOf(0.0)

    internal var centroidX: Double by mutableDoubleStateOf(0.0)
    internal var centroidY: Double by mutableDoubleStateOf(0.0)

    internal var layoutSize by mutableStateOf(IntSize(0, 0))

    internal var visibleAreaPadding = VisibleAreaPadding(0, 0, 0, 0)

    internal var minScale by mutableFloatStateOf(0f)   // should only be changed through MinimumScaleMode

    var maxScale = maxScale
        set(value) {
            field = value
            setScale(scale)
        }

    internal var shouldLoopScale by mutableStateOf(false)

    internal var scrollOffsetRatio = Offset(0f, 0f)
        set(value) {
            if (value.x in 0f..1f && value.y in 0f..1f) {
                field = value
                /* Update the scroll to constrain it */
                setScroll(
                    scrollX = scrollX,
                    scrollY = scrollY
                )
            } else throw IllegalArgumentException("The offset ratio should have values in 0f..1f range")
        }

    // For user gestures animations
    private val userFloatAnimatable = Animatable(0f)
    private val userAnimatable: Animatable<Offset, AnimationVector2D> =
        Animatable(Offset.Zero, Offset.VectorConverter)

    // For api-based animations
    private val apiAnimatable = Animatable(0f)

    private val doubleTapSpec =
        TweenSpec<Float>(durationMillis = 300, easing = LinearOutSlowInEasing)
    private val flingZoomSpec =
        FloatExponentialDecaySpec(
            frictionMultiplier = gestureConfiguration.flingZoomFriction
        ).generateDecayAnimationSpec<Float>()

    @Suppress("unused")
    fun setScale(scale: Float, notify: Boolean = true) {
        this.scale = constrainScale(scale)
        updateCentroid()
        if (notify) notifyStateChanged()
    }

    @Suppress("unused")
    fun setScroll(scrollX: Float, scrollY: Float) {
        this.scrollX = constrainScrollX(scrollX)
        this.scrollY = constrainScrollY(scrollY)
        updateCentroid()
        notifyStateChanged()
    }

    @Suppress("unused")
    fun setRotation(angle: AngleDegree, notify: Boolean = true) {
        this.rotation = angle.modulo()
        updateCentroid()
        if (notify) notifyStateChanged()
    }

    /**
     * Scales the layout with animated scale, without maintaining scroll position.
     *
     * @param scale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    @Suppress("unused")
    suspend fun smoothScaleTo(
        scale: Float,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
    ): Boolean {
        return invokeAndCheckSuccess {
            val currScale = this@ZoomPanRotateState.scale
            if (currScale > 0) {
                apiAnimatable.snapTo(0f)
                apiAnimatable.animateTo(1f, animationSpec) {
                    setScale(lerp(currScale, scale, value))
                }
            }
        }
    }

    suspend fun smoothRotateTo(
        angle: AngleDegree,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        /* We don't have to stop scrolling animation while doing that */
        return invokeAndCheckSuccess {
            val currRotation = this@ZoomPanRotateState.rotation
            var targetAngle = (angle % 360)
            if (abs(targetAngle - currRotation) > 180) {
                targetAngle += if (targetAngle > currRotation) -360 else 360
            }
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setRotation(lerp(currRotation, targetAngle, value))
            }
        }
    }

    /**
     * Animates the scroll to the destination value.
     *
     * @return `true` if the operation completed without being cancelled.
     */
    suspend fun smoothScrollTo(
        destScrollX: Float,
        destScrollY: Float,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        val startScrollX = this.scrollX
        val startScrollY = this.scrollY

        return invokeAndCheckSuccess {
            userAnimatable.stop()
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setScroll(
                    scrollX = lerp(startScrollX, destScrollX, value),
                    scrollY = lerp(startScrollY, destScrollY, value)
                )
            }
        }
    }

    /**
     * Animates the scroll and the scale together with the supplied destination values.
     *
     * @param destScrollX Horizontal scroll of the destination point.
     * @param destScrollY Vertical scroll of the destination point.
     * @param destScale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    suspend fun smoothScrollScaleRotate(
        destScrollX: Float,
        destScrollY: Float,
        destScale: Float,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        val startScrollX = this.scrollX
        val startScrollY = this.scrollY
        val startScale = this.scale

        return invokeAndCheckSuccess {
            userAnimatable.stop()
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setScale(lerp(startScale, destScale, value))
                setScroll(
                    scrollX = lerp(startScrollX, destScrollX, value),
                    scrollY = lerp(startScrollY, destScrollY, value)
                )
            }
        }
    }

    /**
     * Animates the scroll, the scale, and the rotation together with the supplied destination values.
     *
     * @param destScrollX Horizontal scroll of the destination point.
     * @param destScrollY Vertical scroll of the destination point.
     * @param destScale The final scale value the layout should animate to.
     * @param destAngle The final angle in decimal degrees the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    suspend fun smoothScrollScaleRotate(
        destScrollX: Float,
        destScrollY: Float,
        destScale: Float,
        destAngle: AngleDegree,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        val startScrollX = this.scrollX
        val startScrollY = this.scrollY
        val startScale = this.scale

        val currRotation = this@ZoomPanRotateState.rotation
        var targetAngle = (destAngle % 360)
        if (abs(targetAngle - currRotation) > 180) {
            targetAngle += if (targetAngle > currRotation) -360 else 360
        }

        return invokeAndCheckSuccess {
            userAnimatable.stop()
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setScale(lerp(startScale, destScale, value))
                setScroll(
                    scrollX = lerp(startScrollX, destScrollX, value),
                    scrollY = lerp(startScrollY, destScrollY, value)
                )
                setRotation(lerp(currRotation, targetAngle, value))
            }
        }
    }

    /**
     * Animates the layout to the scale provided, while maintaining position determined by the
     * the provided focal point.
     *
     * @param focusX The horizontal focal point to maintain, relative to the layout.
     * @param focusY The vertical focal point to maintain, relative to the layout.
     * @param destScale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    suspend fun smoothScaleWithFocalPoint(
        focusX: Float,
        focusY: Float,
        destScale: Float,
        animationSpec: AnimationSpec<Float>
    ): Boolean {
        val destScaleCst = constrainScale(destScale)
        val startScale = scale
        if (startScale == destScale) return true
        val startScrollX = scrollX
        val startScrollY = scrollY
        val destScrollX = getScrollAtOffsetAndScale(startScrollX, focusX, destScaleCst / startScale)
        val destScrollY = getScrollAtOffsetAndScale(startScrollY, focusY, destScaleCst / startScale)

        return smoothScrollScaleRotate(destScrollX, destScrollY, destScale, animationSpec)
    }

    /**
     * Invoke [block] and return whether the operation completed without being cancelled.
     */
    private suspend fun invokeAndCheckSuccess(block: suspend () -> Unit): Boolean {
        var success = true
        scope?.launch {
            block()
        }?.also {
            it.invokeOnCompletion { t ->
                if (t != null) success = false
            }
        }?.join()

        return success
    }

    suspend fun stopAnimations() {
        apiAnimatable.stop()
        userAnimatable.stop()
        userFloatAnimatable.stop()
    }

    override fun onScaleRatio(scaleRatio: Float, centroid: Offset) {
        if (!isZoomingEnabled) return

        val formerScale = scale
        setScale(scale * scaleRatio)

        /* Pinch and zoom magic */
        val effectiveScaleRatio = scale / formerScale
        val angleRad = -rotation.toRad()
        val centroidRotated = rotateFocalPoint(centroid, angleRad)
        setScroll(
            scrollX = getScrollAtOffsetAndScale(scrollX, centroidRotated.x, effectiveScaleRatio),
            scrollY = getScrollAtOffsetAndScale(scrollY, centroidRotated.y, effectiveScaleRatio)
        )
    }

    private fun getScrollAtOffsetAndScale(scroll: Float, offSet: Float, scaleRatio: Float): Float {
        return (scroll + offSet) * scaleRatio - offSet
    }

    /**
     * Rotates a focal point around the center of the layout.
     */
    private fun rotateFocalPoint(point: Offset, angleRad: AngleRad): Offset {
        val x = if (angleRad == 0f) point.x else {
            layoutSize.height / 2 * sin(angleRad) + layoutSize.width / 2 * (1 - cos(angleRad)) +
                    point.x * cos(angleRad) - point.y * sin(angleRad)
        }

        val y = if (angleRad == 0f) point.y else {
            layoutSize.height / 2 * (1 - cos(angleRad)) - layoutSize.width / 2 * sin(angleRad) +
                    point.x * sin(angleRad) + point.y * cos(angleRad)
        }
        return Offset(x, y)
    }

    override fun onRotationDelta(rotationDelta: Float) {
        if (!isRotationEnabled) return

        setRotation(rotation + rotationDelta)
    }

    override fun onScrollDelta(scrollDelta: Offset) {
        if (!isScrollingEnabled) return

        var scrollX = scrollX
        var scrollY = scrollY

        val rotRad = -rotation.toRad()
        scrollX -= if (rotRad == 0f) scrollDelta.x else {
            scrollDelta.x * cos(rotRad) - scrollDelta.y * sin(rotRad)
        }
        scrollY -= if (rotRad == 0f) scrollDelta.y else {
            scrollDelta.x * sin(rotRad) + scrollDelta.y * cos(rotRad)
        }
        setScroll(scrollX, scrollY)
    }

    override fun onFling(flingSpec: DecayAnimationSpec<Offset>, velocity: Velocity) {
        if (!isScrollingEnabled) return

        val rotRad = -rotation.toRad()
        val velocityX = if (rotRad == 0f) velocity.x else {
            velocity.x * cos(rotRad) - velocity.y * sin(rotRad)
        }
        val velocityY = if (rotRad == 0f) velocity.y else {
            velocity.x * sin(rotRad) + velocity.y * cos(rotRad)
        }

        scope?.launch {
            userAnimatable.snapTo(Offset(scrollX, scrollY))
            userAnimatable.animateDecay(
                initialVelocity = -Offset(velocityX, velocityY),
                animationSpec = flingSpec,
            ) {
                setScroll(
                    scrollX = value.x,
                    scrollY = value.y
                )
            }
        }
    }

    override fun onFlingZoom(velocity: Float, centroid: Offset) {
        if (!isZoomingEnabled || !isFlingZoomEnabled) return

        scope?.launch {
            userFloatAnimatable.snapTo(scale)
            userFloatAnimatable.animateDecay(
                initialVelocity = velocity,
                animationSpec = flingZoomSpec,
            ) {
                onScaleRatio(value / scale, centroid)
            }
        }
    }

    override fun onTouchDown() {
        if (!areGesturesEnabled) return

        scope?.launch {
            stopAnimations()
        }
        stateChangeListener.onTouchDown()
    }

    override fun onPress() {
        stateChangeListener.onPress()
    }

    override fun onTap(focalPt: Offset) {
        if (!stateChangeListener.detectsTap()) return
        offsetToRelative(focalPt) { x, y ->
            stateChangeListener.onTap(x, y)
        }
    }

    override fun onLongPress(focalPt: Offset) {
        if (!stateChangeListener.detectsLongPress()) return
        offsetToRelative(focalPt) { x, y ->
            stateChangeListener.onLongPress(x, y)
        }
    }

    private fun <T> offsetToRelative(focalPt: Offset, block: (Double, Double) -> T): T {
        val angleRad = -rotation.toRad()
        val focalPtRotated = rotateFocalPoint(focalPt, angleRad)
        val x = (scrollX + focalPtRotated.x).toDouble() / (scale * fullWidth)
        val y = (scrollY + focalPtRotated.y).toDouble() / (scale * fullHeight)
        return block(x, y)
    }

    private fun <T> relativeToMarkerLayoutCoords(x: Double, y: Double, block: (Int, Int) -> T): T {
        val xFullPx = x * fullWidth * scale
        val yFullPx = y * fullHeight * scale
        val centerX = centroidX * fullWidth * scale
        val centerY = centroidY * fullHeight * scale

        val angleRad = rotation.toRad()
        val xPx = (rotateCenteredX(
            xFullPx,
            yFullPx,
            centerX,
            centerY,
            angleRad
        )).toInt()

        val yPx = (rotateCenteredY(
            xFullPx,
            yFullPx,
            centerX,
            centerY,
            angleRad
        )).toInt()

        return block(xPx, yPx)
    }

    override fun onDoubleTap(focalPt: Offset) {
        if (!isZoomingEnabled) return

        val destScale = (
                2.0.pow(floor(ln((scale * 2).toDouble()) / ln(2.0))).toFloat()
                ).let {
                if (shouldLoopScale && it > maxScale) minScale else it
            }

        val angleRad = -rotation.toRad()
        val focalPtRotated = rotateFocalPoint(focalPt, angleRad)

        scope?.launch {
            smoothScaleWithFocalPoint(
                focalPtRotated.x,
                focalPtRotated.y,
                destScale,
                doubleTapSpec
            )
        }
    }

    override fun onTwoFingersTap(focalPt: Offset) {
        if (!isZoomingEnabled) return

        val destScale = 2.0.pow(floor(ln((scale / 2).toDouble()) / ln(2.0))).toFloat()

        val angleRad = -rotation.toRad()
        val focalPtRotated = rotateFocalPoint(focalPt, angleRad)

        scope?.launch {
            smoothScaleWithFocalPoint(
                focalPtRotated.x,
                focalPtRotated.y,
                destScale,
                doubleTapSpec
            )
        }
    }

    override fun isListeningForGestures(): Boolean = areGesturesEnabled

    override fun shouldConsumeTapGesture(focalPt: Offset): Boolean {
        return offsetToRelative(focalPt) { x, y ->
            relativeToMarkerLayoutCoords(x, y) { xPx, yPx ->
                stateChangeListener.interceptsTap(x, y, xPx, yPx)
            }
        }
    }

    override fun shouldConsumeLongPress(focalPt: Offset): Boolean {
        return offsetToRelative(focalPt) { x, y ->
            relativeToMarkerLayoutCoords(x, y) { xPx, yPx ->
                stateChangeListener.interceptsLongPress(x, y, xPx, yPx)
            }
        }
    }

    override fun onSizeChanged(composableScope: CoroutineScope, size: IntSize) {
        scope = composableScope

        /* When the size changes, typically on device rotation, the scroll needs to be adapted so
         * that we keep the same location at the center of the screen. Don't do that when layout
         * hasn't been done yet. */
        if (layoutSize != IntSize.Zero) {
            setScroll(
                scrollX = scrollX + (layoutSize.width - size.width) / 2,
                scrollY = scrollY + (layoutSize.height - size.height) / 2
            )
        }

        layoutSize = size
        recalculateMinScale()
        setScale(scale)

        /* Layout was done at least once, resume continuations */
        for (ct in onLayoutContinuations) {
            ct.resume(Unit)
        }
        onLayoutContinuations.clear()
    }

    private fun constrainScrollX(scrollX: Float): Float {
        val angle = rotation.toRad()

        val layoutDimension =
            polarRadius(layoutSize.width.toFloat(), layoutSize.height.toFloat(), angle)
        val bias = (layoutDimension - layoutSize.width) / 2

        return if (fullWidth * scale < layoutDimension) {
            val offset = scrollOffsetRatio.x * fullWidth * scale
            scrollX.coerceIn(fullWidth * scale - layoutDimension - offset + bias, offset + bias)
        } else {
            val offset = scrollOffsetRatio.x * layoutDimension
            scrollX.coerceIn(
                -offset + bias,
                offset + bias + fullWidth * scale - layoutDimension
            )
        }
    }

    private fun constrainScrollY(scrollY: Float): Float {
        val angle = rotation.toRad()

        val layoutDimension =
            polarRadius(layoutSize.height.toFloat(), layoutSize.width.toFloat(), angle)
        val bias = (layoutDimension - layoutSize.height) / 2

        return if (fullHeight * scale < layoutDimension) {
            val offset = scrollOffsetRatio.y * fullHeight * scale
            scrollY.coerceIn(fullHeight * scale - layoutDimension - offset + bias, offset + bias)
        } else {
            val offset = scrollOffsetRatio.y * layoutDimension
            scrollY.coerceIn(
                -offset + bias,
                offset + bias + fullHeight * scale - layoutDimension
            )
        }
    }

    internal fun constrainScale(scale: Float): Float {
        return scale.coerceIn(max(minScale, Float.MIN_VALUE), maxScale.coerceAtLeast(minScale))
    }

    private fun updateCentroid() {
        pivotX = layoutSize.width.toDouble() / 2
        pivotY = layoutSize.height.toDouble() / 2

        centroidX = (scrollX + pivotX) / (fullWidth * scale)
        centroidY = (scrollY + pivotY) / (fullHeight * scale)
    }

    private fun recalculateMinScale() {
        val minScaleX = layoutSize.width.toFloat() / fullWidth
        val minScaleY = layoutSize.height.toFloat() / fullHeight
        val mode = minimumScaleMode
        minScale = when (mode) {
            Fit -> min(minScaleX, minScaleY)
            Fill -> max(minScaleX, minScaleY)
            is Forced -> mode.scale
        }
        setScale(scale)
    }

    private fun notifyStateChanged() {
        if (layoutSize != IntSize.Zero) {
            stateChangeListener.onStateChanged()
        }
    }

    private fun polarRadius(a: Float, b: Float, angle: AngleRad): Float {
        return a * b / sqrt((a * sin(angle)).pow(2) + (b * cos(angle)).pow(2))
    }
}

/**
 * The padding to apply when some UI is obscuring the map on it's borders.
 */
internal data class VisibleAreaPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

interface ZoomPanRotateStateListener {
    fun onStateChanged()
    fun onTouchDown()
    fun onPress()
    fun onLongPress(x: Double, y: Double)
    fun onTap(x: Double, y: Double)
    fun detectsTap(): Boolean
    fun detectsLongPress(): Boolean
    fun interceptsTap(x: Double, y: Double, xPx: Int, yPx: Int): Boolean
    fun interceptsLongPress(x: Double, y: Double, xPx: Int, yPx: Int): Boolean
}