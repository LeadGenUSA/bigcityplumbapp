import SwiftUI
import UIKit
import WebKit

struct VideoHubView: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Tips, walkthroughs and customer stories.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            YouTubePlaylistWebView(playlistID: AppConfig.youtubePlaylistID)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .padding(.horizontal)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            Button {
                openURL(AppConfig.youtubeChannelURL)
            } label: {
                Text("Open in YouTube").bold()
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .foregroundStyle(.white)
                    .background(Theme.brandBlue)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
        .padding(.top, 8)
        .navigationTitle("Video Hub")
    }
}

/// SwiftUI wrapper for WKWebView. Hosts the YouTube playlist inside a proper
/// <iframe> on a youtube.com origin. Loading the bare /embed URL as a top-level
/// page triggers YouTube's "player configuration error" (153); an iframe with a
/// real origin (set via baseURL) is the reliable way to embed in a WKWebView.
struct YouTubePlaylistWebView: UIViewRepresentable {
    let playlistID: String

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = .all
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = true
        webView.backgroundColor = .secondarySystemBackground
        webView.isOpaque = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard webView.url == nil, !webView.isLoading else { return }  // load once
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
              src="https://www.youtube.com/embed/videoseries?list=\(playlistID)&playsinline=1&rel=0&origin=https://www.bigcityplumbing.com"
              allow="encrypted-media; picture-in-picture; web-share; fullscreen"
              allowfullscreen></iframe>
          </div>
        </body>
        </html>
        """
        // baseURL must be a normal third-party origin (NOT youtube.com): an empty
        // referrer triggers YouTube error 153, while embedding "on youtube.com"
        // triggers "Video unavailable" 152. A real site origin makes it a valid
        // third-party embed.
        webView.loadHTMLString(html, baseURL: URL(string: "https://www.bigcityplumbing.com"))
    }
}

#Preview {
    NavigationStack { VideoHubView() }
}
