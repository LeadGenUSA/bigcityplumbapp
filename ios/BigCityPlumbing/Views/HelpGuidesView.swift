import SwiftUI

struct HelpGuidesView: View {
    private let guides = HelpGuideLoader.loadAll()

    var body: some View {
        List(guides) { guide in
            NavigationLink(destination: HelpGuideDetailView(guide: guide)) {
                HStack(spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(Theme.brandBlue.opacity(0.15))
                            .frame(width: 44, height: 44)
                        Image(systemName: Self.systemImage(for: guide.icon))
                            .foregroundStyle(Theme.brandBlue)
                            .font(.system(size: 22, weight: .semibold))
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(guide.title).font(.headline)
                        Text(guide.summary).font(.subheadline).foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .listStyle(.plain)
        .navigationTitle("Help Guides")
    }

    private static func systemImage(for icon: String) -> String {
        switch icon {
        case "snowflake": return "snowflake"
        case "drain":     return "drop.fill"
        case "flame":     return "flame.fill"
        case "droplet":   return "drop.triangle.fill"
        case "phone":     return "phone.fill"
        default:          return "info.circle"
        }
    }
}

#Preview {
    NavigationStack { HelpGuidesView() }
}
