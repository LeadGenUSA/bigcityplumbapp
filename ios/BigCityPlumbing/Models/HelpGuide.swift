import Foundation

struct GuideSection: Codable, Identifiable, Hashable {
    var id: String { heading }
    let heading: String
    let body: String
}

struct HelpGuide: Codable, Identifiable, Hashable {
    let id: String
    let title: String
    let summary: String
    let icon: String
    let sections: [GuideSection]
}

private struct HelpGuidesFile: Codable {
    let guides: [HelpGuide]
}

enum HelpGuideLoader {
    /// Loads the bundled `help_guides.json` from the app bundle.
    /// Returns an empty array on any decode failure (defensive).
    static func loadAll() -> [HelpGuide] {
        guard let url = Bundle.main.url(forResource: "help_guides", withExtension: "json") else {
            assertionFailure("help_guides.json missing from app bundle")
            return []
        }
        do {
            let data = try Data(contentsOf: url)
            let decoded = try JSONDecoder().decode(HelpGuidesFile.self, from: data)
            return decoded.guides
        } catch {
            assertionFailure("Failed to decode help_guides.json: \(error)")
            return []
        }
    }

    static func find(id: String) -> HelpGuide? {
        loadAll().first { $0.id == id }
    }
}
