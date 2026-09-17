# Release Checklist — Google Play

What is already done in the repo, what you still have to do, and the exact commands.

## Done in the repo

- [x] `versionName = "1.0.0"`, `versionCode = 1` — [app/build.gradle.kts](../../app/build.gradle.kts)
- [x] Release signing wired to an untracked `keystore.properties` (see `keystore.properties.example`)
- [x] R8 + resource shrinking on for release; release AAB builds clean
- [x] R8 keep rule for Glance `ActionCallback` (widget refresh button survives minification)
- [x] Backup rules for Android ≤ 11 (`backup_rules.xml`) and 12+ (`data_extraction_rules.xml`)
- [x] No runtime permissions (dead daily-reminder toggle + `POST_NOTIFICATIONS` removed until Phase 2 ships it); only WorkManager's normal ones remain
- [x] Room schema exported to `app/schemas` — required for future migrations
- [x] `targetSdk = 37` — above Play's current minimum target
- [x] Lint: no errors; lint-vital passes
- [x] Privacy policy + store listing text in this folder

## One-time setup

1. **Create the upload key** (do this once; back the `.jks` file up off this machine):
   ```
   keytool -genkeypair -v -keystore worddrop-upload.jks -alias worddrop -keyalg RSA -keysize 2048 -validity 10000
   ```
   Put `worddrop-upload.jks` in the project root (git-ignored).

2. **Create `keystore.properties`** in the project root from `keystore.properties.example`. Git-ignored.

3. **Play Console** → Create app → WordDrop, App, Free, Education. Accept declarations.
   Enroll in **Play App Signing** when prompted (default). Your `.jks` is the *upload* key; Google holds the app-signing key.

4. **Publish the privacy policy** somewhere public (GitHub Pages, your site). Play requires a URL.

## Every release

```powershell
.\gradlew.bat clean :app:bundleRelease :app:lintRelease
```

Output: `app\build\outputs\bundle\release\app-release.aab`

Before uploading:
- [ ] Bump `versionCode` (must increase every upload) and `versionName`
- [ ] Install the release build on a real device and smoke-test — R8 bugs only show here:
  ```powershell
  .\gradlew.bat :app:assembleRelease
  adb install -r app\build\outputs\apk\release\app-release.apk
  ```
  Check: onboarding → add widget (both sizes) → widget refresh button → tap widget opens detail → save → quiz → settings changes reflect on widget → kill app, widget still works.
- [ ] Keep `app\build\outputs\mapping\release\mapping.txt` — upload it to Play (Console → App bundle explorer → the release → Downloads → mapping) so crash traces are readable

Play Console:
- [ ] Production (or Internal testing first) → Create new release → upload `.aab`
- [ ] Release notes (see store-listing.md)
- [ ] Review → Roll out

## Still needed from you before first submission

| Item | Where |
|---|---|
| Upload keystore + `keystore.properties` | Project root |
| 512×512 icon PNG — ready at `docs/play/ic_launcher_512.png` | Play Console → Main store listing |
| 1024×500 feature graphic — ready at `docs/play/assets/feature_graphic_1024x500.png` | Play Console → Main store listing |
| Phone screenshots — 6 ready in `docs/play/assets/` (`python tools/play_assets.py` regenerates); optional real app screens via `.\tools\capture_screenshots.ps1` | Play Console → Main store listing |
| Public privacy policy URL | Play Console → App content → Privacy policy |
| Contact email — hello@avirana.com (already in `privacy-policy.md` and the app) | Play Console → Store settings |
| Data safety form | Play Console → App content (answers in store-listing.md) |
| Content rating questionnaire | Play Console → App content |
| Target audience | Play Console → App content |
| Test on a real device (release APK) | — |

## Recommended, not required

- **Internal testing track first.** Upload there, install via the opt-in link on your own phone, then promote to production. Catches signing/AAB issues without a public release.
- **Word bank content licence.** Most entries come from Wiktionary via `tools/build_word_bank.py` (CC BY-SA 3.0). Credit lives in Settings → About and in the store description; keep both if you touch them. To grow the bank later: `python tools/build_word_bank.py --target N` (bumps the bank version so existing installs re-import).
- Google now requires new personal developer accounts to run a closed test with 12+ testers for 14 days before production access is granted. If your account is new, budget for that.
