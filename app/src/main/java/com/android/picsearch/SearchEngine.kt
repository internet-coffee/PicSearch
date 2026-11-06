package com.android.picsearch

import android.content.Intent
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class SearchEngine(private val baseUrl: String) {
    GOOGLE("https://lens.google.com/uploadbyurl?url="),
    TRACE_MOE("https://trace.moe/?url="),
    SAUCENAO("https://saucenao.com/search.php?url="),
    BING("https://www.bing.com/images/search?view=detailv2&iss=sbi&form=SBIVSP&sbisrc=UrlPaste&q=imgurl:");

    fun buildUrl(imageUrl: String): String =
        baseUrl + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8.toString())

    companion object {
        fun fromIntent(intent: Intent?): SearchEngine {
            val className = intent?.component?.className.orEmpty()
            return when {
                className.endsWith(".TraceMoeAlias") -> TRACE_MOE
                className.endsWith(".SauceNaoAlias") -> SAUCENAO
                className.endsWith(".BingAlias") -> BING
                else -> GOOGLE
            }
        }
    }
}