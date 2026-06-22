import SwiftUI
import Foundation
import UIKit
import WebKit

// MARK: - Model

/// One video from the playlist. `published` is the ISO-8601 publish date used
/// to sort newest-first (ISO strings sort chronologically). Thumbnails come
/// from YouTube's image CDN by video ID (hqdefault is 4:3 with letterbox bars
/// that a 16:9 crop removes cleanly).
struct PlaylistVideo: Identifiable, Hashable {
    let id: String
    let title: String
    let published: String
    var thumbnailURL: URL { URL(string: "https://i.ytimg.com/vi/\(id)/hqdefault.jpg")! }
}

// MARK: - Loader

/// Loads the playlist's videos. Uses the YouTube Data API (all videos, sorted
/// newest-first) when an API key is configured; otherwise falls back to the
/// public RSS feed (most-recent ~15 videos).
@MainActor
final class PlaylistLoader: ObservableObject {
    enum State { case loading, loaded, failed }
    @Published var videos: [PlaylistVideo] = []
    @Published var state: State = .loading

    func load() {
        state = .loading
        Task {
            let result = await Self.fetch()
            self.videos = result
            self.state = result.isEmpty ? .failed : .loaded
        }
    }

    private static func fetch() async -> [PlaylistVideo] {
        if !AppConfig.youtubeApiKey.isEmpty,
           let viaAPI = try? await fetchViaAPI(), !viaAPI.isEmpty {
            return viaAPI
        }
        return (try? await fetchViaRSS()) ?? []
    }

    // YouTube Data API v3: playlistItems.list, paged, sorted newest-first.
    private static func fetchViaAPI() async throws -> [PlaylistVideo] {
        var collected: [PlaylistVideo] = []
        var pageToken: String?
        repeat {
            var comps = URLComponents(string: "https://www.googleapis.com/youtube/v3/playlistItems")!
            comps.queryItems = [
                URLQueryItem(name: "part", value: "snippet,contentDetails"),
                URLQueryItem(name: "maxResults", value: "50"),
                URLQueryItem(name: "playlistId", value: AppConfig.youtubePlaylistID),
                URLQueryItem(name: "key", value: AppConfig.youtubeApiKey),
            ]
            if let token = pageToken {
                comps.queryItems?.append(URLQueryItem(name: "pageToken", value: token))
            }
            var request = URLRequest(url: comps.url!)
            // An iOS-app-restricted API key requires this header (the app's own
            // bundle id) since we call the REST endpoint directly, not via a SDK.
            if let bundleID = Bundle.main.bundleIdentifier {
                request.setValue(bundleID, forHTTPHeaderField: "X-Ios-Bundle-Identifier")
            }
            let (data, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, !(200...299).contains(http.statusCode) {
                throw URLError(.badServerResponse)
            }
            let decoded = try JSONDecoder().decode(APIResponse.self, from: data)
            for item in decoded.items {
                let id = item.contentDetails?.videoId ?? item.snippet.resourceId?.videoId ?? ""
                guard !id.isEmpty else { continue }
                let published = item.contentDetails?.videoPublishedAt ?? item.snippet.publishedAt ?? ""
                collected.append(PlaylistVideo(id: id, title: item.snippet.title, published: published))
            }
            pageToken = decoded.nextPageToken
        } while pageToken != nil
        return collected.sorted { $0.published > $1.published }   // newest first
    }

    private static func fetchViaRSS() async throws -> [PlaylistVideo] {
        let url = URL(string:
            "https://www.youtube.com/feeds/videos.xml?playlist_id=\(AppConfig.youtubePlaylistID)")!
        let (data, _) = try await URLSession.shared.data(from: url)
        return YouTubeFeedParser.parse(data).sorted { $0.published > $1.published }
    }
}

// MARK: - Data API response (Codable)

private struct APIResponse: Decodable {
    let items: [APIItem]
    let nextPageToken: String?
}
private struct APIItem: Decodable {
    let snippet: APISnippet
    let contentDetails: APIContentDetails?
}
private struct APISnippet: Decodable {
    let title: String
    let publishedAt: String?
    let resourceId: APIResourceId?
}
private struct APIResourceId: Decodable { let videoId: String? }
private struct APIContentDetails: Decodable {
    let videoId: String?
    let videoPublishedAt: String?
}

// MARK: - RSS feed parser

/// Minimal XMLParser delegate pulling (videoId, title, published) from each <entry>.
private final class YouTubeFeedParser: NSObject, XMLParserDelegate {
    private var videos: [PlaylistVideo] = []
    private var insideEntry = false
    private var currentVideoID: String?
    private var currentTitle = ""
    private var currentPublished = ""
    private var buffer = ""

    static func parse(_ data: Data) -> [PlaylistVideo] {
        let p = YouTubeFeedParser()
        let parser = XMLParser(data: data)
        parser.delegate = p
        parser.parse()
        return p.videos
    }

    func parser(_ parser: XMLParser, didStartElement elementName: String,
                namespaceURI: String?, qualifiedName qName: String?,
                attributes attributeDict: [String: String]) {
        if elementName == "entry" {
            insideEntry = true
            currentVideoID = nil
            currentTitle = ""
            currentPublished = ""
        }
        buffer = ""
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) { buffer += string }

    func parser(_ parser: XMLParser, didEndElement elementName: String,
                namespaceURI: String?, qualifiedName qName: String?) {
        guard insideEntry else { buffer = ""; return }
        let text = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
        switch elementName {
        case "yt:videoId": currentVideoID = text
        case "title" where currentTitle.isEmpty: currentTitle = text
        case "published" where currentPublished.isEmpty: currentPublished = text
        case "entry":
            if let id = currentVideoID, !id.isEmpty {
                videos.append(PlaylistVideo(id: id, title: currentTitle, published: currentPublished))
            }
            insideEntry = false
        default: break
        }
        buffer = ""
    }
}

// MARK: - Video Hub (single-column list)

struct VideoHubView: View {
    @StateObject private var loader = PlaylistLoader()

    var body: some View {
        Group {
            switch loader.state {
            case .loading:
                ProgressView("Loading videos…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .failed:
                VStack(spacing: 10) {
                    Image(systemName: "wifi.exclamationmark")
                        .font(.largeTitle).foregroundStyle(.secondary)
                    Text("Couldn't load videos.").foregroundStyle(.secondary)
                    Button("Retry") { loader.load() }
                        .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .loaded:
                ScrollView {
                    LazyVStack(spacing: 18) {
                        ForEach(loader.videos) { video in
                            NavigationLink(destination: VideoPlayerView(video: video)) {
                                VideoCard(video: video)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(16)
                }
            }
        }
        .navigationTitle("Video Hub")
        .onAppear { if loader.state != .loaded { loader.load() } }
    }
}

private struct VideoCard: View {
    let video: PlaylistVideo

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack {
                Color(.secondarySystemBackground)
                AsyncImage(url: video.thumbnailURL) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    ProgressView()
                }
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 46))
                    .foregroundStyle(.white.opacity(0.92))
                    .shadow(radius: 4)
            }
            .aspectRatio(16.0 / 9.0, contentMode: .fill)
            .frame(maxWidth: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            Text(video.title)
                .font(.headline)
                .foregroundStyle(.primary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

// MARK: - Single-video player

struct VideoPlayerView: View {
    let video: PlaylistVideo

    var body: some View {
        SingleVideoWebView(videoID: video.id)
            .ignoresSafeArea(.container, edges: .bottom)
            .navigationTitle(video.title)
            .navigationBarTitleDisplayMode(.inline)
    }
}

/// Plays a single YouTube video embedded in-app. Uses a third-party origin
/// baseURL to avoid YouTube's "Video unavailable" (error 152) for embeds hosted
/// on youtube.com itself.
private struct SingleVideoWebView: UIViewRepresentable {
    let videoID: String

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []   // allow autoplay
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.backgroundColor = .black
        webView.isOpaque = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard webView.url == nil, !webView.isLoading else { return }
        let html = """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
          <style>html,body{margin:0;padding:0;background:#000;height:100%}
          .wrap{position:fixed;inset:0}iframe{border:0;width:100%;height:100%}</style>
        </head>
        <body>
          <div class="wrap">
            <iframe
              src="https://www.youtube.com/embed/\(videoID)?playsinline=1&rel=0&autoplay=1&origin=https://www.bigcityplumbing.com"
              allow="autoplay; encrypted-media; picture-in-picture; web-share; fullscreen"
              allowfullscreen></iframe>
          </div>
        </body>
        </html>
        """
        webView.loadHTMLString(html, baseURL: URL(string: "https://www.bigcityplumbing.com"))
    }
}

#Preview {
    NavigationStack { VideoHubView() }
}
