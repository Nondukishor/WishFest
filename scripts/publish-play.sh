#!/usr/bin/env bash
# Build a signed release AAB and upload it to Google Play.
#
# Prerequisites:
#   1. First app version already uploaded manually in Play Console
#   2. play-service-account.json at repo root OR ANDROID_PUBLISHER_CREDENTIALS set
#   3. keystore.properties (or WISHFEST_* env vars) for release signing
#
# Usage:
#   ./scripts/publish-play.sh                  # internal + DRAFT
#   ./scripts/publish-play.sh beta             # beta + DRAFT
#   ./scripts/publish-play.sh production COMPLETED

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

TRACK="${1:-internal}"
STATUS="${2:-DRAFT}"

case "$TRACK" in
  internal|alpha|beta|production) ;;
  *)
    echo "Unknown track: $TRACK (use internal|alpha|beta|production)"
    exit 1
    ;;
esac

if [[ ! -f "play-service-account.json" && -z "${ANDROID_PUBLISHER_CREDENTIALS:-}" ]]; then
  echo "Missing Play credentials."
  echo "Place play-service-account.json in the repo root, or export ANDROID_PUBLISHER_CREDENTIALS."
  echo "See docs/PLAY_STORE_PUBLISH.md"
  exit 1
fi

chmod +x ./gradlew
./gradlew :app:publishReleaseBundle \
  --no-configuration-cache \
  --stacktrace \
  -Pplay.track="$TRACK" \
  -Pplay.releaseStatus="$STATUS"

echo ""
echo "Uploaded to Play track='$TRACK' status='$STATUS'."
echo "Open Play Console to review and roll out if status was DRAFT."
