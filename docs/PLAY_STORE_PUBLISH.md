# Automate Google Play publishing (WishFest)

This project uses **Gradle Play Publisher** + an optional **GitHub Actions** workflow to upload signed App Bundles (`.aab`) to Play Console tracks.

> Important: Google requires the **first** AAB/APK for a new app to be uploaded **manually** in Play Console. Automation works for every release after that.

## One-time Play Console / GCP setup

1. Create (or open) the app in [Google Play Console](https://play.google.com/console).
2. Manually upload the first release AAB once (`./gradlew :app:bundleRelease`).
3. In [Google Cloud Console](https://console.cloud.google.com/):
   - Create/select a GCP project
   - Enable **Google Play Android Developer API**
   - Create a **Service Account** → Keys → Add key → JSON
4. In Play Console → **Users and permissions** → Invite the service account email  
   Grant access to your app with at least:
   - **Release to testing tracks** (and Production if you want CI to ship prod)
   - **View app information**
5. Save the JSON key as `play-service-account.json` in the **repo root** (gitignored),  
   or put the raw JSON into the `ANDROID_PUBLISHER_CREDENTIALS` env / CI secret.

## Local publish

```bash
# Internal testing track, draft release (safe default)
./scripts/publish-play.sh

# Closed testing
./scripts/publish-play.sh alpha

# Production as draft (review in Console before rolling out)
./scripts/publish-play.sh production DRAFT

# Production completed (goes live immediately — use carefully)
./scripts/publish-play.sh production COMPLETED
```

Equivalent Gradle command:

```bash
./gradlew :app:publishReleaseBundle \
  --no-configuration-cache \
  -Pplay.track=internal \
  -Pplay.releaseStatus=DRAFT
```

Release signing uses `keystore.properties` or:

- `WISHFEST_STORE_FILE`
- `WISHFEST_STORE_PASSWORD`
- `WISHFEST_KEY_ALIAS`
- `WISHFEST_KEY_PASSWORD`

## GitHub Actions (fully automated)

Workflow: `.github/workflows/publish-play.yml`

**Triggers**

- Manual: Actions → **Publish to Google Play** → choose track + status
- Tag push: `v1.0.1`, `v2.0.0`, … (uploads to **internal** as **DRAFT**)

### Required repository secrets

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -i app/wishfest-release-key.jks \| pbcopy` (macOS) |
| `WISHFEST_STORE_PASSWORD` | keystore password |
| `WISHFEST_KEY_ALIAS` | key alias (`wishfest_key`) |
| `WISHFEST_KEY_PASSWORD` | key password |
| `ANDROID_PUBLISHER_CREDENTIALS` | full contents of the Play service-account JSON |

Add secrets under: GitHub repo → **Settings → Environments → `dev` → Environment secrets**.

The workflow job uses `environment: dev`, so repository-level secrets alone are not enough.

### Encode keystore (macOS)

```bash
base64 -i app/wishfest-release-key.jks | pbcopy
```

Paste into the `KEYSTORE_BASE64` secret (no newlines if possible).

## Version bumps

Before each store upload, bump in `app/build.gradle.kts`:

- `versionCode` (must increase every Play upload)
- `versionName` (user-visible)

Play rejects uploads that reuse an existing `versionCode`.

## Tracks cheat sheet

| Track | Typical use |
| --- | --- |
| `internal` | Fast internal testers |
| `alpha` / `beta` | Closed / open testing |
| `production` | Public release |

Prefer `DRAFT` status from CI, then press **Roll out** in Play Console after review.
