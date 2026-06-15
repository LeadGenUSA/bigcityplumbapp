import SwiftUI

struct RootTabView: View {
    var body: some View {
        TabView {
            NavigationStack { HomeView() }
                .tabItem { Label("Home", systemImage: "house.fill") }

            NavigationStack { PipePuzzleView() }
                .tabItem { Label("Game", systemImage: "puzzlepiece.fill") }

            NavigationStack { VideoHubView() }
                .tabItem { Label("Videos", systemImage: "play.rectangle.fill") }

            NavigationStack { HelpGuidesView() }
                .tabItem { Label("Guides", systemImage: "book.fill") }

            NavigationStack { ServiceRequestView() }
                .tabItem { Label("Service", systemImage: "wrench.and.screwdriver.fill") }
        }
        .tint(Theme.brandOrange)
    }
}

#Preview {
    RootTabView()
}
