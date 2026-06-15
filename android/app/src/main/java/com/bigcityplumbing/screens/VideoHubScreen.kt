package com.bigcityplumbing.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bigcityplumbing.config.AppConfig
import com.bigcityplumbing.ui.theme.BrandBlue

@Composable
fun VideoHubScreen() {
    val context = LocalContext.current

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
            embedUrl = AppConfig.youtubePlaylistEmbedUrl(),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.youtubeChannelUrl()))
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
        ) { Text("Open in YouTube") }
    }
}

/**
 * Lightweight WebView wrapper that loads a YouTube playlist embed.
 * For production, consider the official YouTube Player API for finer control.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlaylistView(embedUrl: String, modifier: Modifier = Modifier) {
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
                loadUrl(embedUrl)
            }
        },
        update = { webView -> webView.loadUrl(embedUrl) },
    )
}
