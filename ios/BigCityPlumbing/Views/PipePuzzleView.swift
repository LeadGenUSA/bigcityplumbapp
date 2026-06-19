import SwiftUI
import WebKit

/// Hosts the self-contained Pipe Drop HTML5 game (bundled at
/// Resources/game/index.html) inside a WKWebView. Plays entirely offline and
/// fullscreen: the bottom tab bar is hidden while the game is on screen, and
/// an in-game "Home" button returns the app to the Home tab.
struct PipePuzzleView: View {
    /// Called when the player taps the in-game Home button. The tab bar is
    /// hidden in fullscreen, so this is how the app switches away from the game.
    var onExit: () -> Void = {}

    var body: some View {
        // GeometryReader gives the WebView an explicit, correct frame from its
        // very first layout, and GameWebKitView defers loading until that frame
        // is non-zero. Together they guarantee WKWebView fixes its viewport at
        // the real device width (e.g. 390pt) instead of SwiftUI's intermediate
        // 320pt default — which it would otherwise pin for the page's lifetime.
        GeometryReader { geo in
            GameWebView(onExit: onExit)
                .frame(width: geo.size.width, height: geo.size.height)
        }
        .ignoresSafeArea()
        .toolbar(.hidden, for: .navigationBar)  // game has its own header
        .toolbar(.hidden, for: .tabBar)         // fullscreen: no bottom tab bar
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
    var onExit: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onExit: onExit) }

    /// Receives the "exitGame" message posted by the in-game Home button.
    final class Coordinator: NSObject, WKScriptMessageHandler {
        let onExit: () -> Void
        init(onExit: @escaping () -> Void) { self.onExit = onExit }
        func userContentController(_ controller: WKUserContentController,
                                   didReceive message: WKScriptMessage) {
            if message.name == "exitGame" { onExit() }
        }
    }

    func makeUIView(context: Context) -> GameWebKitView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.preferences.javaScriptCanOpenWindowsAutomatically = false
        // Bridge so the game's Home button (window.webkit.messageHandlers.exitGame)
        // can ask SwiftUI to leave the fullscreen game.
        config.userContentController.add(context.coordinator, name: "exitGame")
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
