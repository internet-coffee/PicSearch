package com.android.picsearch.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object LitterboxUploader {

    private const val TAG = "LitterboxUploader"
    private const val TIMEOUT = 30_000
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * @param fileBytes   圖片的原始位元組
     * @param mimeType    MIME 類型
     * @param fileName    上傳時使用的檔名
     * @return 上傳成功後公開 URL，失敗回傳 null
     */
    suspend fun upload(
        fileBytes: ByteArray,
        mimeType: String,
        fileName: String
    ): String? {
        Log.d(TAG, "Trying Litterbox... (${fileBytes.size} bytes)")
        val litterboxUrl = uploadToLitterbox(fileBytes, mimeType, fileName)
        if (litterboxUrl != null) {
            Log.d(TAG, "Litterbox success: $litterboxUrl")
            return litterboxUrl
        }

        Log.e(TAG, "Litterbox failed")
        return null
    }

    // Litterbox

    private suspend fun uploadToLitterbox(
        fileBytes: ByteArray,
        mimeType: String,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val boundary = boundary()
            val conn = openConnection(boundary)

            DataOutputStream(conn.outputStream).use { dos ->
                dos.writeField(boundary, "reqtype", "fileupload")
                dos.writeField(boundary, "time", "1h")
                dos.writeFilePart(boundary, "fileToUpload", fileName, mimeType, fileBytes)
                dos.writeBytes("--$boundary--\r\n")
                dos.flush()
            }

            readResponse(conn)
        } catch (e: Exception) {
            Log.e(TAG, "Litterbox exception", e)
            null
        }
    }

    // 輔助函式

    private fun boundary() =
        "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "")

    private fun openConnection(boundary: String): HttpURLConnection =
        (URL("https://litterbox.catbox.moe/resources/internals/api.php").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            useCaches = false
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

    private fun DataOutputStream.writeField(boundary: String, name: String, value: String) {
        writeBytes("--$boundary\r\n")
        writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        writeBytes("$value\r\n")
    }

    private fun DataOutputStream.writeFilePart(
        boundary: String,
        fieldName: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ) {
        writeBytes("--$boundary\r\n")
        writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        writeBytes("Content-Type: $mimeType\r\n\r\n")
        write(bytes)
        writeBytes("\r\n")
    }

    private fun readResponse(conn: HttpURLConnection): String? {
        val code = conn.responseCode
        return if (code == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }.trim().takeIf { it.isNotEmpty() }
        } else {
            Log.e(TAG, "Litterbox HTTP $code")
            null
        }
    }
}