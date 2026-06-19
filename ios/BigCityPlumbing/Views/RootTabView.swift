import SwiftUI

struct RootTabView: View {
    @State private var selection = 0

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack { HomeView() }
                .tag(0)
                .tabItem { Label("Home", systemImage: "house.fill") }

            // The game runs fullscreen (its NavigationStack hides the tab bar);
            // its in-game Home button switches back to the Home tab.
            NavigationStack { PipePuzzleView(onExit: { selection = 0 }) }
                .tag(1)
                .tabItem { Label("Game", systemImage: "puzzlepiece.fill") }

            NavigationStack { VideoHubView() }
                .tag(2)
                .tabItem { Label("Videos", systemImage: "play.rectangle.fill") }

            NavigationStack { HelpGuidesView() }
                .tag(3)
                .tabItem { Label("Guides", systemImage: "book.fill") }

            NavigationStack { ServiceRequestView() }
                .tag(4)
                .tabItem { Label("Service", systemImage: "wrench.and.screwdriver.fill") }
        }
        .tint(Theme.brandOrange)
    }
}

#Preview {
    RootTabView()
}
