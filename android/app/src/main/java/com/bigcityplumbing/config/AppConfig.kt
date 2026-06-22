package com.bigcityplumbing.config

/**
 * A money-saving offer shown in the "Special Offers" section on the Home tab.
 * Customers redeem by tapping the card, which dials the shop.
 *
 * @param discount  big eye-catching value, e.g. "$25 OFF" or "FREE"
 * @param title     what the offer applies to, e.g. "Any Service Call"
 * @param details   fine print / restrictions shown under the title
 * @param code      promo code the customer mentions when booking
 */
data class Coupon(
    val discount: String,
    val title: String,
    val details: String,
    val code: String,
)

/**
 * Single source of truth for app-wide constants the client will swap before submission.
 * Edit ONLY this file when changing phone number, YouTube channel, etc.
 */
object AppConfig {
    // ============================================================
    // CLIENT: REPLACE THESE PLACEHOLDERS BEFORE STORE SUBMISSION
    // ============================================================
    const val COMPANY_NAME = "Big City Plumbing & Heating"
    const val COMPANY_ADDRESS = "2639 Middle Country Rd, Centereach, NY 11720"
    const val PHONE_NUMBER_DISPLAY = "(631) 361-9500"   // shown to user
    const val PHONE_NUMBER_DIAL = "+16313619500"        // tel: format, no spaces
    const val EMAIL = "service@example.com"
    const val WEBSITE_URL = "https://example.com"

    // YouTube — the Videos tab shows this playlist (must be Public or Unlisted to embed).
    // Playlist ID is the part after "list=" in a youtube.com/playlist?list=... URL.
    const val YOUTUBE_PLAYLIST_ID = "PLCA5F1wp6EynBq4VyPhGawPTbUypB3uRF"

    // YouTube Data API v3 key (read-only). Lets the Videos tab list ALL videos,
    // newest first. Restrict it in Google Cloud Console to the YouTube Data API
    // and to this app's package. Leave empty to fall back to the public RSS feed
    // (most-recent ~15 videos only).
    const val YOUTUBE_API_KEY = ""

    // Coupons shown on the Home tab. Edit/add/remove freely — the Home screen
    // adapts to however many are listed. Keep "discount" short so it fits the card.
    val OFFERS: List<Coupon> = listOf(
        Coupon(
            discount = "$25 OFF",
            title = "Any Service Call",
            details = "New customers. Mention code when you book.",
            code = "BIGCITY25",
        ),
        Coupon(
            discount = "FREE",
            title = "Estimate on Installs",
            details = "Water heaters, boilers & more. No obligation.",
            code = "FREEEST",
        ),
        Coupon(
            discount = "10% OFF",
            title = "Senior & Military",
            details = "Valid with ID. Cannot be combined with other offers.",
            code = "HEROES10",
        ),
    )
    // ============================================================

    /** Build a tel: URI safe for Intent.ACTION_DIAL. */
    fun telUri(): String = "tel:$PHONE_NUMBER_DIAL"

    /** Build YouTube embed URL for the playlist (shown in the Videos tab WebView). */
    fun youtubePlaylistEmbedUrl(): String =
        "https://www.youtube.com/embed/videoseries?list=$YOUTUBE_PLAYLIST_ID"

    /** Public playlist URL — used by the "Open in YouTube" button. */
    fun youtubeChannelUrl(): String =
        "https://www.youtube.com/playlist?list=$YOUTUBE_PLAYLIST_ID"
}
