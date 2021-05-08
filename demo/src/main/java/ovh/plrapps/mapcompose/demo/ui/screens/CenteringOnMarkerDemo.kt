package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.demo.viewmodels.CenteringOnMarkerVM
import ovh.plrapps.mapcompose.ui.MapUI
import java.math.RoundingMode
import java.text.DecimalFormat

@Composable
fun CenteringOnMarkerDemo(
    modifier: Modifier = Modifier,
    viewModel: CenteringOnMarkerVM,
    onCenter: () -> Unit
) {
    /* Add a callout on marker click */
    viewModel.state.apply {
        onMarkerClick { id, x, y ->
            addCallout(id, x, y, absoluteOffset = Offset(0f, -150f)) {
                Callout(x, y)
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        MapUI(
            modifier.weight(2f),
            state = viewModel.state
        )
        Button(onClick = {
            onCenter()
        }, Modifier.padding(8.dp)) {
            Text(text = "Center on marker")
        }
    }
}

@Composable
fun Callout(x: Double, y: Double) {
    Surface(
        Modifier
            .clip(RoundedCornerShape(5.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Title",
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
                    .padding(4.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

val df = DecimalFormat("#.##").apply {
    roundingMode = RoundingMode.CEILING
}