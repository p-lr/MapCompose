package ovh.plrapps.mapcompose.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import ovh.plrapps.mapview.compose.ui.state.ZoomPanRotateState

class MapComposeState(fullWidth: Int, fullHeight: Int) {
    internal val zoomPanRotateState = ZoomPanRotateState(fullWidth, fullHeight)

    internal val childComposables = mutableStateMapOf<Int, @Composable () -> Unit>()

    @Suppress("unused")
    fun addComposable(id: Int, c: @Composable () -> Unit) {
        childComposables[id] = c
    }

    @Suppress("unused")
    fun removeComposable(id: Int): Boolean {
        return childComposables.remove(id) != null
    }
}