# Premium Videos + $2.99/mo Subscription — Implementation Plan

**Status: PAUSED by client (2026-06-05).**
- ✅ Phase 0 done: the FREE Videos tab is wired to the public playlist `PLCA5F1wp6EynBq4VyPhGawPTbUypB3uRF`.
- ⏸ The $2.99/mo cross-platform subscription is fully planned below but NOT started.
- **To resume:** work the "What YOU must set up" checklist (dev accounts, RevenueCat, Firebase, premium videos),
  then begin Phase 1. Minimum to let a developer start coding/testing on Android without paid accounts:
  a free Firebase project (`google-services.json`) + a RevenueCat account (Android API key).

Big City Plumbing & Heating app (Android + iOS).

## Decisions locked in
- **Paid content host:** Unlisted YouTube videos.
- **Price:** $2.99 / month, auto-renewing.
- **Payments:** MUST use store in-app purchases — Apple StoreKit + Google Play Billing.
  Stripe/PayPal for in-app digital content is against store rules and gets apps rejected.
- **Subscription manager:** RevenueCat (one SDK for both stores; free under ~$2.5k/mo revenue;
  handles receipt validation + "Restore Purchases").

## The hard truths that shaped this plan
1. **Store cut:** Apple/Google take ~15–30% (15% if enrolled in the small-business programs,
   which you should be). On $2.99 that's ~$0.45 → you net ~$2.54.
2. **Unlisted ≠ secure:** anyone with the link can watch on YouTube. Two protection tiers below.
3. **Architecture change:** today the app is free, no login, no backend. A subscription needs
   entitlement tracking and (for real protection) a way to hand out video IDs only to subscribers.

---

## Two build tiers (pick one)

### Tier 1 — No backend (cheapest, simplest, weakest protection)  ⟵ matches "unlisted/cheap"
- Gate the premium screen **client-side** on the RevenueCat `premium` entitlement.
- Premium video IDs live in the app (or in RevenueCat product metadata / a bundled config).
- Risk: a determined user can decompile the app and extract the unlisted IDs. For a $2.99 tier
  this is usually "good enough" deterrence.
- Cost: $0 beyond store + dev accounts.

### Tier 2 — Tiny backend (recommended hardening)
- A serverless function (Firebase Functions or Cloudflare Worker) returns the premium video IDs
  **only** after verifying the caller's RevenueCat entitlement (via RevenueCat REST API/webhook).
- Video IDs are never shipped in the app binary.
- Still not bulletproof (a subscriber can screen-grab a link), but removes casual extraction.
- Cost: ~$0 on free serverless tiers; a few hours more dev.

> Recommendation: ship **Tier 1** first to validate demand, add **Tier 2** if piracy becomes real.

---

## What YOU must set up (I can't do these — they need your accounts/banking)
- [ ] **Apple Developer Program** — $99/yr.
- [ ] **Google Play Console** — $25 one-time.
- [ ] **App Store Connect:** create an auto-renewable subscription (group + product), price tier $2.99,
      localized name/description, review notes, and **sandbox tester** accounts.
- [ ] **Google Play Console:** create a subscription product, base plan $2.99/month.
- [ ] **RevenueCat account:** create entitlement `premium`, map both store products to it, grab API keys.
- [ ] **Firebase project** (free): enable Authentication with Apple + Google + email providers; download
      `google-services.json` (Android) and `GoogleService-Info.plist` (iOS).
- [ ] **Sign in with Apple capability** in App Store Connect (required since we also offer Google login).
- [ ] **Bank + tax info** in both consoles for payouts.
- [ ] **Privacy Policy URL + Terms of Use (EULA) URL** — Apple requires these for subscriptions.
- [ ] Enroll in **App Store Small Business Program** + **Google Play 15% tier** (lower fees).

## What I (Claude Code) will build
**Shared / config**
- `AppConfig`: RevenueCat API keys (Android + iOS), Firebase config, entitlement id `premium`,
  free playlist id, premium video list (Tier 1: bundled list).

**Auth (cross-platform login)**
- Firebase Auth integration on both platforms: **Sign in with Apple** + **Google** + email.
- On login, call `Purchases.logIn(firebaseUid)` so the `premium` entitlement is shared across devices/platforms.
- Sign-out + account screen.

**Android (Kotlin/Compose)**
- Add RevenueCat SDK (`com.revenuecat.purchases:purchases`) + Firebase Auth.
- Initialize Purchases on app start; link to the signed-in user.
- **Login screen** (Apple/Google/email).
- **Paywall screen**: $2.99/mo offering, Subscribe, **Restore Purchases**, required legal text
  (price, period, auto-renew, cancel-anytime) + Privacy/Terms links.
- Gate the premium video list on the `premium` entitlement.
- Premium player: YouTube embed of the unlisted video (WebView, same pattern as current Videos tab).

**iOS (Swift/SwiftUI)**
- RevenueCat SDK (SwiftPM) + Firebase Auth.
- Same login + paywall + gating + player, SwiftUI parity.

**Tier 2 only (optional later)**
- Serverless endpoint that checks RevenueCat entitlement → returns premium video IDs.

## Store compliance checklist (or it gets rejected)
- [ ] **Restore Purchases** button present.
- [ ] Purchase screen shows price, billing period, auto-renew, and how to cancel.
- [ ] Links to **Privacy Policy** and **Terms (EULA)**.
- [ ] "Manage Subscription" deep-links to the store's subscription settings.
- [ ] No outside payment links for the digital subscription.

## Phased rollout
- **Phase 0 (now):** Wire the FREE Videos tab to your real YouTube playlist. ✅ DONE.
- **Phase 1:** **Android** — Firebase Auth login (Apple/Google/email) + RevenueCat plumbing + paywall UI.
- **Phase 2:** Gate premium list; play unlisted videos; sandbox-test the purchase + cross-device restore.
- **Phase 3:** iOS parity — login + paywall + gating (needs a Mac / the Fiverr dev to build + test).
- **Phase 4:** Create store products, connect banking, submit for review.
- **Phase 5 (optional):** Add Tier-2 backend hardening.

## Cost summary (recurring)
| Item | Cost |
|---|---|
| Apple Developer | $99 / year |
| Google Play | $25 once |
| RevenueCat | Free under ~$2.5k/mo revenue |
| Firebase Auth (login) | Free (Spark tier) |
| Backend (Tier 2) | ~$0 (serverless free tier) |
| Video hosting | $0 (unlisted YouTube) |
| Store fee per $2.99 | ~$0.45 (15% tier) → net ~$2.54 |

## Client decisions (recorded)
1. **Free playlist:** `PLCA5F1wp6EynBq4VyPhGawPTbUypB3uRF` — DONE, wired into the Videos tab (Phase 0).
   (Reminder: this playlist must be set to **Public** or **Unlisted** in YouTube, or the embed won't play.)
2. **Developer accounts:** Not created yet → action item before Phase 4. Start Apple enrollment early (can take days).
3. **Tier:** Tier 1 (no backend) to start.
4. **Premium videos:** none recorded yet — will create later.
5. **Login:** wants cross-device.

### Cross-platform = YES (client confirmed: works on Android AND iPhone)
A subscriber must be able to use their subscription across both platforms. Implications:
- The $2.99 is still bought through ONE store (Apple on iPhone / Google on Android) — stores don't share billing.
- Portability comes from a **login**: the user signs in, and RevenueCat ties the `premium` entitlement to that
  account, so signing in on the other platform unlocks the videos with **no second charge**.
- **Auth provider:** **Firebase Authentication** (free Spark tier) — works on both Android & iOS, supports
  Sign in with Apple, Google, and email. The Firebase UID is passed to `Purchases.logIn(uid)` so the entitlement
  follows the user. No custom server required (Firebase is managed).
- **Apple rule:** offering Google sign-in requires also offering **Sign in with Apple** → we ship both.

This keeps us "no custom backend" (Tier 1) but adds a **managed login** layer.

## Still-open for Phase 1
1. Begin Apple ($99/yr) + Google ($25) developer enrollment now — gates RevenueCat product setup + submission.
2. Create a **Firebase project** (free) for Authentication; create a **RevenueCat** account.
   (I can scaffold the login + paywall UI behind config placeholders before keys exist, but it can't be
    purchase-tested until the dev accounts + store products exist.)
