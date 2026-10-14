#!/usr/bin/env bash
set -euo pipefail
collect() {
  mkdir -p app/build/reports/screenshots
  adb pull /sdcard/Android/data/org.nexlibre.prismavideo/files/screenshots app/build/reports/screenshots || true
  adb logcat -d > app/build/reports/screenshots/logcat.txt || true
}
trap collect EXIT
./gradlew assembleDebug --no-daemon
gh release download v2.0.1-beta --repo nexlibre/prisma-video --pattern '*.apk' --dir /tmp/prisma-previous
python3 scripts/verify-upgrade.py /tmp/prisma-previous/Prisma-Video-2.0.1-beta.apk app/build/outputs/apk/debug/app-debug.apk
./gradlew connectedDebugAndroidTest --no-daemon
