package com.bigcityplumbing.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts the self-contained Pipe Drop HTML5 game (bundled in assets/game/index.html)
 * inside a WebView. The game runs entirely offline — no network requests.
 */
@Composable
fun PipePuzzleScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = ::buildGameWebView,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun buildGameWebView(context: android.content.Context): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        webViewClient = WebViewClient()
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true       // game uses localStorage for "best score"
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // Needed to load the bundled file:///android_asset/ URL on some devices.
            allowFileAccess = true
            allowContentAccess = false
        }
        loadUrl("file:///android_asset/game/index.html")
    }
}