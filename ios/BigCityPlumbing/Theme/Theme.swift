import SwiftUI
import UIKit

/// Brand colors and shared appearance helpers.
enum Theme {
    static let brandBlue       = Color(red: 0x19/255, green: 0x76/255, blue: 0xD2/255)  // #1976D2
    static let brandBlueDark   = Color(red: 0x0D/255, green: 0x47/255, blue: 0xA1/255)  // #0D47A1
    static let brandBlueLight  = Color(red: 0x63/255, green: 0xA4/255, blue: 0xFF/255)  // #63A4FF
    static let brandOrange     = Color(red: 0xFF/255, green: 0x6F/255, blue: 0x00/255)  // #FF6F00
    static let brandOrangeDark = Color(red: 0xC4/255, green: 0x3E/255, blue: 0x00/255)  // #C43E00

    /// Apply UINavigationBar / UITabBar appearance once at launch.
    static func applyAppearance() {
        // Navigation bar — solid brand blue with white title
        let navAppearance = UINavigationBarAppearance()
        navAppearance.configureWithOpaqueBackground()
        navAppearance.backgroundColor = UIColor(brandBlue)
        navAppearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        navAppearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        UINavigationBar.appearance().standardAppearance = navAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navAppearance
        UINavigationBar.appearance().compactAppearance = navAppearance
        UINavigationBar.appearance().tintColor = .white

        // Tab bar — selected items in brand orange for contrast on light/dark
        UITabBar.appearance().tintColor = UIColor(brandOrange)
    }
}
