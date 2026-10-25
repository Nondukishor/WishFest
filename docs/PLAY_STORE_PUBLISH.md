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

- Push to `main` / `master`: submits the AAB to **closed testing (`alpha`)** with status **COMPLETED**. Google Play review and app setup requirements still apply.
- Manual: Actions → **Publish to Google Play** → choose track + status (defaults: alpha + COMPLETED)
- Tag push `v1.0.1`: same as push, with `versionName` from the tag

Each run sets a unique `versionCode` from `github.run_number` so Play accepts every upload.

### Required repository secrets

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | Base64-encoded contents of the existing release keystore (see below) |
| `WISHFEST_STORE_PASSWORD` | keystore password |
| `WISHFEST_KEY_ALIAS` | key alias (`wishfest_key`) |
| `WISHFEST_KEY_PASSWORD` | key password |
| `ANDROID_PUBLISHER_CREDENTIALS` | full contents of the Play service-account JSON |

Add secrets under: GitHub repo → **Settings → Environments → `dev` → Environment secrets**.

The workflow job uses `environment: dev`. Environment secrets take precedence over repository secrets with the same name.

### Encode keystore (macOS)

```bash
base64 -i app/wishfest-release-key.jks | pbcopy
```

Paste the copied output into the `KEYSTORE_BASE64` secret, not the command itself. Keep using the existing release key.

To set it directly without copying the wrong text:

```bash
base64 -i app/wishfest-release-key.jks | gh secret set KEYSTORE_BASE64 --repo Nondukishor/WishFest --env dev
```

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

## Production blocked with FAILED_PRECONDITION

WishFest must complete the closed-testing requirement before production access is granted.
The workflow therefore targets `alpha` on branch and tag pushes. In Play Console, use the
closed testing track with the matching identifier (`alpha`); select or create that track
and configure its testers, countries, and required app setup before publishing.

At least 12 testers must opt in continuously for 14 days. Adding their email addresses
alone does not count: share the opt-in link and ask them to join, install, test, and
provide feedback. Internal testing does not satisfy this requirement. After the test,
apply for production access on the Dashboard and wait for approval.

Once production access is approved, manually run this workflow with `track=production`.
Do not rerun an old failed production job to test this fix: reruns use the old workflow.
Push the updated workflow or start a new manual run from the updated branch.

If Play Console still considers the app a draft, select `DRAFT` when manually running
this workflow and finish the initial release and required setup in Play Console.
Do not automatically retry a rejected production release on another track.

Google references:
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/9845334
