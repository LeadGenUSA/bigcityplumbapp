import Foundation

/// A money-saving offer shown in the "Special Offers" section on the Home tab.
/// Customers redeem by tapping the card, which dials the shop.
struct Coupon: Identifiable, Hashable {
    let id = UUID()
    let discount: String   // big eye-catching value, e.g. "$25 OFF" or "FREE"
    let title: String      // what the offer applies to
    let details: String    // fine print / restrictions
    let code: String       // promo code the customer mentions when booking
}

/// Single source of truth for client-specific values. Replace placeholders before submission.
enum AppConfig {
    // ============================================================
    // CLIENT: REPLACE THESE PLACEHOLDERS BEFORE STORE SUBMISSION
    // ============================================================
    static let companyName = "Big City Plumbing & Heating"
    static let companyAddress = "2639 Middle Country Rd, Centereach, NY 11720"
    static let phoneNumberDisplay = "(631) 361-9500"
    static let phoneNumberDial = "+16313619500"  // tel: format, no spaces
    static let email = "service@example.com"
    static let websiteURL = URL(string: "https://example.com")!

    // YouTube — the Videos tab shows this playlist (must be Public or Unlisted to embed).
    // Playlist ID is the part after "list=" in a youtube.com/playlist?list=... URL.
    static let youtubePlaylistID = "PLCA5F1wp6EynBq4VyPhGawPTbUypB3uRF"

    // YouTube Data API v3 key (read-only). Lets the Videos tab list ALL videos,
    // newest first. Restrict it in Google Cloud Console to the YouTube Data API
    // and to this app's bundle ID. Leave empty to fall back to the public RSS
    // feed (most-recent ~15 videos only).
    static let youtubeApiKey = "AIzaSyBMqrBa4YERIUmtBh6bKEib6FqHUJf-4u4"

    /// Coupons shown on the Home tab. Edit/add/remove freely — the Home screen adapts.
    static let offers: [Coupon] = [
        Coupon(discount: "$25 OFF", title: "Any Service Call",
               details: "New customers. Mention code when you book.", code: "BIGCITY25"),
        Coupon(discount: "FREE", title: "Estimate on Installs",
               details: "Water heaters, boilers & more. No obligation.", code: "FREEEST"),
        Coupon(discount: "10% OFF", title: "Senior & Military",
               details: "Valid with ID. Cannot be combined with other offers.", code: "HEROES10"),
    ]
    // ============================================================

    /// `tel:` URL safe to pass to UIApplication.shared.open
    static var telURL: URL? {
        URL(string: "tel://\(phoneNumberDial)")
    }

    /// Embeddable YouTube playlist URL (loaded inside WKWebView)
    static var youtubePlaylistEmbedURL: URL {
        URL(string: "https://www.youtube.com/embed/videoseries?list=\(youtubePlaylistID)")!
    }

    /// Public playlist URL — used by the "Open in YouTube" button.
    static var youtubeChannelURL: URL {
        URL(string: "https://www.youtube.com/playlist?list=\(youtubePlaylistID)")!
    }
}
