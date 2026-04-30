#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# pull-app-logs.sh — pull the on-device log directory written by LogManager.
#
# What this gives you (vs pull-logcat.sh):
#   - app.log + app.log.1..5    rolling app log mirrored from AriaLog
#   - crashes/                   JVM uncaught exceptions captured by CrashHandler
#   - anr/                       main-thread ANR snapshots from AnrWatchdog
#   - native_crashes/            SIGSEGV / SIGABRT reports from aria_crash_handler.cpp
#   - logcat/                    in-app logcat snapshots
#
# These survive disconnection from Android Studio. Pull them after a crash
# happens in the field, or as part of a bug report.
#
# Usage:
#   pull-app-logs.sh
#   pull-app-logs.sh --tar       # also produce a single .tar.gz for sharing
# ─────────────────────────────────────────────────────────────────────────────

set -u
set -o pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PKG="com.ariaagent.mobile"
DEV_PATH="/sdcard/Android/data/${PKG}/files/logs"
TS="$(date +%Y%m%d-%H%M%S)"
DEST="${REPO_ROOT}/build-logs/runtime/app-logs-${TS}"

MAKE_TAR=0
[ "${1:-}" = "--tar" ] && MAKE_TAR=1

if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: adb not on PATH." >&2
    exit 2
fi

DEV=$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')
if [ -z "${DEV}" ]; then
    echo "ERROR: no adb device connected." >&2
    exit 2
fi

# Confirm the path exists (catches "app never ran" / "package wrong" early).
if ! adb shell "[ -d ${DEV_PATH} ]"; then
    echo "ERROR: ${DEV_PATH} does not exist on the device." >&2
    echo "       Has the app been launched at least once on this device?"  >&2
    echo "       Is the package id ${PKG} correct?"                         >&2
    exit 3
fi

mkdir -p "${DEST}"
echo "[pull-app-logs] device path : ${DEV_PATH}"
echo "[pull-app-logs] dest        : ${DEST}"
echo

# adb pull on a directory copies the whole tree.
adb pull "${DEV_PATH}" "${DEST}/"

# Move the inner "logs" dir up one so the tarball is rooted at "logs/".
if [ -d "${DEST}/logs" ]; then
    mv "${DEST}/logs/"* "${DEST}/" 2>/dev/null || true
    rmdir "${DEST}/logs" 2>/dev/null || true
fi

echo
echo "[pull-app-logs] tree:"
find "${DEST}" -maxdepth 3 -type f -printf '  %P  (%s bytes)\n' 2>/dev/null \
    || find "${DEST}" -type f | sed 's/^/  /'

if [ "${MAKE_TAR}" -eq 1 ]; then
    TAR="${DEST}.tar.gz"
    tar -C "$(dirname "${DEST}")" -czf "${TAR}" "$(basename "${DEST}")"
    echo
    echo "[pull-app-logs] tarball: ${TAR} ($(du -h "${TAR}" | cut -f1))"
fi
