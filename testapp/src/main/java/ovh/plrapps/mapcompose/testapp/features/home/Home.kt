package ovh.plrapps.mapcompose.testapp.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ovh.plrapps.mapcompose.testapp.R
import ovh.plrapps.mapcompose.testapp.core.ui.nav.NavDestinations

@Composable
fun Home(demoListState: LazyListState, onDemoSelected: (dest: NavDestinations) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                backgroundColor = MaterialTheme.colors.primarySurface,
            )
        }
    ) {
        LazyColumn(state = demoListState) {
            NavDestinations.values().map { dest ->
                item {
                    Text(
                        text = stringResource(dest.title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDemoSelected.invoke(dest) }
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Divider(thickness = 1.dp)
                }
            }
        }
    }
}