#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# symbolicate-native.sh — turn raw native_crashes/*.txt frames into source:line.
#
# Usage:
#   symbolicate-native.sh native-crash-1714471234-12345.txt
#
# How it works:
#   The crash report contains lines like:
#     #03 pc 0000000000abcdef  /data/app/.../lib/arm64/libllama-jni.so (foo+12)
#   We pipe each one through ndk-stack with the unstripped libs in
#   android/app/build/intermediates/cmake/debug/obj/arm64-v8a, which is where
#   gradle puts the symbol-rich .so before stripping it for the APK.
#
# Requirements:
#   - $ANDROID_NDK_HOME or $ANDROID_NDK on PATH (provides ndk-stack).
#   - The crash file must come from the SAME APK build as the .so on disk —
#     symbol addresses do not survive recompilation.
# ─────────────────────────────────────────────────────────────────────────────

set -u
set -o pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SYMS="${REPO_ROOT}/android/app/build/intermediates/cmake/debug/obj/arm64-v8a"

if [ "$#" -ne 1 ]; then
    echo "usage: $0 <native-crash-file.txt>" >&2
    exit 2
fi

CRASH="$1"
[ -f "${CRASH}" ] || { echo "ERROR: ${CRASH} not found" >&2; exit 2; }

NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK:-}}"
if [ -z "${NDK}" ] || [ ! -x "${NDK}/ndk-stack" ]; then
    # Try sdkmanager-installed default location.
    if [ -x "${ANDROID_SDK_ROOT:-}/ndk/27.1.12297006/ndk-stack" ]; then
        NDK="${ANDROID_SDK_ROOT}/ndk/27.1.12297006"
    else
        echo "ERROR: ndk-stack not found." >&2
        echo "       Set ANDROID_NDK_HOME or ANDROID_NDK to your NDK install." >&2
        exit 2
    fi
fi

if [ ! -d "${SYMS}" ]; then
    echo "ERROR: symbol dir not found: ${SYMS}" >&2
    echo "       Run a debug build first so unstripped .so are available." >&2
    exit 2
fi

echo "[symbolicate] ndk-stack : ${NDK}/ndk-stack"
echo "[symbolicate] symbols   : ${SYMS}"
echo "[symbolicate] crash     : ${CRASH}"
echo

# ndk-stack reads tombstone-style backtraces. Our format is similar enough.
"${NDK}/ndk-stack" -sym "${SYMS}" -dump "${CRASH}"
