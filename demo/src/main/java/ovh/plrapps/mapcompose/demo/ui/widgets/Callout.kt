package ovh.plrapps.mapcompose.demo.ui.widgets

import android.animation.TimeInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * A callout which animates its entry with an overshoot scaling interpolator.
 */
@Composable
fun Callout(
    x: Double, y: Double,
    title: String,
    shouldAnimate: Boolean,
    onAnimationDone: () -> Unit
) {
    var animVal by remember { mutableStateOf(if (shouldAnimate) 0f else 1f) }
    LaunchedEffect(true) {
        if (shouldAnimate) {
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = tween(250, easing = overshootEasing)
            ) {
                animVal = value
            }
            onAnimationDone()
        }
    }
    Surface(
        Modifier
            .alpha(animVal)
            .padding(10.dp)
            .graphicsLayer {
                scaleX = animVal
                scaleY = animVal
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        shape = RoundedCornerShape(5.dp),
        elevation = 10.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "position ${df.format(x)} , ${df.format(y)}",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

private val df = DecimalFormat("#.##").apply {
    roundingMode = RoundingMode.CEILING
}

private val overshootEasing = OvershootInterpolator(1.2f).toEasing()

private fun TimeInterpolator.toEasing() = Easing { x ->
    getInterpolation(x)
}
