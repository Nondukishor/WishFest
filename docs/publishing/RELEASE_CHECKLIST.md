# WishFest publishing handoff

Prepared for Google Play, 24 October 2026. No upload, review submission or production rollout has been performed.

## Build

Run from the repository root:

```sh
./scripts/build_release.sh --offline
```

Omit --offline if dependencies need downloading. Requires the configured Android SDK, NDK 28.2.13676358 and the project's Gradle Java toolchain.

Outputs:
- app/build/outputs/bundle/release/app-release.aab — upload bundle for Play Console.
- app/build/outputs/apk/release/app-release.apk — signed sideload/test build.
- app/build/reports/lint-results-release.html — complete lint findings.
- app/build/outputs/mapping/release/mapping.txt — retain for crash symbolication if generated.
- release-artifacts/verification.json — sizes, hashes, signature-check completion and native segment alignment.

Package: com.nipu.wishfest. Version: 1.0 (code 2). Minimum Android: API 29. Target: API 37. ABIs: arm64-v8a and x86_64. No AI hardware feature is required in the manifest. Play Console confirmed that code 1 was already used, so this upload uses code 2. Increment versionCode again for subsequent uploads. Confirm the existing package ID is the intended permanent ID.

## Signing

The existing release key is preserved. Passwords have moved out of build.gradle.kts into ignored keystore.properties with local-only file permissions. The key itself remains at app/wishfest-release-key.jks and is ignored. Do not commit, email or include either secret file in a public release archive. Back up the keystore and credentials securely; losing them can prevent updates.

For a clean checkout, copy keystore.properties.example to keystore.properties and fill it locally, or provide WISHFEST_STORE_FILE, WISHFEST_STORE_PASSWORD, WISHFEST_KEY_ALIAS and WISHFEST_KEY_PASSWORD. The store path resolves from the project root. Release assembly/bundling fails if signing credentials are missing. Use Play App Signing; confirm the certificate matches the existing upload key if this app is already registered. Treat the old source-embedded passwords as exposed to anyone who had source access; review key/password handling before public repository publication. Do not replace a registered signing identity without a migration plan.

## Required Play Console work

- Create/select the app and confirm developer identity, app ownership and package ID.
- Use pro.nipu@gmail.com as the public support/privacy email.
- Review STORE_LISTING.md and paste the listing fields.
- Host docs/privacy_policy.html on an active, publicly accessible, non-geofenced HTTPS page and put its URL in Play Console. The app also contains an offline-readable copy.
- Prepare a 512×512 PNG store icon, 1024×500 feature graphic, and at least two genuine app screenshots matching Play's current specifications. Existing logo artwork is in app/src/main/assets/logo.png; it is not itself a validated store submission asset.
- Review DATA_SAFETY.md and complete Data safety, app access, ads, audience, content-rating, pricing and distribution declarations. No account is needed to use the app.
- Confirm rights to the logo, artwork, template content and all third-party components. Review SDK and open-source license obligations.
- Assess the limited-scope AI drafting exception; implement an in-app reporting flow if required for the final product scope.
- Upload the signed AAB to internal testing first and review Play's automated/pre-launch reports.
- Complete any closed-testing and developer-verification gates shown for your account before requesting production access.
- Check all open items before submitting for review and choosing production rollout.

## Current validation result

The final signed release build succeeded. All 13 unit tests passed. Release lint reported 0 errors and 58 non-fatal warnings. APK and AAB signatures verified; APK ZIP alignment and every native library's ELF load-segment alignment passed the 16 KB checks. Both artifacts contain the offline privacy policy. The merged release manifest is not debuggable, disables backup and has no required AI hardware features.

A signed-release installation was attempted with `adb install -r` on the connected device. Android rejected it with INSTALL_FAILED_UPDATE_INCOMPATIBLE because the existing com.nipu.wishfest installation has a different signing certificate. The installed app was not uninstalled and its data was preserved. Use a separate test device or a compatible signed installation for release runtime testing. If the device's installed package originated from an already-published app, compare the correct upload/app-signing certificates in Play Console before uploading; do not infer that this local key matches that published app.

## Release-device checks (not completed by the local build)

- Install the signed release on a clean supported device. Test Android 10 and a current Android version, a small screen, large font sizes and a 16 KB page-size environment.
- Verify templates, blank creation, message editing, all festival frames and the bottom-right watermark.
- Test missing AI/AICore and airplane mode: offline wishes and editing must still work.
- Test available AI on a supported device and failure/cancellation paths.
- Import a photo; undo/redo; save and reopen a draft; duplicate and delete projects.
- Save to Photos and share all three output sizes; return to the editor and confirm edits survive.
- Read the in-app privacy policy and verify the hosted copy matches it.
- Test backgrounding, process death and relaunch. Current navigation retains the active card in memory; process recreation can return the user to Home. Persisted drafts can be reopened from My cards.
- Check system navigation, keyboard, edge-to-edge insets and accessibility with TalkBack.

## Validation boundaries

A successful build, unit tests, lint and ELF/ZIP alignment checks do not replace runtime tests, Play Console checks or store-policy approval. Non-fatal lint findings (including launcher artwork sizing and dependency update suggestions) remain visible in the report. Store screenshots and graphics, hosted policy URL, account declarations, device checks and Play review are still required.

References checked for this handoff:
- Target API: https://support.google.com/googleplay/android-developer/answer/11926878
- 16 KB native support: https://developer.android.com/guide/practices/page-sizes
- Listing fields: https://support.google.com/googleplay/android-developer/answer/9859152
- Graphics: https://support.google.com/googleplay/android-developer/answer/9866151
- Privacy: https://support.google.com/googleplay/android-developer/answer/10144311
