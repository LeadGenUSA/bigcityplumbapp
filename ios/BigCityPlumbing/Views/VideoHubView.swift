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

            YouTubePlaylistWebView(url: AppConfig.youtubePlaylistEmbedURL)
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

/// SwiftUI wrapper for WKWebView. Loads the YouTube playlist embed URL.
struct YouTubePlaylistWebView: UIViewRepresentable {
    let url: URL

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
        // Load once. YouTube redirects the URL after loading, so comparing against
        // `url` would reload endlessly; only load when nothing has loaded yet.
        if webView.url == nil {
            webView.load(URLRequest(url: url))
        }
    }
}

#Preview {
    NavigationStack { VideoHubView() }
}
