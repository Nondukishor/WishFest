#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./gradlew :app:bundleRelease :app:assembleRelease :app:lintRelease :app:testDebugUnitTest "$@"
python3 scripts/verify_release.py
