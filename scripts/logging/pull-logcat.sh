#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# pull-logcat.sh — capture logcat from the connected device with ARIA filters.
#
# Usage:
#   pull-logcat.sh                      # tail until Ctrl-C, write to logcat-<ts>.log
#   pull-logcat.sh --dump               # snapshot current ring buffer and exit
#   pull-logcat.sh --since "10:30:00"   # capture from a time of day
#   pull-logcat.sh --pid                # auto-resolve ARIA's pid and filter to it
#   pull-logcat.sh --crash              # only crash buffer (Java + native tombstones)
#
# Output:
#   build-logs/runtime/logcat-YYYYMMDD-HHMMSS.log
#
# Notes:
#   - Requires `adb` on PATH and one device connected (USB or `adb connect`).
#   - The on-device app log file (app.log) is captured by pull-app-logs.sh —
#     this script is for the OS-level logcat only.
#   - The ARIA tag prefix is "ARIA/" so `--filter` defaults to ARIA:V *:W
#     (verbose for our app, warnings+ for everything else).
# ─────────────────────────────────────────────────────────────────────────────

set -u
set -o pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PKG="com.ariaagent.mobile"
OUT_DIR="${REPO_ROOT}/build-logs/runtime"
mkdir -p "${OUT_DIR}"
TS="$(date +%Y%m%d-%H%M%S)"
OUT="${OUT_DIR}/logcat-${TS}.log"

MODE="tail"
FILTER='ARIA:V AndroidRuntime:E DEBUG:E ActivityManager:W libllama-jni:V *:W'
PID_FILTER=""
SINCE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --dump)   MODE="dump"; shift ;;
        --since)  SINCE="$2"; shift 2 ;;
        --pid)    PID_FILTER="yes"; shift ;;
        --crash)  FILTER="-b crash"; shift ;;
        --filter) FILTER="$2"; shift 2 ;;
        -h|--help) sed -n '1,30p' "$0"; exit 0 ;;
        *) echo "unknown arg: $1" >&2; exit 2 ;;
    esac
done

if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: adb not on PATH. Install platform-tools and retry." >&2
    exit 2
fi

DEVS=$(adb devices | awk 'NR>1 && $2=="device"{print $1}')
if [ -z "${DEVS}" ]; then
    echo "ERROR: no adb device connected (try: adb devices)" >&2
    exit 2
fi
DEV_COUNT=$(echo "${DEVS}" | wc -l)
if [ "${DEV_COUNT}" -gt 1 ]; then
    echo "WARN: multiple devices, using the first. Use ANDROID_SERIAL to pin." >&2
fi

# Clear logcat first so the captured window starts clean (only when --dump is
# NOT set — for snapshots we want what's already there).
if [ "${MODE}" = "tail" ] && [ -z "${SINCE}" ]; then
    adb logcat -c >/dev/null 2>&1 || true
fi

PID_ARG=""
if [ "${PID_FILTER}" = "yes" ]; then
    PID=$(adb shell pidof "${PKG}" 2>/dev/null | tr -d '\r' | awk '{print $1}')
    if [ -n "${PID}" ]; then
        echo "[pull-logcat] filtering to pid ${PID} (${PKG})" >&2
        PID_ARG="--pid=${PID}"
    else
        echo "[pull-logcat] WARN: ${PKG} not running — no pid filter applied" >&2
    fi
fi

SINCE_ARG=""
if [ -n "${SINCE}" ]; then
    SINCE_ARG="-T ${SINCE}"
fi

ARGS="-v threadtime ${SINCE_ARG} ${PID_ARG}"

echo "[pull-logcat] mode      : ${MODE}"
echo "[pull-logcat] filter    : ${FILTER}"
echo "[pull-logcat] output    : ${OUT}"
echo "[pull-logcat] (Ctrl-C to stop in tail mode)"
echo

if [ "${MODE}" = "dump" ]; then
    # shellcheck disable=SC2086
    adb logcat -d ${ARGS} ${FILTER} > "${OUT}"
    LINES=$(wc -l < "${OUT}")
    echo "[pull-logcat] captured ${LINES} lines"
else
    # shellcheck disable=SC2086
    adb logcat ${ARGS} ${FILTER} | tee "${OUT}"
fi
