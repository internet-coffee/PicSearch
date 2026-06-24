package com.android.picsearch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.android.picsearch.ui.theme.PicSearchTheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null && intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val textUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!textUrl.isNullOrBlank()) {
                val encodedUrl = URLEncoder.encode(textUrl, StandardCharsets.UTF_8.toString())
                val finalUrl = "https://lens.google.com/uploadbyurl?url=$encodedUrl"

                launchCustomTab(this, finalUrl)

                finish()
                return
            }
        }

        if (savedInstanceState == null) {
            viewModel.handleIntent(intent, contentResolver)
        }

        setContent {
            PicSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    MainScreen(
                        uiState = uiState,
                        onFinishApp = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    uiState: UiState,
    onFinishApp: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is UiState.Idle -> {}
            is UiState.Error -> Text(text = "Error: ${uiState.message}")
            is UiState.Loading, is UiState.Success -> {
                CircularProgressIndicator()

                if (uiState is UiState.Success) {
                    LaunchedEffect(uiState.url) {
                        launchCustomTab(context, uiState.url)
                        onFinishApp()
                    }
                }
            }
        }
    }
}

fun launchCustomTab(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}