#!/bin/bash
# Archive + upload to TestFlight. Bumps CURRENT_PROJECT_VERSION on every run.
# Needs: app/secrets/AuthKey_<KEY_ID>.p8 (App Store Connect API key, App Manager
# role). Builds land in TestFlight ~10 min after upload and expire in 90 days.
set -euo pipefail

# Credentials come from the environment so nothing identifying lives in the repo.
# Set these once in a gitignored file and source it before running:
#
#   export WIFESIGNAL_KEY_ID=XXXXXXXXXX          # App Store Connect API key ID
#   export WIFESIGNAL_ISSUER_ID=<issuer-uuid>    # stable per team
#
PROJECT_DIR="${WIFESIGNAL_DIR:-$(cd "$(dirname "$0")" && pwd)}"
KEY_ID="${WIFESIGNAL_KEY_ID:?set WIFESIGNAL_KEY_ID (App Store Connect API key ID)}"
ISSUER_ID="${WIFESIGNAL_ISSUER_ID:?set WIFESIGNAL_ISSUER_ID (App Store Connect issuer ID)}"
API_KEY="$PROJECT_DIR/app/secrets/AuthKey_${KEY_ID}.p8"
ARCHIVE=/tmp/WifeSignal.xcarchive
EXPORT_DIR=/tmp/WifeSignal-export

cd "$PROJECT_DIR"

CURRENT=$(grep 'CURRENT_PROJECT_VERSION:' app/project.yml | head -1 | grep -oE '[0-9]+')
NEXT=$((CURRENT + 1))
sed -i '' "s/CURRENT_PROJECT_VERSION: \"$CURRENT\"/CURRENT_PROJECT_VERSION: \"$NEXT\"/" app/project.yml

xcodegen generate --spec app/project.yml --project app

rm -rf "$ARCHIVE" "$EXPORT_DIR"

xcodebuild -project app/WifeSignal.xcodeproj -scheme WifeSignal \
  -destination 'generic/platform=iOS' \
  archive -archivePath "$ARCHIVE" \
  -allowProvisioningUpdates \
  -authenticationKeyPath "$API_KEY" \
  -authenticationKeyID "$KEY_ID" \
  -authenticationKeyIssuerID "$ISSUER_ID"

xcodebuild -exportArchive \
  -archivePath "$ARCHIVE" \
  -exportOptionsPlist "$PROJECT_DIR/ExportOptions.plist" \
  -exportPath "$EXPORT_DIR" \
  -allowProvisioningUpdates \
  -authenticationKeyPath "$API_KEY" \
  -authenticationKeyID "$KEY_ID" \
  -authenticationKeyIssuerID "$ISSUER_ID"

echo "✅ Build $NEXT uploaded. Check TestFlight in ~10 min."
