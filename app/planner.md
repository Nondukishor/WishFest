# Festival Wishes — Android Application Plan

> Status: implementation blueprint, not a working application. Policy and service research checked on 24 October 2026. Recheck linked requirements and Play Console notices before submission.

Create beautiful, editable festival greeting cards for Durga Puja, Kali Puja, Eid al-Fitr, Eid al-Adha, and other celebrations. Personalize text, photographs, backgrounds and decorations, save projects locally, and share finished images through Android's sharing system.

## Contents

1. Product scope and user journeys
2. Technology and architectural decisions
3. Modules and repository structure
4. Document format and local storage
5. Rendering, editing and sharing
6. Optional free services
7. Security, privacy and accessibility
8. Engineering standards and validation
9. Delivery roadmap
10. Google Play publishing checklist
11. Setup, costs and open decisions
12. Official references

## 1. Product scope

### Principles

- C++ owns the portable editing engine, domain model, commands and validation.
- No database: no Room, SQLite, Firebase, Supabase, or remote database.
- Offline creation, editing, project recovery, export and sharing are mandatory.
- No login, account, social-media credentials, backend, advertising or tracking in the first release.
- Optional network services must improve the app without becoming prerequisites.
- Respect religious imagery and cultural context; human-review every bundled greeting and illustration.

### Release 1

| Area | Planned behavior |
| --- | --- |
| Festival discovery | Category browser, text search, language/style filters; favorites saved locally |
| Template library | At least 5 original templates per launch festival across 4 launch categories; include light, elegant, colorful and photo-card styles |
| Editor | Editable greeting, sender/recipient name, font, size, color, alignment, background color/gradient/photo, stickers and frames |
| Layer tools | Select, drag, resize, rotate, reorder, duplicate, delete, lock and show/hide; alignment guides and safe margins |
| History | Undo/redo; coalesce one drag gesture into one undo action |
| Projects | Autosave, reopen, rename, duplicate and delete; recover after process death |
| Export | PNG and JPEG; square 1080×1080, portrait 1080×1350 and story 1080×1920 presets |
| Sharing | Android Sharesheet for Instagram, Facebook, X, WhatsApp and any compatible installed app |
| Localization | English and Bengali UI initially; Bengali, English and Arabic text input/rendering test cases |
| Settings | Theme, language, export preference, cache controls, delete local data, privacy policy and licenses |

Preset dimensions are convenient creative choices, not promises about every social platform's current specifications. Let users preview cropping. Provide copy-caption and save-image alternatives if the receiving app ignores captions or offers no compatible share target.

### Later releases

Downloadable static template packs; optional stock-photo search; more festivals and languages; local reminders if users request them; user-triggered project backup/import. Defer AI generation, video cards, social login, community uploads, cloud sync, subscriptions and direct platform posting until validated demand justifies complexity.

### Main journeys

1. Open app → choose festival → preview template → customize → preview export → share/save.
2. My Projects → reopen draft → restore assets → continue editing → export.
3. Replace background → system photo picker → crop → apply → autosave an app-private copy.
4. Optional online search → choose a credited photo → download within provider terms → edit; offline fallback remains available.

### Screens and states

Home; festival results; template preview; editor with contextual bottom sheets; export preview; project library; settings/about. Design empty, loading, offline, retry, missing-font, unsupported-image, permission-denied, disk-full and export-cancelled states. Opening a template creates a user-owned project; bundled originals never change.

## 2. Technology and architecture

### Recommended stack

| Concern | Decision |
| --- | --- |
| Portable application core | C++20, Android NDK, CMake and standard library |
| Android shell | Kotlin, a single Activity, Jetpack Compose and ViewModels |
| Integration | Small JNI bridge with explicit ownership and versioned DTOs |
| Drawing | Shared Android Canvas renderer for editor scene and bitmap export; Compose provides controls |
| Complex scripts | Android text layout/shaping through StaticLayout/TextPaint; bundled licensed fallback fonts |
| Storage | Versioned UTF-8 JSON and ordinary app-private files; SharedPreferences for small preferences |
| Networking | Optional Kotlin HTTP adapter; no networking in the C++ domain |
| Distribution | Signed Android App Bundle; arm64-v8a release and x86_64 development/testing |
| Compatibility | Proposed minSdk 29; compileSdk/targetSdk at least 36, subject to current release requirements |

**C++ native does not mean every Android component can sensibly be C++.** Android UI, keyboard/IME, accessibility, lifecycle, photo selection, content URIs and sharing work best through Android APIs. This plan uses C++ as the core technology with Kotlin platform adapters. If 100% C++ is a strict requirement, evaluate Qt Quick/QML and its distribution/license implications before implementation; expect Android bridges to remain necessary. Do not start with NativeActivity for this form-heavy editor.

Keep the first renderer on Android rather than introducing a large graphics engine. If a future cross-platform renderer is needed, evaluate Skia plus HarfBuzz/FreeType separately and preserve the existing scene contract.

### Dependency rule

UI → application operations → domain. Android adapters implement file, rendering and sharing boundaries. The pure C++ domain imports no Android, JNI, filesystem implementation, HTTP client or UI types.

C++ is the authoritative owner of the editable document and command history. ViewModels hold immutable presentation snapshots and transient UI state; never maintain a second editable document in Kotlin. A single document executor serializes mutations. Coalesce UI notifications to frame cadence; avoid JNI calls for each layer on every frame.

### Responsibilities

| Layer | Owns | Must not own |
| --- | --- | --- |
| Domain | CardDocument, Layer, transforms, constraints, edit commands, undo history | Android context, file paths, drawing widgets |
| Application | CreateProject, ApplyEdit, LoadProject, SaveProject, ExportCard orchestration | Activity lifecycle or SDK objects |
| Native bridge | Handle registry, DTO conversion, validation and error mapping | Business decisions |
| Android adapters | Atomic files, bitmap rendering/encoding, picker, Sharesheet, optional HTTP | Duplicated domain rules |
| Presentation | Navigation, gestures, controls, loading/error states | Raw native pointers or direct JSON mutation |

Return structured Result/Error values. An operation can report InvalidDocument, MissingAsset, UnsupportedVersion, StorageFull, Cancelled or RenderFailed. Kotlin maps these to localized user messages.

## 3. Proposed repository

```text
festival-wishes/
├── README.md
├── app/                         # Android entry point and dependency composition
├── android/
│   ├── presentation/            # Compose screens, ViewModels, navigation
│   ├── platform/                # Picker, storage, sharing, lifecycle integrations
│   └── rendering/               # Canvas scene renderer and bitmap exporter
├── native/
│   ├── domain/include/wishes/
│   ├── domain/src/
│   ├── application/include/wishes/
│   ├── application/src/
│   ├── bridge/                  # JNI only
│   ├── serialization/           # Versioned JSON mapping and migrations
│   └── tests/
├── content/
│   ├── festivals.json
│   ├── templates/
│   ├── backgrounds/
│   ├── stickers/
│   ├── fonts/
│   └── licenses/                # Source URLs, licenses, attribution records
├── schemas/                     # Document/template/pack JSON Schema
├── tools/                       # Template validation and thumbnail generation
├── docs/                        # ADRs, privacy inventory, release runbook
├── CMakeLists.txt
├── CMakePresets.json
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

This is a proposed layout, not existing files. Use Gradle modules for presentation/platform/rendering with app as the composition root; use CMake targets for domain/application/serialization/JNI. Keep dependency direction enforceable without splitting every screen into a separate module.

## 4. Document format and database-free persistence

### Entities

Festival: stable ID, localized display names, keywords and content references. Template: ID, version, festival ID, aspect ratio, preview, initial scene and license metadata. CardDocument: ID, schema version, canvas, ordered layers, asset references and timestamps. Layer: stable ID, type, transform, opacity, visibility, lock status and type-specific properties. Asset: ID, local relative path, content hash, MIME type and provenance.

Festival dates are region-dependent, and Eid dates depend on local observation. Categorize by festival without requiring date calculations. Any future calendar shows region, year, source and tentative/confirmed status; never treat a generic holiday API as religious authority.

### Example project

```json
{
  "schemaVersion": 1,
  "id": "project-uuid",
  "templateId": "eid-elegant-01",
  "templateVersion": 1,
  "canvas": {"width": 1080, "height": 1080, "background": "#FFF7E8"},
  "layers": [
    {
      "id": "greeting-uuid",
      "type": "text",
      "transform": {"x": 120, "y": 380, "width": 840, "height": 220, "rotation": 0},
      "opacity": 1,
      "visible": true,
      "locked": false,
      "text": "ঈদ মোবারক",
      "fontId": "noto-sans-bengali",
      "fontSize": 84,
      "color": "#245C42",
      "alignment": "center"
    }
  ],
  "assets": [],
  "updatedAt": "2026-10-24T06:00:00Z"
}
```

Coordinates and typography use the logical canvas units. Store ordered layers back-to-front. Retain template version for provenance, but save a complete editable scene so subsequent template updates cannot silently alter drafts.

```text
files/
  projects/<uuid>/document.json
  projects/<uuid>/document.previous.json
  projects/<uuid>/assets/<content-hash>.<extension>
  projects/<uuid>/thumbnail.webp
  favorites.json
  downloaded-packs/<pack-id>/<version>/
cache/
  previews/
  share/
  optional-api/
```

- Discover drafts by scanning project directories and reading metadata; build the list in memory. No persistent index is required for release 1.
- Autosave after a 750 ms idle debounce and after committed operations; pause/stop flush is best effort, not the only persistence strategy.
- One writer per project. Write to a sibling temporary file, flush/sync, then atomically replace using Android AtomicFile or equivalent backup semantics. Retain last known good data.
- Copy selected photos into project-owned storage immediately; drafts must survive revoked picker access or a removed original.
- Stage new assets before committing document references. Remove orphan assets only after a successful save and reference check.
- Validate schema, finite numeric values, dimensions, unique IDs, relative paths, file sizes and asset hashes. Reject traversal and references outside the project directory.
- Migrate old schemas sequentially with fixtures; back up before migration. Open newer unsupported versions with a clear error, never destructive downgrade.
- OS cache eviction must not remove draft assets. Suggest a 100 MB cache budget with LRU eviction; show project disk usage and user-controlled deletion separately.
- Exclude private drafts/photos from automatic cloud backup by default through Android backup rules. Explain that uninstall removes app-private projects; gallery exports remain.
- Future portable project export uses Storage Access Framework and a data-only ZIP with manifest. Import enforces expanded-size/count limits and rejects symlinks and path traversal.

## 5. Editor, rendering and export

### Scene consistency

Use the same scene renderer and text-layout implementation for editor and export. Preview scales the logical canvas; export renders at selected output dimensions. Store crop, rotation, alignment, clipping and font choices explicitly. Transform hit-test positions through the inverse viewport transform.

Use StaticLayout for multiline/RTL text, correct shaping and line wrapping. Verify Bengali conjuncts, Arabic joining/diacritics, mixed-direction text and emoji fallback. Bundle font files with their licenses; avoid live font downloads. Do not split or truncate strings by byte or individual UTF-16 code unit.

### Native boundary rules

- Create opaque engine IDs using a registry; never expose unchecked native pointers.
- Register JNI methods centrally, document ownership and close engines explicitly.
- Never allow C++ exceptions across JNI. Convert to error DTOs; inspect pending Java exceptions.
- JNIEnv is thread-local. Attach native worker threads correctly and release references and pinned buffers promptly.
- Exchange standard UTF-8 through explicit conversion/byte arrays; do not assume JNI modified UTF-8 is ordinary UTF-8.
- Batch edits and snapshots. Keep image pixels out of JSON and avoid repeated full bitmap copies.
- Cancel background work when its owner closes; reject calls to released handles and suppress stale callbacks.

### Images and memory

Decode selected images to bounded sizes, honor EXIF orientation, and strip location/device metadata from exports. Start with PNG/JPEG/WebP input; report unsupported formats clearly. Downsample source photos for previews and preserve enough local resolution for export. Apply limits before full decode: proposed 25 MB encoded input, 40 megapixel source, 32 editable layers, 8 image layers and 2,000 characters per text layer; adjust after profiling.

1080×1920 RGBA pixels alone occupy about 7.9 MiB; layers, source decoding and additional buffers increase actual memory. Allocate offscreen buffers only during export, release promptly, and handle allocation failure. Keep image decode, file I/O and encoding off the main thread; dispatch thread-affine drawing appropriately.

### Save and share

1. Validate the scene, render to an offscreen bitmap, encode PNG or JPEG and show export progress/cancel.
2. Save to gallery through MediaStore, using pending-state handling and cleanup on failure; no broad storage permission on the proposed API 29+ baseline.
3. For sharing, place a completed export under a narrowly configured FileProvider cache path.
4. Send ACTION_SEND with the correct MIME type, content URI, ClipData where needed and FLAG_GRANT_READ_URI_PERMISSION through the system chooser.
5. Retain the shared file long enough for receivers to read; clean stale exports later rather than immediately after opening the chooser.
6. Offer copy-caption and save-image actions. A chooser selection does not prove a post was published.

Do not request Facebook/Instagram/X credentials or implement direct posting in release 1. No social platform API is needed for ordinary image sharing. Do not request QUERY_ALL_PACKAGES to build a social-app list.

## 6. Optional free services

**Recommended launch: no API dependency.** Ship original backgrounds, gradients, frames, messages and licensed fonts. A curated local catalog gives reliable festival relevance and avoids keys, quotas and unexpected content.

| Option | Useful for | Constraints and decision |
| --- | --- | --- |
| Pexels API | Optional photo background search | Free default quota currently 200 requests/hour and 20,000/month per key. Preserve provider/photographer attribution, respect terms and avoid recreating a stock-photo service. Higher quotas are not guaranteed. |
| Pixabay API | Possible photo/illustration search | Current default 100 requests/60 seconds; response caching for 24 hours; no systematic mass downloads. Permanent image hotlinking is disallowed, and documented permanent-use guidance references server downloads. Defer until the intended mobile storage flow is confirmed with the provider. |
| Static HTTPS catalog | Optional owned template updates | Host versioned JSON plus licensed assets on static hosting. No database needed. Hosting has quotas and is not guaranteed free forever. |
| Bundled licensed fonts | Bengali, Arabic and Latin typography | Package actual font files with required license notices; no runtime font API required. |

Pexels integration is an optional candidate, not a guarantee of approval for this app's exact flow. Review current API terms before enabling image search and check suitability of results for religious themes.

A secret in native code, BuildConfig, resources, encrypted strings or the APK is still extractable. Preferred choices: omit stock APIs for launch; or later use a minimal serverless proxy with provider-secret storage and abuse controls, without a database. The proxy introduces hosting, logging and operational obligations and cannot be assumed free. Direct client use requires provider permission and acceptance of key exposure; never present obfuscation as protection.

For optional HTTP: HTTPS only, allowlisted domains, explicit timeouts, bounded responses, cancellation, cache-first behavior, at most limited retry with backoff, and Retry-After handling for 429. Never upload users' photos, names, drafts or greeting text for background search. Downloaded packs are data only—no executable code or scripts. Validate manifests, hashes, versions, size budgets and extraction paths; keep a known-good bundled fallback.

## 7. Privacy, accessibility and content quality

- Collect no personal data or analytics in the baseline build. Maintain a data-flow inventory rather than assuming no database means no collection.
- Third-party image requests expose network information such as IP address to providers; proxies, crash SDKs and hosting logs also affect disclosures.
- Use Android Photo Picker with Activity Result contracts and platform-supported fallback. Request only user-selected images.
- Do not request contacts, location, microphone, call logs, SMS, all-files access or broad photo/video permissions.
- Release 1 needs no notification permission. Add opt-in notifications only with a concrete reminder feature and updated policy review.
- Provide in-app deletion for projects, downloaded content and all local data; distinguish retained gallery exports.
- Never log card text, names, private image paths, tokens or photo metadata.
- Provide TalkBack labels, predictable focus, 48 dp touch targets, sufficient contrast, font-scaling support, dark mode and text alternatives for icon buttons.
- Offer numeric transform controls and layer-list actions for users unable to perform drag/rotate gestures. Accessibility font scaling affects controls, not the intended exported artwork.
- Review religious iconography, spelling, greeting suitability, font coverage and respectful cropping. Do not claim official religious endorsement.
- Record asset origin, creator, license, redistribution rights and attribution obligations. Do not copy social-media graphics or assume stock licenses permit every use.

## 8. Clean-code and engineering rules

### C++

Use RAII, value types and unique ownership by default; shared_ptr only for justified shared lifetimes. Prefer const, enum class, explicit constructors, scoped resources and standard containers. Avoid naked owning new/delete, global mutable state and platform macros spread through domain code. Document coordinate systems, thread ownership and invariants. Validate external input at boundaries; keep dependencies behind narrow interfaces when substitution is useful.

Adopt clang-format and clang-tidy; enable useful compiler warnings and treat project warnings as errors in CI. Use AddressSanitizer/UndefinedBehaviorSanitizer on suitable host/native configurations. Pin one NDK/CMake toolchain and C++ runtime strategy; audit every packaged native dependency. Do not apply a generic linker flag as a substitute for verifying 16 KB compatibility.

### Kotlin and repository hygiene

Unidirectional state flow, immutable screen state, ViewModel lifecycle management and structured concurrency. Use Kotlin formatting/lint and Android lint. Inject adapters at the composition root without introducing a large dependency-injection framework prematurely. Keep classes focused; prefer readable domain names over Manager/Helper catch-all classes. Document public boundaries and why unusual decisions exist.

Pin dependencies in the Gradle version catalog, commit the wrapper/checksums and CMake configuration, verify dependency artifacts, audit licenses, and maintain a dependency inventory. Store signing credentials outside version control. Use protected release workflows, small reviewed changes, meaningful commits and short architecture decision records for important choices.

### Validation matrix

| Level | Required evidence |
| --- | --- |
| Native unit tests | Transform/hit testing, command validation, undo/redo, schema round-trip, migrations and malformed inputs |
| File integration | Interrupted write, recovery, asset staging, disk-full, corrupt document and deletion |
| Renderer | Representative golden images for each script/aspect ratio on pinned emulator/fonts; tolerance-based comparisons |
| Android instrumentation | Picker cancel/import, process-death reopen, rotation, gallery save, URI grants and export errors |
| Device QA | API 29 baseline and current target; arm64 physical device; 4 KB and 16 KB environments; low-memory device; tablet/foldable |
| Sharing QA | At least Instagram, Facebook, X and WhatsApp when installed; no-target/cancel/caption limitations documented |
| Security | JSON/image fuzzing where practical, oversized input, invalid handles, traversal and archive abuse if imports exist |
| Release | Signed AAB installs, offline first-run, policy declarations, pre-launch report, accessibility review and asset licenses |

Initial product targets, to validate on an agreed midrange reference phone: first usable home under 2 seconds, responsive gestures within a 16.7 ms frame budget where feasible, and 1080×1920 export under 3 seconds. These are engineering goals, not measured results. Establish memory budgets after a real-device spike.

Definition of done: acceptance criteria pass; no blocking crashes/data loss; privacy and license impact reviewed; localization/accessibility states covered; docs updated; CI green.

## 9. Delivery roadmap

Estimated 8–10 engineering weeks for one experienced developer, plus content production and store review. This is a planning estimate; native/text-rendering findings may change it.

| Stage | Scope | Exit criteria |
| --- | --- | --- |
| Week 1: feasibility | Toolchain, C++ engine/JNI, one editable card, Bengali/Arabic text, bitmap export, 16 KB smoke test | One end-to-end offline card works on an arm64 device |
| Week 2: foundation | Schema, domain commands, Android adapters, atomic persistence and project library | Draft survives process death; old/corrupt schema behavior verified |
| Weeks 3–4: editor | Text/image/layer tools, gestures, undo/redo, crop and consistent scene rendering | Representative templates editable without preview/export mismatch |
| Week 5: content | Four festival categories, 20 original templates minimum, licensed fonts and previews | Cultural/editorial and license review complete |
| Week 6: export/share | Presets, gallery, Sharesheet, recovery/error UX | Actual receiver-app and offline flows pass |
| Week 7: hardening | Accessibility, localization, memory/performance, sanitizer and 16 KB tests | Release candidate meets agreed budgets and quality gates |
| Weeks 8–10: release | Required closed testing if applicable, feedback fixes, listing, declarations and review | Production access granted where required and signed release approved |

Closed testing can overlap late hardening only when the candidate is genuinely usable. A required 14-day test period and production-access review must be scheduled explicitly; store approval dates cannot be guaranteed. Ship optional online backgrounds after the offline release, not on its critical path.

## 10. Google Play publishing plan

### Policy snapshot: 24 October 2026

This checklist focuses on policies relevant to this app; it is not a substitute for the full Developer Program Policies. Read the live pages and account-specific Console tasks again before every release.

| Requirement | Application action | Official reference |
| --- | --- | --- |
| Target API | New phone apps and updates must target Android 16 / API 36 or higher from 31 August 2026. Set targetSdk at least 36; do not rely on a temporary extension. | [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878) |
| Native page size | New apps/updates targeting Android 15+ must support 16 KB page sizes from 1 November 2025. Check every shipped .so, ELF/package alignment and actual execution. | [16 KB guide](https://developer.android.com/guide/practices/page-sizes) |
| 64-bit | Include arm64-v8a for native delivery; if adding 32-bit ABIs, satisfy corresponding 64-bit requirements. | [64-bit guide](https://developer.android.com/google/play/requirements/64-bit) |
| App packaging | Publish a signed AAB, enroll/use Play App Signing, protect upload key and increment versionCode. | [App bundles](https://developer.android.com/guide/app-bundle) |
| Personal-account testing | For affected personal accounts created after 13 November 2023, run a closed test with at least 12 opted-in testers continuously for 14 days before applying for production access. Submit truthful engagement/testing answers; approval is not automatic. | [Testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465) |
| Identity and registration | Complete account identity/contact verification and any applicable organization/device checks. Verify app registration and package ownership tasks shown in Console, including the 2026 verification changes. | [Verification guidance](https://developer.android.com/developer-verification), [July 2026 announcement](https://support.google.com/googleplay/android-developer/answer/17134731) |
| Privacy policy | Provide an active public policy URL in Console and an accessible in-app link even if the release collects no personal data. Describe local handling, sharing, retention/deletion, third parties and support contact accurately. | [User Data](https://support.google.com/googleplay/android-developer/answer/10144311) |
| Data safety | Complete the form based on actual app and SDK/network behavior. Local-only handling is distinct from transmission; assess user-directed sharing and provider requests under Google's definitions. | [Data safety form](https://support.google.com/googleplay/android-developer/answer/10787469) |
| Photo access | Use system picker; omit broad READ_MEDIA_IMAGES/VIDEO and all-files access. Reassess declarations if the permission model changes. | [Photo/video permissions](https://support.google.com/googleplay/android-developer/answer/14115180) |
| App content | Complete content rating, target audience, ads declaration and app-access information. No login restriction for reviewers; screenshots must show real functionality. | [Prepare for review](https://support.google.com/googleplay/android-developer/answer/9859455) |
| Intellectual property | Own or license every template, font and image; retain proof and provide notices. Use social platform names/brands accurately without implying endorsement. | [Developer policies](https://play.google.com/about/developer-content-policy/) |
| Functional quality | Supply meaningful editing capability and useful original content. Avoid repetitive reskinned apps, misleading claims, broken navigation or a low-value image-only wrapper. | [Developer policies](https://play.google.com/about/developer-content-policy/) |
| Downloaded content | Permit validated data/assets only; do not implement out-of-Play executable self-updates. | [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/16559646) |

Use a current pinned NDK; NDK r28+ provides 16 KB alignment by default, but dependencies and packaging still require inspection. Use an Android Gradle Plugin version compatible with Google's current packaging guidance; verify the generated bundle and delivered APKs. Never hard-code 4096-byte page assumptions. Test on a 16 KB emulator/device, not merely a successful build.

The July 2026 policy announcement also includes chat/child-safety and call-log changes. Those features are outside release 1; the directly relevant actions are app registration, content rating and truthful data handling. Check the announcements and deadlines pages for later changes rather than treating one announcement as the entire current policy set.

### Conditional obligations

- Accounts: no accounts in release 1. If account creation is added, implement the applicable in-app and external account-deletion paths and retention disclosures.
- Monetization: no ads or paid packs initially. If selling digital content later, assess Google Play Billing and applicable program/regional exceptions; update declarations and refund/support flows.
- Children: declare the real audience honestly. If targeting children or mixed audiences, assess Families requirements and approved SDK/ads rules; an age label is not a workaround.
- AI: absent initially. Before adding generative images/text, review AI-generated content requirements, reporting mechanisms and third-party data disclosures/consent.
- Public uploads/community: absent initially. Adding hosted user content requires a separate moderation, reporting/blocking and abuse-response plan.
- Reminders: request notification permission only when needed; use ordinary scheduling where suitable and do not request exact alarms without a qualifying use case.

### Submission checklist

- [ ] Finalize unique applicationId, app name, support email and verified developer information.
- [ ] Confirm current target API, developer verification, registration and account-specific tasks.
- [ ] Audit merged manifest and every SDK; remove unnecessary permissions.
- [ ] Build signed release AAB; verify ABI coverage, all native libraries and 16 KB operation.
- [ ] Pass offline, recovery, export, actual sharing, accessibility and low-memory tests.
- [ ] Complete any required closed test and production-access application.
- [ ] Publish accurate privacy policy and complete Data safety, content rating, target-audience, ads and app-access declarations.
- [ ] Prepare icon, feature graphic, real phone screenshots, localized descriptions and release notes using current Console specifications.
- [ ] Review licenses and culturally sensitive content; provide license notices in app.
- [ ] Inspect pre-launch report; fix crashes, ANRs and blockers before submission.
- [ ] Submit, address review feedback, and use staged rollout where available for subsequent releases.
- [ ] Monitor Android vitals/Console feedback and maintain an incident/rollback plan; understand user-data implications before rollback.

## 11. Build preparation, costs and decisions

### Prerequisites

Android Studio stable, JDK required by the chosen AGP, Android SDK 36 or newer, a pinned compatible NDK and CMake, an arm64 Android device, baseline/current emulators and a 16 KB test environment. Record exact tested versions during the week-one spike rather than labeling unverified package versions as latest.

Planned commands after the repository is implemented:

```bash
./gradlew :app:assembleDebug
./gradlew lint testDebugUnitTest
./gradlew connectedDebugAndroidTest
cmake --preset host-tests
cmake --build --preset host-tests
ctest --preset host-tests
./gradlew :app:bundleRelease
```

The CMake presets and Gradle tasks must be wired up by implementation; these commands are not executable deliverables in this planning document. Keep signing secrets in local/CI secret storage and never commit them. CI should run host core tests, formatting/static analysis, Android build/lint and representative emulator tests; reserve full device/release checks for the appropriate pipeline.

### Cost model

Baseline runtime backend/database cost: none because neither is operated. Other possible costs remain: Play developer registration, artwork/font licenses if not free, developer time, devices, policy/support hosting, and optional network hosting. Check actual registration charges during enrollment; do not assume every service remains free or unlimited. Keep the app ad-free until demand and operating costs justify a deliberate monetization plan.

### Decisions before implementation

Defaults already proposed so work can begin: C++ core plus Kotlin shell, API 29 minimum, offline-only launch, four festival categories, English/Bengali UI, no accounts/ads, local original templates. Confirm public app name/package namespace, developer account type, target countries/audience and licensed content ownership before store setup. If pure C++ is non-negotiable, revisit the UI technology at the feasibility stage before building screens.

## 12. Official technical and policy references

- [Android NDK](https://developer.android.com/ndk)
- [JNI tips](https://developer.android.com/training/articles/perf-jni)
- [Android app architecture](https://developer.android.com/topic/architecture)
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker)
- [Share content](https://developer.android.com/training/sharing/send)
- [Secure file sharing](https://developer.android.com/training/secure-file-sharing)
- [Shared media storage](https://developer.android.com/training/data-storage/shared/media)
- [StaticLayout](https://developer.android.com/reference/android/text/StaticLayout)
- [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile)
- [Backup rules](https://developer.android.com/identity/data/autobackup)
- [Pexels API documentation](https://www.pexels.com/api/documentation/)
- [Pexels free API limits](https://help.pexels.com/hc/en-us/articles/47677890260761-Is-the-Pexels-API-free-to-use)
- [Pexels quota-increase conditions](https://help.pexels.com/hc/en-us/articles/900005852323-How-do-I-get-unlimited-requests)
- [Pixabay API documentation](https://pixabay.com/api/docs/)
- [Google Play Developer Program Policies](https://play.google.com/about/developer-content-policy/)
- [Policy announcements](https://support.google.com/googleplay/android-developer/announcements/13412212)

License for the future application repository remains to be chosen. Third-party asset and dependency licenses remain independently applicable.
