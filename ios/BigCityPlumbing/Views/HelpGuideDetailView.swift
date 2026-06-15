import SwiftUI

struct HelpGuideDetailView: View {
    let guide: HelpGuide
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(guide.title)
                    .font(.largeTitle.bold())
                Text(guide.summary)
                    .font(.body)
                    .foregroundStyle(.secondary)

                ForEach(guide.sections) { section in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(section.heading).font(.title3.weight(.semibold))
                        Text(section.body).font(.body)
                    }
                }

                Button {
                    if let url = AppConfig.telURL { openURL(url) }
                } label: {
                    HStack {
                        Image(systemName: "phone.fill")
                        Text("Call \(AppConfig.phoneNumberDisplay)").bold()
                    }
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .foregroundStyle(.white)
                    .background(Theme.brandOrange)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .padding(.top, 8)
            }
            .padding()
        }
        .navigationTitle(guide.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        HelpGuideDetailView(guide: HelpGuideLoader.loadAll().first ?? HelpGuide(
            id: "x", title: "Sample", summary: "Sample summary", icon: "info",
            sections: [GuideSection(heading: "Heading", body: "Body text.")]
        ))
    }
}
