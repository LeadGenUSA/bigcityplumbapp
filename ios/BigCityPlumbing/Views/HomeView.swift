import SwiftUI

struct HomeView: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        // Hero lives OUTSIDE the ScrollView so its gradient can bleed up behind
        // the status bar (a ScrollView insets its content below the safe area,
        // which is what left the white strip). The rest scrolls below it.
        VStack(spacing: 0) {
            hero
            ScrollView {
                VStack(spacing: 16) {
                    callButton
                    if !AppConfig.offers.isEmpty { offersSection }
                    Text("What can we help with?")
                        .font(.title2.bold())
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)
                    tileGrid
                    Spacer(minLength: 8)
                }
                .padding(.top, 16)
                .padding(.bottom, 16)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .background(Color(.systemBackground))
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(AppConfig.companyName)
                .font(.largeTitle.bold())
                .foregroundStyle(.white)
            Text("Centereach, NY • Licensed & Insured")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.85))
            Text("Plumbing, heating & emergency service.\nTap below to call us 24/7.")
                .font(.body)
                .foregroundStyle(.white)
                .padding(.top, 8)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        // Gradient is the background so it can bleed up behind the status bar
        // (removing the white strip at the top) while the text stays in the
        // safe area.
        .background(
            LinearGradient(
                colors: [Theme.brandBlue, Theme.brandBlueDark],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
    }

    private var callButton: some View {
        Button {
            if let url = AppConfig.telURL { openURL(url) }
        } label: {
            HStack {
                Image(systemName: "phone.fill")
                Text("Call \(AppConfig.phoneNumberDisplay)")
                    .font(.title3.bold())
            }
            .frame(maxWidth: .infinity, minHeight: 56)
            .foregroundStyle(.white)
            .background(Theme.brandOrange)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .padding(.horizontal)
    }

    private var offersSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Special Offers")
                .font(.title2.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)
            Text("Tap a coupon to call and redeem.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(AppConfig.offers) { offer in
                        CouponCard(offer: offer) {
                            if let url = AppConfig.telURL { openURL(url) }
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    private var tileGrid: some View {
        let cols = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]
        return LazyVGrid(columns: cols, spacing: 12) {
            NavigationLink(destination: ServiceRequestView()) {
                Tile(title: "Service Request", subtitle: "Schedule a visit", systemImage: "wrench.and.screwdriver.fill")
            }
            NavigationLink(destination: HelpGuidesView()) {
                Tile(title: "Help Guides", subtitle: "DIY tips & advice", systemImage: "info.circle.fill")
            }
            NavigationLink(destination: VideoHubView()) {
                Tile(title: "Video Hub", subtitle: "Watch our videos", systemImage: "play.rectangle.fill")
            }
            NavigationLink(destination: PipePuzzleView()) {
                Tile(title: "Pipe Puzzle", subtitle: "Quick game break", systemImage: "puzzlepiece.fill")
            }
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
    }
}

private struct Tile: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        VStack(alignment: .leading) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Theme.brandBlue.opacity(0.15))
                    .frame(width: 44, height: 44)
                Image(systemName: systemImage)
                    .foregroundStyle(Theme.brandBlue)
                    .font(.system(size: 22, weight: .semibold))
            }
            Spacer(minLength: 6)
            Text(title).font(.headline)
            Text(subtitle).font(.subheadline).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, minHeight: 140, alignment: .leading)
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(.secondarySystemBackground))
        )
    }
}

private struct CouponCard: View {
    let offer: Coupon
    let onRedeem: () -> Void

    var body: some View {
        Button(action: onRedeem) {
            VStack(alignment: .leading, spacing: 0) {
                // orange "ticket stub" header with the headline discount
                Text(offer.discount)
                    .font(.title.weight(.black))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(colors: [Theme.brandOrange, Theme.brandOrangeDark],
                                       startPoint: .leading, endPoint: .trailing)
                    )
                VStack(alignment: .leading, spacing: 8) {
                    Text(offer.title).font(.headline)
                    Text(offer.details).font(.caption).foregroundStyle(.secondary)
                    HStack {
                        Text(offer.code)
                            .font(.callout.bold())
                            .foregroundStyle(Theme.brandOrangeDark)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Theme.brandOrange.opacity(0.08))
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Theme.brandOrange, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        Spacer()
                        HStack(spacing: 4) {
                            Image(systemName: "phone.fill").font(.caption)
                            Text("Redeem").font(.callout.weight(.semibold))
                        }
                        .foregroundStyle(Theme.brandBlue)
                    }
                }
                .padding(16)
            }
            .frame(width: 260, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.secondarySystemBackground))
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack { HomeView() }
}
