package ovh.plrapps.mapcompose.ui.state

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.*
import ovh.plrapps.mapcompose.ui.layout.*
import ovh.plrapps.mapcompose.utils.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.*

internal class ZoomPanRotateState(
    val fullWidth: Int,
    val fullHeight: Int,
    private val stateChangeListener: ZoomPanRotateStateListener
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
        suspendCoroutine<Unit> {
            onLayoutContinuations.add(it)
        }
    }

    internal var minimumScaleMode: MinimumScaleMode = Fit
        set(value) {
            field = value
            recalculateMinScale()
        }

    internal var isRotationEnabled = false

    /* Only source of truth. Don't mutate directly, use appropriate setScale(), setRotation(), etc. */
    internal var scale by mutableStateOf(1f)
    internal var rotation: AngleDegree by mutableStateOf(0f)
    internal var scrollX by mutableStateOf(0f)
    internal var scrollY by mutableStateOf(0f)

    internal var centroidX: Double by mutableStateOf(0.0)
    internal var centroidY: Double by mutableStateOf(0.0)

    internal var layoutSize by mutableStateOf(IntSize(0, 0))

    private var minScale = 0f   // should only be changed through MinimumScaleMode
        set(value) {
            field = value
            setScale(scale)
        }

    var maxScale = 2f
        set(value) {
            field = value
            setScale(scale)
        }

    var shouldLoopScale = false

    /**
     * When scaled out beyond the scaled permitted by [Fill], the padding is used by the layout.
     */
    internal var padding: IntOffset by mutableStateOf(IntOffset.Zero)

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

    /* Not used internally (user customizable) */
    internal var tapCb: LayoutTapCb? = null

    private val userAnimatable: Animatable<Offset, AnimationVector2D> =
        Animatable(Offset.Zero, Offset.VectorConverter)
    private val apiAnimatable = Animatable(0f)

    private val doubleTapSpec =
        TweenSpec<Float>(durationMillis = 300, easing = LinearOutSlowInEasing)
    private val flingSpec =
        SplineBasedFloatDecayAnimationSpec(Density(2f)).generateDecayAnimationSpec<Offset>()

    @Suppress("unused")
    fun setScale(scale: Float) {
        this.scale = constrainScale(scale)
        updatePadding()
        updateCentroid()
        stateChangeListener.onStateChanged()
    }

    @Suppress("unused")
    fun setScroll(scrollX: Float, scrollY: Float) {
        this.scrollX = constrainScrollX(scrollX)
        this.scrollY = constrainScrollY(scrollY)
        updateCentroid()
        stateChangeListener.onStateChanged()
    }

    @Suppress("unused")
    fun setRotation(angle: AngleDegree) {
        this.rotation = angle.modulo()
        updateCentroid()
        stateChangeListener.onStateChanged()
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
            apiAnimatable.snapTo(0f)
            apiAnimatable.animateTo(1f, animationSpec) {
                setRotation(lerp(currRotation, angle, value))
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
    suspend fun smoothScrollAndScale(
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
    ) : Boolean {
        val destScaleCst = constrainScale(destScale)
        val startScale = scale
        if (startScale == destScale) return true
        val startScrollX = scrollX
        val startScrollY = scrollY
        val destScrollX = getScrollAtOffsetAndScale(startScrollX, focusX, destScaleCst / startScale)
        val destScrollY = getScrollAtOffsetAndScale(startScrollY, focusY, destScaleCst / startScale)

        return smoothScrollAndScale(destScrollX, destScrollY, destScale, animationSpec)
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
    }

    override fun onScaleRatio(scaleRatio: Float, centroid: Offset) {
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
        if (isRotationEnabled) {
            setRotation(rotation + rotationDelta)
        }
    }

    override fun onScrollDelta(scrollDelta: Offset) {
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

    override fun onFling(velocity: Velocity) {
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

    override fun onTouchDown() {
        scope?.launch {
            stopAnimations()
        }
        stateChangeListener.onTouchDown()
    }

    override fun onTap(focalPt: Offset) {
        val tapCb = tapCb ?: return
        val angleRad = -rotation.toRad()
        val focalPtRotated = rotateFocalPoint(focalPt, angleRad)
        val x = (scrollX - padding.x + focalPtRotated.x).toDouble() / (scale * fullWidth)
        val y = (scrollY - padding.y + focalPtRotated.y).toDouble() / (scale * fullHeight)
        tapCb.invoke(x, y)
    }

    override fun onDoubleTap(focalPt: Offset) {
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

    override fun onSizeChanged(composableScope: CoroutineScope, size: IntSize) {
        scope = composableScope

        /* When the size changes, typically on device rotation, the scroll needs to be adapted so
         * that we keep the same location at the center of the screen. */
        setScroll(
            scrollX = scrollX + (layoutSize.width - size.width) / 2,
            scrollY = scrollY + (layoutSize.height - size.height) / 2
        )

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
        val offset = scrollOffsetRatio.x * layoutSize.width
        return scrollX.coerceIn(-offset, max(0f, fullWidth * scale - layoutSize.width + offset))
    }

    private fun constrainScrollY(scrollY: Float): Float {
        val offset = scrollOffsetRatio.y * layoutSize.width
        return scrollY.coerceIn(-offset, max(0f, fullHeight * scale - layoutSize.height + offset))
    }

    internal fun constrainScale(scale: Float): Float {
        return scale.coerceIn(max(minScale, Float.MIN_VALUE), maxScale.coerceAtLeast(minScale))
    }

    private fun updateCentroid() {
        centroidX = (scrollX + min(
            layoutSize.width.toDouble() / 2,
            fullWidth * scale.toDouble() / 2
        )) / (fullWidth * scale)
        centroidY = (scrollY + min(
            layoutSize.height.toDouble() / 2,
            fullHeight * scale.toDouble() / 2
        )) / (fullHeight * scale)
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
    }

    private fun updatePadding() {
        val paddingX = if (fullWidth * scale >= layoutSize.width) {
            0
        } else {
            layoutSize.width / 2 - (fullWidth * scale).roundToInt() / 2
        }

        val paddingY = if (fullHeight * scale >= layoutSize.height) {
            0
        } else {
            layoutSize.height / 2 - (fullHeight * scale).roundToInt() / 2
        }
        padding = IntOffset(paddingX, paddingY)
    }
}

interface ZoomPanRotateStateListener {
    fun onStateChanged()
    fun onTouchDown()
}

internal typealias LayoutTapCb = (x: Double, y: Double) -> Unit