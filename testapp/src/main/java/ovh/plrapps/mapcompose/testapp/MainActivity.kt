package ovh.plrapps.mapcompose.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ovh.plrapps.mapcompose.testapp.core.ui.MapComposeTestApp
import ovh.plrapps.mapcompose.testapp.core.ui.theme.MapComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MapComposeTheme {
                MapComposeTestApp()
            }
        }
    }
}