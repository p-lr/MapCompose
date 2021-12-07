package ovh.plrapps.mapcompose.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ovh.plrapps.mapcompose.testapp.core.ui.MapComposeTestApp
import ovh.plrapps.mapcompose.testapp.core.ui.theme.MapComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapComposeTheme {
                MapComposeTestApp()
            }
        }
    }
}