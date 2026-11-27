package com.android.picsearch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.picsearch.ui.theme.PicSearchTheme

private const val TAG = "PicSearch"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null && intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val textUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!textUrl.isNullOrBlank()) {
                val engine = SearchEngine.fromIntent(intent)
                launchCustomTab(this, engine.buildUrl(textUrl))

                finish()
                return
            }
        }

        if (savedInstanceState == null) {
            viewModel.handleIntent(intent)
        }

        setContent {
            PicSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    MainScreen(
                        uiState = uiState,
                        onFinishApp = { finish() }
                    )
                }
            }
        }
    }
}

fun launchCustomTab(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No browser available", e)
        Toast.makeText(context, R.string.error_no_browser, Toast.LENGTH_LONG).show()
    }
}