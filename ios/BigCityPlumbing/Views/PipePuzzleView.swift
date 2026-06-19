import SwiftUI
import WebKit

/// Hosts the self-contained Pipe Drop HTML5 game (bundled at
/// Resources/game/index.html) inside a WKWebView. Plays entirely offline.
struct PipePuzzleView: View {
    var body: some View {
        // GeometryReader gives the WebView a *concrete* size. A plain
        // .frame(maxWidth:.infinity) doesn't override a UIViewRepresentable's
        // intrinsic size, so WKWebView fell back to a 320pt layout width and
        // scaled the page up. Passing explicit width/height makes the viewport
        // resolve to the real device width (e.g. 390pt).
        GeometryReader { geo in
            GameWebView()
                .frame(width: geo.size.width, height: geo.size.height)
        }
        .ignoresSafeArea()
        .toolbar(.hidden, for: .navigationBar)
    }
}

private struct GameWebView: UIViewRepresentable {
    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.preferences.javaScriptCanOpenWindowsAutomatically = false
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.backgroundColor = .systemBackground
        webView.isOpaque = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        // Load only on first appearance — avoid reloading on every layout change.
        if webView.url != nil { return }
        guard let url = Bundle.main.url(forResource: "index", withExtension: "html",
                                        subdirectory: "game") else {
            // Fallback: file may be bundled flat
            if let flat = Bundle.main.url(forResource: "index", withExtension: "html") {
                webView.loadFileURL(flat, allowingReadAccessTo: flat.deletingLastPathComponent())
            }
            return
        }
        webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
    }
}

#Preview {
    NavigationStack { PipePuzzleView() }
}