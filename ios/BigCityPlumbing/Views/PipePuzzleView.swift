import SwiftUI
import WebKit

/// Hosts the self-contained Pipe Drop HTML5 game (bundled at
/// Resources/game/index.html) inside a WKWebView. Plays entirely offline.
struct PipePuzzleView: View {
    var body: some View {
        GameWebView()
            .ignoresSafeArea()
            .toolbar(.hidden, for: .navigationBar)  // game has its own header
    }
}

/// WKWebView that defers loading its content until it actually has a real,
/// non-zero width. WKWebView fixes its viewport/layout width at load time:
/// if we load while the frame is still .zero (which is what happens when a
/// UIViewRepresentable loads in updateUIView), WebKit falls back to a 320pt
/// default layout width and pins the viewport there, scaling the whole page
/// up and starving the flex board of height. Loading in layoutSubviews once
/// bounds.width > 0 makes the viewport resolve to the real device width.
final class GameWebKitView: WKWebView {
    private var didLoadContent = false

    override func layoutSubviews() {
        super.layoutSubviews()
        guard !didLoadContent, bounds.width > 0 else { return }
        didLoadContent = true
        guard let url = Bundle.main.url(forResource: "index", withExtension: "html",
                                        subdirectory: "game")
            ?? Bundle.main.url(forResource: "index", withExtension: "html") else { return }
        loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
    }
}

private struct GameWebView: UIViewRepresentable {
    func makeUIView(context: Context) -> GameWebKitView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.preferences.javaScriptCanOpenWindowsAutomatically = false
        // Start with a real screen-sized frame (not .zero) as extra insurance
        // so the viewport never resolves against a zero width.
        let webView = GameWebKitView(frame: UIScreen.main.bounds, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.backgroundColor = .systemBackground
        webView.isOpaque = false
        return webView
    }

    func updateUIView(_ webView: GameWebKitView, context: Context) {
        // Loading is handled in GameWebKitView.layoutSubviews once the view
        // has a real width — nothing to do here.
    }
}

#Preview {
    NavigationStack { PipePuzzleView() }
}
