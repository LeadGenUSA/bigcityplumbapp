package com.bigcityplumbing.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bigcityplumbing.config.AppConfig

@Composable
fun VideoHubScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Video Hub", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Tips, walkthroughs and customer stories.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        YouTubePlaylistView(
            playlistId = AppConfig.YOUTUBE_PLAYLIST_ID,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

/**
 * Hosts the YouTube playlist inside an <iframe> on a youtube.com origin.
 * Loading the bare /embed URL as a top-level page triggers YouTube's
 * "player configuration error" (153); an iframe with a real origin is reliable.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlaylistView(playlistId: String, modifier: Modifier = Modifier) {
    val html = remember(playlistId) {
        """
        <!doctype html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>html,body{margin:0;padding:0;background:#000;height:100%}
        .wrap{position:fixed;inset:0}iframe{border:0;width:100%;height:100%}</style>
        </head><body><div class="wrap">
        <iframe src="https://www.youtube.com/embed/videoseries?list=$playlistId&playsinline=1&rel=0&origin=https://www.bigcityplumbing.com"
          allow="encrypted-media; picture-in-picture; web-share; fullscreen" allowfullscreen></iframe>
        </div></body></html>
        """.trimIndent()
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                // baseURL must be a normal third-party origin (NOT youtube.com):
                // an empty referrer triggers YouTube error 153, while embedding
                // "on youtube.com" triggers "Video unavailable" 152. A real site
                // origin makes it a valid third-party embed.
                loadDataWithBaseURL("https://www.bigcityplumbing.com", html, "text/html", "utf-8", null)
            }
        },
    )
}
