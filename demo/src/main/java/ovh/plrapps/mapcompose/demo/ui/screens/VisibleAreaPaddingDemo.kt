package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.mapcompose.api.centerOnMarker
import ovh.plrapps.mapcompose.api.setVisibleAreaPadding
import ovh.plrapps.mapcompose.demo.viewmodels.VisibleAreaPaddingVM
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun VisibleAreaPaddingDemo(
    modifier: Modifier = Modifier, viewModel: VisibleAreaPaddingVM = viewModel()
) {
    val obstructionSize = 100.dp
    val obstructionColor = Color(0xA0000000)
    var leftObstructionEnabled by remember { mutableStateOf(true) }
    var rightObstructionEnabled by remember { mutableStateOf(false) }
    var topObstructionEnabled by remember { mutableStateOf(false) }
    var bottomObstructionEnabled by remember { mutableStateOf(false) }

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(leftObstructionEnabled, onCheckedChange = { leftObstructionEnabled = !leftObstructionEnabled })
                Text("Left")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(rightObstructionEnabled, onCheckedChange = { rightObstructionEnabled = !rightObstructionEnabled })
                Text("Right")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(topObstructionEnabled, onCheckedChange = { topObstructionEnabled = !topObstructionEnabled })
                Text("Top")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(bottomObstructionEnabled, onCheckedChange = { bottomObstructionEnabled = !bottomObstructionEnabled })
                Text("Bottom")
            }
        }
        Box {
            MapUI(
                modifier,
                state = viewModel.state
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = leftObstructionEnabled,
                enter = expandHorizontally(),
                exit = shrinkHorizontally(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Surface(
                    color = obstructionColor,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(obstructionSize)
                ) {}
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = rightObstructionEnabled,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    color = obstructionColor,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(obstructionSize)
                ) {}
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = topObstructionEnabled,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = obstructionColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(obstructionSize)
                ) {}
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = bottomObstructionEnabled,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = obstructionColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(obstructionSize)
                ) {}
            }
        }
    }

    LaunchedEffect(leftObstructionEnabled, rightObstructionEnabled, topObstructionEnabled, bottomObstructionEnabled) {
        viewModel.state.setVisibleAreaPadding(
            left = if (leftObstructionEnabled) obstructionSize else 0.dp,
            right = if (rightObstructionEnabled) obstructionSize else 0.dp,
            top = if (topObstructionEnabled) obstructionSize else 0.dp,
            bottom = if (bottomObstructionEnabled) obstructionSize else 0.dp
        )
        viewModel.state.centerOnMarker("m0")
    }
}
