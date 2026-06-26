#!/bin/bash
# APK build করে deploy করার script
# ব্যবহার: bash deploy-apk.sh

set -e

ANDROID_HOME=/opt/android-sdk
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_DIR="/var/www/car-apk"
APK_SRC="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

echo "🔨 Building APK..."
cd "$PROJECT_DIR"
./gradlew assembleDebug

if [ ! -f "$APK_SRC" ]; then
    echo "❌ Build ব্যর্থ — APK পাওয়া যায়নি: $APK_SRC"
    exit 1
fi

TS=$(date +%Y%m%d-%H%M%S)
DEST="$APK_DIR/carrental-${TS}.apk"
cp "$APK_SRC" "$DEST"
echo "✅ APK কপি হয়েছে: $DEST"

bash "$APK_DIR/gen-index.sh"
echo "🌐 Download page: https://car.zisan.me/apk/"
