#!/bin/bash
# ═══════════════════════════════════════════════════
# 🎬 مصنع الفيديو - سكربت بناء APK
# ═══════════════════════════════════════════════════
# شغّل هذا الملف على جهازك لبناء APK
#
# الاستخدام:
#   chmod +x build-apk.sh
#   ./build-apk.sh
#
# المتطلبات:
#   1. Java JDK 17: https://adoptium.net/
#   2. Android SDK: ثبّت Android Studio
# ═══════════════════════════════════════════════════

set -e

echo "🎬 مصنع الفيديو - بناء APK"
echo "═══════════════════════════════════════"

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java غير مثبت!"
    echo "📥 حمّله من: https://adoptium.net/"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "✅ Java $JAVA_VER"

# Check Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "❌ Android SDK غير موجود!"
    echo "📥 ثبّت Android Studio من: https://developer.android.com/studio"
    exit 1
fi

SDK_PATH="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo "✅ Android SDK: $SDK_PATH"

# Create local.properties if missing
if [ ! -f "local.properties" ]; then
    echo "sdk.dir=$SDK_PATH" > local.properties
    echo "✅ تم إنشاء local.properties"
fi

# Make gradlew executable
chmod +x gradlew 2>/dev/null || true

echo ""
echo "🔨 جاري بناء APK..."
echo "═══════════════════════════════════════"

# Build debug APK
./gradlew assembleDebug --no-daemon

echo ""
echo "═══════════════════════════════════════"
echo "✅ تم بناء APK بنجاح!"
echo ""

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "📦 الملف: $APK_PATH"
    echo "📏 الحجم: $APK_SIZE"
    echo ""
    echo "📱 أنقل الملف لجوالك وثبّته!"
else
    echo "❌ لم يتم العثور على APK"
fi
