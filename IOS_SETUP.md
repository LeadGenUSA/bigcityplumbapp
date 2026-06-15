# iOS Build & Submit — Setup Guide (Cloud Mac route)

You're on Windows, so you need a macOS + Xcode environment to build iOS. This walks the
**cloud Mac (DIY)** path. You already have an Apple Developer account ($99/yr) — good, that's required.

## 1. Get a cloud Mac
- **MacinCloud** (simplest): pick a "Managed Server" or pay-as-you-go plan that has **Xcode pre-installed**
  (saves a ~1-hour Xcode download). ~$20–30/mo or hourly.
- (Alternative: AWS EC2 Mac — more powerful but 24-hour minimum + more setup. Overkill for occasional builds.)
- Connect from Windows using **Microsoft Remote Desktop** (free, Microsoft Store) or the VNC details they give you.

## 2. Get the project onto the Mac
Recommended: use **Git/GitHub** (also makes future updates + any freelancer handoff easy).
- Ask me to `git init` this project and push it to a private GitHub repo, then on the Mac: `git clone <repo>`.
- (Low-tech alternative: zip the `BigCityPlumbingApp` folder, upload through the remote session.)

## 3. One-time Mac tooling
```bash
# Homebrew (if not present)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
# XcodeGen (generates the .xcodeproj from project.yml)
brew install xcodegen
```
Sign Xcode into your Apple ID: **Xcode ▸ Settings ▸ Accounts ▸ +** → add your Apple Developer account.

## 4. Generate & open the project
```bash
cd ios
xcodegen generate          # creates BigCityPlumbing.xcodeproj
open BigCityPlumbing.xcodeproj
```

## 5. Signing
- Select the **BigCityPlumbing** target ▸ **Signing & Capabilities**.
- Check **Automatically manage signing**, choose your **Team**.
- Bundle ID is already `com.bigcityplumbing.app`.
- Put your **Team ID** (developer.apple.com ▸ Account ▸ Membership ▸ 10-char Team ID) into
  `ios/project.yml` → `DEVELOPMENT_TEAM:` so it survives re-generation. (I can set this for you — just paste it.)

## 6. Build & run
- First run on the **iOS Simulator** (no device needed) — this is the real compile test.
- If you hit Swift build errors, send me the exact text — I'll fix them (the iOS code has never been
  compiled before, so a few may surface despite the pre-flight).
- To run on a physical iPhone: plug it in (or use the cloud provider's device options), trust it, pick it, Run.

## 7. TestFlight / App Store
- In **App Store Connect**, create the app record (bundle id `com.bigcityplumbing.app`).
- In Xcode: **Product ▸ Archive ▸ Distribute App ▸ App Store Connect ▸ Upload**.
- Use **TestFlight** to install on real iPhones without full review; submit for review when ready.

---

## Pre-flight fixes already applied (Windows side, by Claude)
- **`Info.plist`: `armv7` → `arm64`.** As-shipped it would have been *blocked from installing on iPhone 8/X and
  newer* (those dropped 32-bit). Critical fix.
- **App icon generated** (`AppIcon-1024.png`, your logo on brand navy) and wired into the asset catalog —
  the App Store rejects uploads with no 1024×1024 icon.
- **VideoHub WebView reload loop** fixed (load-once) — YouTube redirects the URL, which caused repeated reloads.
- **Deleted unused `Models/PipeGame.swift`** (old native game; the game is now the bundled HTML/WebView).
- **Synced the game** (`Resources/game/index.html`) to the current version (water flow, logo ball, new art).

## Known, non-blocking notes
- iOS **Home tab coupons section** — DONE (now matches Android: same "Special Offers" cards, tap-to-call).
- `Resources/levels.json` is now unused (only the deleted native game read it) — harmless; can be removed later.
- `DEVELOPMENT_TEAM` in `project.yml` is still blank — set it (step 5).
- I could not *compile* this from Windows; the above clears the obvious risks, but the first Simulator build is
  the real verification. Send me any errors.
