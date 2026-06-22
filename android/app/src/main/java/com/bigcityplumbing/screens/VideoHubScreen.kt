package com.bigcityplumbing.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.bigcityplumbing.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

// ---- Model -----------------------------------------------------------------

data class PlaylistVideo(val id: String, val title: String) {
    // hqdefault is 4:3 with letterbox bars that a 16:9 crop removes cleanly.
    val thumbnailUrl: String get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}

private enum class LoadState { LOADING, LOADED, FAILED }

// ---- Screen ----------------------------------------------------------------

/**
 * Video Hub: a grid of tappable video cards for the playlist, each playing
 * in-app when tapped. The video list comes from YouTube's public playlist
 * RSS feed (no API key). Note: the feed only returns the ~15 most recent
 * videos — switch to the YouTube Data API if the playlist grows beyond that.
 */
@Composable
fun VideoHubScreen() {
    var videos by remember { mutableStateOf<List<PlaylistVideo>>(emptyList()) }
    var loadState by remember { mutableStateOf(LoadState.LOADING) }
    var selected by remember { mutableStateOf<PlaylistVideo?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(retryKey) {
        loadState = LoadState.LOADING
        val result = runCatching { fetchPlaylist(AppConfig.YOUTUBE_PLAYLIST_ID) }
            .getOrDefault(emptyList())
        videos = result
        loadState = if (result.isEmpty()) LoadState.FAILED else LoadState.LOADED
    }

    val current = selected
    if (current != null) {
        BackHandler { selected = null }
        VideoPlayer(video = current, onBack = { selected = null })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Video Hub", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Tips, walkthroughs and customer stories.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (loadState) {
                LoadState.LOADING ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                LoadState.FAILED ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Couldn't load videos.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { retryKey++ }) { Text("Retry") }
                    }
                LoadState.LOADED ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(videos) { video ->
                            VideoCard(video) { selected = video }
                        }
                    }
            }
        }
    }
}

@Composable
private fun VideoCard(video: PlaylistVideo, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            video.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---- Single-video player ---------------------------------------------------

@Composable
private fun VideoPlayer(video: PlaylistVideo, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
        }
        val html = remember(video.id) { singleVideoHtml(video.id) }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                buildVideoWebView(ctx).apply {
                    loadDataWithBaseURL("https://www.bigcityplumbing.com", html, "text/html", "utf-8", null)
                }
            },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun buildVideoWebView(context: android.content.Context): WebView =
    WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false   // allow autoplay
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webViewClient = WebViewClient()
        webChromeClient = WebChromeClient()
    }

private fun singleVideoHtml(videoId: String): String = """
    <!doctype html><html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
    <style>html,body{margin:0;padding:0;background:#000;height:100%}
    .wrap{position:fixed;inset:0}iframe{border:0;width:100%;height:100%}</style>
    </head><body><div class="wrap">
    <iframe src="https://www.youtube.com/embed/$videoId?playsinline=1&rel=0&autoplay=1&origin=https://www.bigcityplumbing.com"
      allow="autoplay; encrypted-media; picture-in-picture; web-share; fullscreen" allowfullscreen></iframe>
    </div></body></html>
""".trimIndent()

// ---- Feed loading ----------------------------------------------------------

private suspend fun fetchPlaylist(playlistId: String): List<PlaylistVideo> =
    withContext(Dispatchers.IO) {
        val url = URL("https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            conn.inputStream.bufferedReader().use { parseFeed(it.readText()) }
        } finally {
            conn.disconnect()
        }
    }

/** Pulls (videoId, title) out of each <entry> in the playlist Atom feed. */
private fun parseFeed(xml: String): List<PlaylistVideo> {
    val videos = mutableListOf<PlaylistVideo>()
    val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        .newPullParser()
    parser.setInput(StringReader(xml))
    var insideEntry = false
    var videoId: String? = null
    var title: String? = null
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "entry" -> { insideEntry = true; videoId = null; title = null }
                "yt:videoId" -> if (insideEntry) videoId = parser.nextText().trim()
                "title" -> if (insideEntry && title == null) title = parser.nextText().trim()
            }
            XmlPullParser.END_TAG -> if (parser.name == "entry") {
                val id = videoId
                if (id != null && id.isNotEmpty()) videos.add(PlaylistVideo(id, title ?: ""))
                insideEntry = false
            }
        }
        event = parser.next()
    }
    return videos
}
