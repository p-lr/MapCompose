@file:OptIn(ExperimentalMaterial3Api::class)

package ovh.plrapps.mapcompose.demo.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ovh.plrapps.mapcompose.demo.R
import ovh.plrapps.mapcompose.demo.ui.MainDestinations

@Composable
fun Home(demoListState: LazyListState, onDemoSelected: (dest: MainDestinations) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxWidth(),
            state = demoListState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MainDestinations.entries.map { dest ->
                item {
                    Button(
                        onClick = { onDemoSelected.invoke(dest) }
                    ) {
                        Text(text = dest.title)
                    }
                }
            }
        }
    }
}