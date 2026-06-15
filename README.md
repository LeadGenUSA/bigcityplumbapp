# Big City Plumbing — Native Mobile Apps

Two separate native codebases that ship the same product on both stores:

- **iOS** — Swift / SwiftUI (Xcode 15+, iOS 16+)
- **Android** — Kotlin / Jetpack Compose (Android Studio Hedgehog+, minSdk 24 / Android 7.0)

```
BigCityPlumbingApp/
├── ios/                          # Swift / SwiftUI project
│   ├── project.yml               # XcodeGen spec (generates the .xcodeproj)
│   └── BigCityPlumbing/          # Swift source
├── android/                      # Kotlin / Jetpack Compose project
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── app/                      # App module source
└── shared/
    └── help_guides.json          # Content shared by both apps
```

Both apps ship the six contracted features:

1. **Pipe Puzzle game** — 5×5 tile rotation puzzle with BFS-based solve detection.
2. **Video Hub** — embedded YouTube playlist (WKWebView on iOS, WebView on Android).
3. **Help Guides** — 5 starter topics, sourced from `shared/help_guides.json`.
4. **Click-to-call** — `tel:` URL on iOS, `ACTION_DIAL` Intent on Android.
5. **Service request form** — opens the user's mail app pre-filled (no backend required).
6. **Branding & UI** — blue (#1976D2) + orange (#FF6F00), shared design language.

---

## Before you build: customize for your business

There is exactly one config file per platform. Edit only these:

| Platform | File | What to update |
|---|---|---|
| iOS | `ios/BigCityPlumbing/Config/AppConfig.swift` | Phone, email, YouTube channel/playlist IDs |
| Android | `android/app/src/main/java/com/bigcityplumbing/config/AppConfig.kt` | Same fields |

Also update the bundle identifier / application ID before submission:

| Platform | File | What to change |
|---|---|---|
| iOS | `ios/project.yml` → `PRODUCT_BUNDLE_IDENTIFIER` | e.g. `com.bigcityplumbing.app` |
| iOS | `ios/project.yml` → `DEVELOPMENT_TEAM` | Your 10-character Apple Team ID |
| Android | `android/app/build.gradle.kts` → `applicationId` | e.g. `com.bigcityplumbing` |

App icon placeholders are wired up but plain. Drop a 1024×1024 PNG into:

- iOS: `ios/BigCityPlumbing/Assets.xcassets/AppIcon.appiconset/`
- Android: replace the foreground vector at `android/app/src/main/res/drawable/ic_launcher_foreground.xml` (or generate proper bitmaps via Android Studio → New Image Asset).

---

## Build the iOS app

You need a **Mac** with Xcode 15+. The project is generated from `ios/project.yml` via [XcodeGen](https://github.com/yonaskolb/XcodeGen) — a single one-time install.

```bash
# 1. Install XcodeGen (once per machine)
brew install xcodegen

# 2. Generate the Xcode project
cd ios
xcodegen

# 3. Open it
open BigCityPlumbing.xcodeproj
```

Then in Xcode:

1. Select the **BigCityPlumbing** target → *Signing & Capabilities* → pick your team.
2. Pick a simulator (e.g. iPhone 15) and press ⌘R to run.
3. To submit: *Product → Archive*, then *Distribute App → App Store Connect*.

> If you'd rather skip XcodeGen, in Xcode do *File → New → Project → iOS App*, then drag the `BigCityPlumbing` folder into the project (check *Copy items if needed* and *Create groups*). Make sure `help_guides.json` is added to the *Copy Bundle Resources* phase.

---

## Build the Android app

You need [Android Studio](https://developer.android.com/studio) (Hedgehog 2023.1.1+).

```bash
# 1. Open Android Studio → Open
#    Pick the BigCityPlumbingApp/android folder
# 2. Android Studio offers to install missing SDK + Gradle wrapper. Accept.
# 3. Press ▶ to run on an emulator or connected device.
```

To build a release bundle for the Play Store:

```bash
cd android
./gradlew bundleRelease
# Output: android/app/build/outputs/bundle/release/app-release.aab
```

Before publishing you must:

1. Generate a release keystore: `keytool -genkey -v -keystore my-release-key.keystore -alias bigcity -keyalg RSA -keysize 2048 -validity 10000`
2. Add a real `signingConfig` for `release` in `app/build.gradle.kts` (currently signs with debug keys for convenience).

---

## App Store deployment checklist

### Apple App Store ($99/year)
1. Sign in to [App Store Connect](https://appstoreconnect.apple.com) — make sure the client owns the developer account (per contract).
2. *My Apps → +* → New iOS App.
3. Bundle ID must match `PRODUCT_BUNDLE_IDENTIFIER` in `project.yml`.
4. Fill out App Privacy questionnaire — this app collects no personal data, only opens external apps (mail, phone, YouTube). Disclose accordingly.
5. Provide a privacy policy URL (Apple requires one even if you collect nothing). The contract notes Termly or a lawyer; a simple template is fine for v1.
6. Upload archive from Xcode. Submit for review.

### Google Play Store ($25 one-time)
1. Sign in to [Play Console](https://play.google.com/console). Pay the one-time fee.
2. Create app → fill out Store listing, Content rating, Target audience, Data safety.
3. Upload the `.aab` from `bundleRelease`.
4. Same privacy policy URL as iOS.

---

## What's intentionally NOT included

- **Push notifications** — not in the contract scope and would add backend cost.
- **User accounts / login** — same.
- **Analytics SDKs** — left to client to add (Firebase Analytics is the usual pick).
- **Real backend** — the service request form uses the device's mail app, which is a $0 solution. Replace with an HTTP POST to your CRM later if needed.
- **Custom app-store assets** — screenshots, marketing copy, and feature graphics are part of submission and were not contracted.

## Troubleshooting

- **Android: "SDK location not found"** — open the project once in Android Studio so it can write `local.properties` with your SDK path. Don't commit that file.
- **iOS: code signing errors** — set your *Team* in *Signing & Capabilities*, and make sure the bundle identifier is unique on your account.
- **YouTube embed shows "Video unavailable"** — the channel's uploads must be public, and embedding must be enabled in YouTube Studio settings.

---

## Contract notes

This codebase fulfills the deliverables in *Final_Mobile_App_Contract.docx* (4-8-2026). All ownership transfers to the client upon full payment per the contract. The freelancer assists with store deployment (uploading builds, walking through review steps) but the client must maintain Apple ($99/year) and Google ($25 one-time) developer accounts and pay associated fees.
