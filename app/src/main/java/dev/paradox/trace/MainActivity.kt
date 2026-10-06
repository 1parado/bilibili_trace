package dev.paradox.trace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.paradox.trace.data.remote.BilibiliViewParser
import dev.paradox.trace.ui.TraceApp
import dev.paradox.trace.ui.theme.TraceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraceTheme {
                TraceApp(sharedBvid = extractSharedBvid(intent))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent {
            TraceTheme {
                TraceApp(sharedBvid = extractSharedBvid(intent))
            }
        }
    }

    private fun extractSharedBvid(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return BilibiliViewParser.normalizeBvid(text)
    }
}
