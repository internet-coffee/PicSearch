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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val url: String) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    fun handleIntent(intent: Intent?, contentResolver: ContentResolver) {
        if (intent?.action != Intent.ACTION_SEND) return
        when {
            intent.type?.startsWith("image/") == true -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) {
                    uploadImage(uri, contentResolver)
                } else {
                    _uiState.value = UiState.Error(getString(R.string.error_failed_to_get_image_uri))
                }
            }
            else -> {
                _uiState.value = UiState.Error(getString(R.string.error_unsupported_content_type))
            }
        }
    }

    private fun uploadImage(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileBytes = inputStream.readBytes()
                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val fileName = getFileName(uri, contentResolver)

                    val imageUrl = LitterboxUploader.upload(fileBytes, mimeType, fileName)

                    val encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8.toString())
                    val finalUrl = "https://lens.google.com/uploadbyurl?url=$encodedUrl"
                    _uiState.value = UiState.Success(finalUrl)
                } ?: run {
                    _uiState.value = UiState.Error(getString(R.string.error_cannot_read_file))
                }
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