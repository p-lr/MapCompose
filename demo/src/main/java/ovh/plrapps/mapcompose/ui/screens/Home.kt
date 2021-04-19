package ovh.plrapps.mapcompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ovh.plrapps.mapcompose.R
import ovh.plrapps.mapcompose.ui.MainDestinations

@Composable
fun Home(demoListState: LazyListState, onDemoSelected: (dest: MainDestinations) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                backgroundColor = MaterialTheme.colors.primarySurface,
            )
        }
    ) {
        LazyColumn(state = demoListState) {
            MainDestinations.values().filterNot { it == MainDestinations.HOME }.map { dest ->
                item {
                    Text(
                        text = dest.name,
                        modifier = Modifier
                            .wrapContentSize(Alignment.Center)
                            .clickable { onDemoSelected.invoke(dest) }
                    )
                }
            }
        }
    }
}