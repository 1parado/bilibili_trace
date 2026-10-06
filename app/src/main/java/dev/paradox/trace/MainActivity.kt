package dev.paradox.trace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.paradox.trace.ui.TraceApp
import dev.paradox.trace.ui.theme.TraceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraceTheme {
                TraceApp()
            }
        }
    }
}
