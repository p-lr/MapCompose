package ovh.plrapps.mapcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ovh.plrapps.mapcompose.ui.MapComposeDemoApp
import ovh.plrapps.mapcompose.ui.theme.MapComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapComposeTheme {
                MapComposeDemoApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MapComposeTheme {
        MapComposeDemoApp()
    }
}