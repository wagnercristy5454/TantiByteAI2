package com.tantibyteai.app.commands

import android.content.Context
import android.content.Intent
import android.net.Uri

class CommandDispatcher(private val context: Context) {

    fun handleFreeText(input: String) {
        val t = input.lowercase().trim()
        when {
            t.contains("maps") || t.contains("navigatie") || t.contains("drum") -> openMaps(t)
            t.contains("youtube") || t.contains("video") -> openYouTube(t)
            t.contains("suna") || t.contains("apel") -> openDialer(t)
            t.contains("whatsapp") -> openWhatsApp()
            t.contains("browser") || t.contains("internet") -> openBrowser()
        }
    }

    private fun openMaps(query: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun openYouTube(query: String) {
        val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun openDialer(input: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun openWhatsApp() {
        val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun openBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
