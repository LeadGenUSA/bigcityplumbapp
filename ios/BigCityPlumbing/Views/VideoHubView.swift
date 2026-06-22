import SwiftUI
import Foundation
import UIKit
import WebKit

// MARK: - Model

/// One video from the playlist. Thumbnails come straight from YouTube's image
/// CDN by video ID (hqdefault is 4:3 with letterbox bars that a 16:9 fill crops
/// off cleanly).
struct PlaylistVideo: Identifiable, Hashable {
    let id: String
    let title: String
    var thumbnailURL: URL { URL(string: "https://i.ytimg.com/vi/\(id)/hqdefault.jpg")! }
}

// MARK: - Playlist feed loader

/// Loads the playlist's videos from YouTube's public RSS/Atom feed
/// (no API key required). Note: the feed returns only the ~15 most recent
/// videos — switch to the YouTube Data API if the playlist grows beyond that.
@MainActor
final class PlaylistLoader: ObservableObject {
    enum State { case loading, loaded, failed }
    @Published var videos: [PlaylistVideo] = []
    @Published var state: State = .loading

    func load(playlistID: String) {
        state = .loading
        guard let url = URL(string:
            "https://www.youtube.com/feeds/videos.xml?playlist_id=\(playlistID)") else {
            state = .failed; return
        }
        URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            let parsed = data.map { YouTubeFeedParser.parse($0) } ?? []
            Task { @MainActor in
                guard let self else { return }
                self.videos = parsed
                self.state = parsed.isEmpty ? .failed : .loaded
            }
        }.resume()
    }
}

/// Minimal XMLParser delegate that pulls (videoId, title) out of each <entry>.
private final class YouTubeFeedParser: NSObject, XMLParserDelegate {
    private var videos: [PlaylistVideo] = []
    private var insideEntry = false
    private var currentVideoID: String?
    private var currentTitle = ""
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
        }
        buffer = ""
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        buffer += string
    }

    func parser(_ parser: XMLParser, didEndElement elementName: String,
                namespaceURI: String?, qualifiedName qName: String?) {
        guard insideEntry else { buffer = ""; return }
        let text = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
        switch elementName {
        case "yt:videoId":
            currentVideoID = text
        case "title" where currentTitle.isEmpty:
            currentTitle = text
        case "entry":
            if let id = currentVideoID, !id.isEmpty {
                videos.append(PlaylistVideo(id: id, title: currentTitle))
            }
            insideEntry = false
        default:
            break
        }
        buffer = ""
    }
}

// MARK: - Video Hub (grid)

struct VideoHubView: View {
    @StateObject private var loader = PlaylistLoader()
    private let columns = [GridItem(.flexible(), spacing: 12),
                           GridItem(.flexible(), spacing: 12)]

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
                    Button("Retry") { loader.load(playlistID: AppConfig.youtubePlaylistID) }
                        .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .loaded:
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 16) {
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
        .onAppear { if loader.state != .loaded { loader.load(playlistID: AppConfig.youtubePlaylistID) } }
    }
}

private struct VideoCard: View {
    let video: PlaylistVideo

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack {
                Color(.secondarySystemBackground)
                AsyncImage(url: video.thumbnailURL) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    ProgressView()
                }
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 36))
                    .foregroundStyle(.white.opacity(0.92))
                    .shadow(radius: 3)
            }
            .aspectRatio(16.0 / 9.0, contentMode: .fill)
            .frame(maxWidth: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            Text(video.title)
                .font(.subheadline).fontWeight(.semibold)
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

/// Plays a single YouTube video embedded in-app. Uses the same third-party
/// origin baseURL as the playlist embed to avoid YouTube's "Video unavailable"
/// (error 152) for embeds hosted on youtube.com itself.
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
