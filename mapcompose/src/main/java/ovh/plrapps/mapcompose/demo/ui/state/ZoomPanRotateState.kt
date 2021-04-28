package ovh.plrapps.mapcompose.demo.ui.state

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.Fill
import ovh.plrapps.mapcompose.api.Fit
import ovh.plrapps.mapcompose.api.Forced
import ovh.plrapps.mapcompose.api.MinimumScaleMode
import ovh.plrapps.mapcompose.demo.ui.layout.GestureListener
import ovh.plrapps.mapcompose.demo.ui.layout.LayoutSizeChangeListener
import ovh.plrapps.mapcompose.utils.*
import kotlin.math.*

internal class ZoomPanRotateState(
    val fullWidth: Int,
    val fullHeight: Int,
    private val stateChangeListener: ZoomPanRotateStateListener
) : GestureListener, LayoutSizeChangeListener {
    private var scope: CoroutineScope? = null

    private val minimumScaleMode: MinimumScaleMode = Fit
    internal var isRotationEnabled = false

    /* Only source of truth. Don't mutate directly, use appropriate setScale(), setRotation(), etc. */
    internal var scale by mutableStateOf(1f)
    internal var rotation: AngleDegree by mutableStateOf(0f)
    internal var scrollX by mutableStateOf(0f)
    internal var scrollY by mutableStateOf(0f)

    internal var centroidX: Double by mutableStateOf(0.0)
    internal var centroidY: Double by mutableStateOf(0.0)

    internal var layoutSize by mutableStateOf(IntSize(0, 0))
    var minScale = 0f
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

    /* Used for fling animation */
    private val scrollAnimatable: Animatable<Offset, AnimationVector2D> =
        Animatable(Offset.Zero, Offset.VectorConverter)

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
    fun smoothScaleTo(
        scale: Float,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
    ) {
        scope?.launch {
            val currScale = this@ZoomPanRotateState.scale
            if (currScale > 0) {
                Animatable(0f).animateTo(1f, animationSpec) {
                    setScale(lerp(currScale, scale, value))
                }
            }
        }
    }

    fun smoothRotateTo(
        angle: AngleDegree,
        animationSpec: AnimationSpec<Float>
    ) {
        /* We don't have to stop scrolling animation while doing that */
        scope?.launch {
            val currRotation = this@ZoomPanRotateState.rotation
            Animatable(0f).animateTo(1f, animationSpec) {
                setRotation(lerp(currRotation, angle, value))
            }
        }
    }

    /**
     * Animates the scroll to the destination value.
     */
    fun smoothScrollTo(
        destScrollX: Float,
        destScrollY: Float,
        animationSpec: AnimationSpec<Float>
    ) {
        val startScrollX = this.scrollX
        val startScrollY = this.scrollY

        scope?.launch {
            scrollAnimatable.stop()
            Animatable(0f).animateTo(1f, animationSpec) {
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
    fun smoothScrollAndScale(
        destScrollX: Float,
        destScrollY: Float,
        destScale: Float,
        animationSpec: AnimationSpec<Float>
    ) {
        val startScrollX = this.scrollX
        val startScrollY = this.scrollY
        val startScale = this.scale

        scope?.launch {
            scrollAnimatable.stop()
            Animatable(0f).animateTo(1f, animationSpec) {
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
    fun smoothScaleWithFocalPoint(
        focusX: Float,
        focusY: Float,
        destScale: Float,
        animationSpec: AnimationSpec<Float>
    ) {
        val destScaleCst = constrainScale(destScale)
        val startScale = scale
        if (startScale == destScale) return
        val startScrollX = scrollX
        val startScrollY = scrollY
        val destScrollX = getScrollAtOffsetAndScale(startScrollX, focusX, destScaleCst / startScale)
        val destScrollY = getScrollAtOffsetAndScale(startScrollY, focusY, destScaleCst / startScale)

        smoothScrollAndScale(destScrollX, destScrollY, destScale, animationSpec)
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
            scrollAnimatable.snapTo(Offset(scrollX, scrollY))
            scrollAnimatable.animateDecay(
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

    override fun onTap() {
        scope?.launch {
            scrollAnimatable.stop()
        }
    }

    override fun onDoubleTap(focalPt: Offset) {
        val destScale = (
                2.0.pow(floor(ln((scale * 2).toDouble()) / ln(2.0))).toFloat()
                ).let {
                if (shouldLoopScale && it > maxScale) minScale else it
            }

        val angleRad = -rotation.toRad()
        val focalPtRotated = rotateFocalPoint(focalPt, angleRad)

        smoothScaleWithFocalPoint(
            focalPtRotated.x,
            focalPtRotated.y,
            destScale,
            doubleTapSpec
        )
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
    }

    private fun constrainScrollX(scrollX: Float): Float {
        val angleRad = rotation.toRad() % Math.PI
        val offsetRight = when (angleRad) {
            in 0.0..Math.PI / 4 -> (1 - 2 * angleRad / Math.PI) * layoutSize.width
            in Math.PI / 4..Math.PI / 2 -> 2 * layoutSize.height * angleRad / Math.PI + (layoutSize.width - layoutSize.height) / 2
            in Math.PI / 2..3 * Math.PI / 4 -> -2 * layoutSize.height * angleRad / Math.PI + (3 * layoutSize.height + layoutSize.width) / 2
            else -> (2 * angleRad / Math.PI - 1) * layoutSize.width
        }

        val offsetLeft = layoutSize.width - offsetRight.toFloat()

        val limitLeft = -offsetLeft
        val limitRight = max(0f, fullWidth * scale - offsetRight.toFloat())
        val limitMin = min(limitLeft, limitRight)
        val limitMax = max(limitLeft, limitRight)

        return scrollX.coerceIn(limitMin, limitMax)
    }

    private fun constrainScrollY(scrollY: Float): Float {
        val angleRad = rotation.toRad() % Math.PI
        val offsetBottom = when (angleRad) {
            in 0.0..Math.PI / 4 -> (1 - 2 * angleRad / Math.PI) * layoutSize.height
            in Math.PI / 4..Math.PI / 2 -> 2 * layoutSize.width * angleRad / Math.PI + (layoutSize.height - layoutSize.width) / 2
            in Math.PI / 2..3 * Math.PI / 4 -> -2 * layoutSize.width * angleRad / Math.PI + (3 * layoutSize.width + layoutSize.height) / 2
            else -> (2 * angleRad / Math.PI - 1) * layoutSize.height
        }

        val offsetTop = layoutSize.height - offsetBottom.toFloat()

        val limitBottom = max(0f, fullHeight * scale - offsetBottom.toFloat())
        val limitTop = -offsetTop

        val limitMin = min(limitTop, limitBottom)
        val limitMax = max(limitTop, limitBottom)

        return scrollY.coerceIn(limitMin, limitMax)
    }

    private fun constrainScale(scale: Float): Float {
        return scale.coerceIn(max(minScale, Float.MIN_VALUE), maxScale)  // scale between 0+ and 2f
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
        minScale = when (minimumScaleMode) {
            Fit -> min(minScaleX, minScaleY)
            Fill -> max(minScaleX, minScaleY)
            is Forced -> minimumScaleMode.scale
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
}