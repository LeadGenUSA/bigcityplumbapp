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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

// ---- Model -----------------------------------------------------------------

data class PlaylistVideo(val id: String, val title: String, val published: String = "") {
    // hqdefault is 4:3 with letterbox bars that a 16:9 crop removes cleanly.
    val thumbnailUrl: String get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}

private enum class LoadState { LOADING, LOADED, FAILED }

// ---- Screen ----------------------------------------------------------------

/**
 * Video Hub: a single-column list of tappable video cards, newest first, each
 * playing in-app when tapped. Uses the YouTube Data API (all videos) when a key
 * is configured; otherwise falls back to the public playlist RSS feed
 * (most-recent ~15 videos). New videos on the playlist appear automatically.
 */
@Composable
fun VideoHubScreen() {
    var videos by remember { mutableStateOf<List<PlaylistVideo>>(emptyList()) }
    var loadState by remember { mutableStateOf(LoadState.LOADING) }
    var selected by remember { mutableStateOf<PlaylistVideo?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(retryKey) {
        loadState = LoadState.LOADING
        val result = runCatching { fetchVideos() }.getOrDefault(emptyList())
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
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
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
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
                modifier = Modifier.size(52.dp),
            )
        }
        Text(
            video.title,
            style = MaterialTheme.typography.titleMedium,
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

// ---- Video loading ---------------------------------------------------------

private suspend fun fetchVideos(): List<PlaylistVideo> = withContext(Dispatchers.IO) {
    if (AppConfig.YOUTUBE_API_KEY.isNotEmpty()) {
        runCatching { fetchViaApi() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { return@withContext it }
    }
    runCatching { fetchViaRss() }.getOrDefault(emptyList())
}

// YouTube Data API v3: playlistItems.list, paged, sorted newest-first.
private fun fetchViaApi(): List<PlaylistVideo> {
    val out = mutableListOf<PlaylistVideo>()
    var pageToken: String? = null
    do {
        val url = buildString {
            append("https://www.googleapis.com/youtube/v3/playlistItems")
            append("?part=snippet,contentDetails&maxResults=50")
            append("&playlistId=").append(AppConfig.YOUTUBE_PLAYLIST_ID)
            append("&key=").append(AppConfig.YOUTUBE_API_KEY)
            if (pageToken != null) append("&pageToken=").append(pageToken)
        }
        val root = JSONObject(httpGet(url))
        val items = root.getJSONArray("items")
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val snippet = item.getJSONObject("snippet")
            val details = item.optJSONObject("contentDetails")
            val id = details?.optString("videoId").orEmptyOr {
                snippet.optJSONObject("resourceId")?.optString("videoId") ?: ""
            }
            if (id.isNotEmpty()) {
                val published = details?.optString("videoPublishedAt").orEmptyOr {
                    snippet.optString("publishedAt", "")
                }
                out.add(PlaylistVideo(id, snippet.optString("title", ""), published))
            }
        }
        pageToken = root.optString("nextPageToken", "").ifEmpty { null }
    } while (pageToken != null)
    return out.sortedByDescending { it.published }   // newest first
}

private fun fetchViaRss(): List<PlaylistVideo> {
    val xml = httpGet("https://www.youtube.com/feeds/videos.xml?playlist_id=${AppConfig.YOUTUBE_PLAYLIST_ID}")
    return parseFeed(xml).sortedByDescending { it.published }
}

private fun httpGet(urlStr: String): String {
    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
    }
    try {
        if (conn.responseCode !in 200..299) throw RuntimeException("HTTP ${conn.responseCode}")
        return conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

/** Pulls (videoId, title, published) out of each <entry> in the playlist Atom feed. */
private fun parseFeed(xml: String): List<PlaylistVideo> {
    val videos = mutableListOf<PlaylistVideo>()
    val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        .newPullParser()
    parser.setInput(StringReader(xml))
    var insideEntry = false
    var videoId: String? = null
    var title: String? = null
    var published: String? = null
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "entry" -> { insideEntry = true; videoId = null; title = null; published = null }
                "yt:videoId" -> if (insideEntry) videoId = parser.nextText().trim()
                "title" -> if (insideEntry && title == null) title = parser.nextText().trim()
                "published" -> if (insideEntry && published == null) published = parser.nextText().trim()
            }
            XmlPullParser.END_TAG -> if (parser.name == "entry") {
                val id = videoId
                if (id != null && id.isNotEmpty()) {
                    videos.add(PlaylistVideo(id, title ?: "", published ?: ""))
                }
                insideEntry = false
            }
        }
        event = parser.next()
    }
    return videos
}

private inline fun String?.orEmptyOr(fallback: () -> String): String =
    if (!this.isNullOrEmpty()) this else fallback()
