package com.android.picsearch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.picsearch.ui.theme.PicSearchTheme

@Composable
fun MainScreen(
    uiState: UiState,
    onFinishApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is UiState.Idle -> {}
            is UiState.Error -> Text(text = stringResource(R.string.error_message, uiState.message))
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

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadingPreview() {
    PicSearchTheme { MainScreen(uiState = UiState.Loading, onFinishApp = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenErrorPreview() {
    PicSearchTheme { MainScreen(uiState = UiState.Error("Network error"), onFinishApp = {}) }
}