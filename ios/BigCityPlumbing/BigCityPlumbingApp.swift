import SwiftUI

@main
struct BigCityPlumbingApp: App {
    init() {
        // Brand the navigation/tab bars globally
        Theme.applyAppearance()
    }
    var body: some Scene {
        WindowGroup {
            RootTabView()
                .preferredColorScheme(nil) // honor system setting
        }
    }
}
