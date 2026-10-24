#!/usr/bin/env bash
# Writes signing secrets into GitHub Environment "dev".
# Usage: ./scripts/set-dev-signing-secrets.sh

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ ! -f app/wishfest-release-key.jks ]]; then
  echo "Missing app/wishfest-release-key.jks"
  exit 1
fi

if [[ ! -f keystore.properties ]]; then
  echo "Missing keystore.properties"
  exit 1
fi

store_password="$(sed -n 's/^storePassword=//p' keystore.properties | tr -d '\r')"
key_alias="$(sed -n 's/^keyAlias=//p' keystore.properties | tr -d '\r')"
key_password="$(sed -n 's/^keyPassword=//p' keystore.properties | tr -d '\r')"

echo "Setting KEYSTORE_BASE64 / WISHFEST_* secrets on environment 'dev'..."
gh secret set KEYSTORE_BASE64 --env dev < <(base64 -i app/wishfest-release-key.jks)
gh secret set WISHFEST_STORE_PASSWORD --env dev -b "$store_password"
gh secret set WISHFEST_KEY_ALIAS --env dev -b "$key_alias"
gh secret set WISHFEST_KEY_PASSWORD --env dev -b "$key_password"

echo "Done. If ANDROID_PUBLISHER_CREDENTIALS is not set yet, add the Play JSON:"
echo "  gh secret set ANDROID_PUBLISHER_CREDENTIALS --env dev < play-service-account.json"
echo "Then re-run the workflow."
