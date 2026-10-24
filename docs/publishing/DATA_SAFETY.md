# Data safety review for the release owner

This is a source-based draft, not a submitted Play Console declaration. Recheck the exact bundled SDK version before answering the form.

## App behavior

- No login, developer-run card server, advertising SDK, in-app purchase flow or developer-run analytics service was found in the source reviewed.
- Draft text, imported photos and sticker choices are stored locally. Photo access uses Android GetContent; there is no broad media-library permission.
- Saving writes to Pictures/WishFest. Sharing explicitly passes an image to an app chosen by the user.
- Private drafts are excluded from Android backup and device transfer by the release configuration.
- Support email is initiated by the user outside the app. Handle the personal information users include in those requests appropriately.

## Google SDKs: do not declare “no data collected” without review

The build includes Google ML Kit genai-prompt 1.0.0-beta4. Google's disclosure covers identifiers, app/device information, diagnostics, performance, feature-event information, language configuration and input/output sizes. Review the corresponding Play form categories (including device/other IDs, app interactions and diagnostics) and purposes such as analytics. Google describes encrypted transport and no third-party transfers for this SDK data; determine collection, sharing and service-provider exemptions using the current form definitions. Do not assume Google-managed data is deletable through local draft deletion.

Greeting inputs and outputs are processed on-device; that does not eliminate SDK diagnostic disclosures.

Sources:
- https://developers.google.com/ml-kit/android-data-disclosure
- https://developers.google.com/ml-kit/terms
- https://support.google.com/googleplay/android-developer/answer/10144311

## Remaining owner decisions

Confirm SDK disclosure answers, retention/deletion responses, target audience and content rating. This app has no account-creation feature, so account-deletion answers should reflect that. If account creation, ads, cloud AI, analytics or crash reporting are added, update both the policy and the Play declaration.

Review whether optional greeting drafting falls within Google's limited-scope productivity-assistance exception to the AI-generated content policy. Do not claim a working in-app moderation/reporting backend: none is configured. If Play requires an in-app reporting mechanism for this app's scope, implement it before production review.

Policy scope: https://support.google.com/googleplay/android-developer/answer/14094294
