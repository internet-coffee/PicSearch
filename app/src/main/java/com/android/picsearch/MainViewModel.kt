package com.android.picsearch

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.IntentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.picsearch.network.LitterboxUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Success(val url: String) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val engine = SearchEngine.fromIntent(intent)
        when {
            intent.type?.startsWith("image/") == true -> {
                val uri =
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) {
                    uploadImage(uri, engine)
                } else {
                    _uiState.value =
                        UiState.Error(getString(R.string.error_failed_to_get_image_uri))
                }
            }

            else -> {
                _uiState.value = UiState.Error(getString(R.string.error_unsupported_content_type))
            }
        }
    }

    private fun uploadImage(uri: Uri, engine: SearchEngine) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val finalUrl = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IOException(getString(R.string.error_cannot_read_file))
                    val mimeType = resolver.getType(uri) ?: "image/jpeg"
                    val fileName = getFileName(uri, resolver)

                    engine.buildUrl(LitterboxUploader.upload(bytes, mimeType, fileName))
                }
                _uiState.value = UiState.Success(finalUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: getString(R.string.error_unknown))
            }
        }
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "uploaded_image"
    }
}