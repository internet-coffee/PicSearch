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
     * @throws java.io.IOException 上傳失敗時拋出
     */
    suspend fun upload(
        fileBytes: ByteArray,
        mimeType: String,
        fileName: String
    ): String? {
        Log.d(TAG, "Trying Litterbox... (${fileBytes.size} bytes)")
        val litterboxUrl = uploadToLitterbox(fileBytes, mimeType, fileName)
        Log.d(TAG, "Litterbox success: $litterboxUrl")
        return litterboxUrl
    }


    // Litterbox

    private suspend fun uploadToLitterbox(
        fileBytes: ByteArray,
        mimeType: String,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        val boundary = boundary()
        val conn = openConnection(boundary)

        DataOutputStream(conn.outputStream).use { dos ->
            dos.writeField(boundary, "reqtype", "fileupload")
            dos.writeField(boundary, "time", "1h")
            dos.writeFilePart(boundary, "fileToUpload", fileName, mimeType, fileBytes)
            dos.writeUtf8("--$boundary--\r\n")
            dos.flush()
        }

        readResponse(conn)
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

    private fun DataOutputStream.writeUtf8(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
    }

    private fun DataOutputStream.writeField(boundary: String, name: String, value: String) {
        writeUtf8("--$boundary\r\n")
        writeUtf8("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        writeUtf8("$value\r\n")
    }

    private fun DataOutputStream.writeFilePart(
        boundary: String,
        fieldName: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ) {
        writeUtf8("--$boundary\r\n")
        writeUtf8("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        writeUtf8("Content-Type: $mimeType\r\n\r\n")
        write(bytes)
        writeUtf8("\r\n")
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        if (code != 200) {
            Log.e(TAG, "Litterbox HTTP $code")
            throw java.io.IOException("HTTP $code")
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }.trim()
        if (body.isEmpty()) {
            throw java.io.IOException("Empty response body")
        }
        return body
    }
}