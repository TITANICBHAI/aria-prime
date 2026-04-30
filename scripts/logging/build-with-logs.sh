#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# build-with-logs.sh — wrap `gradlew` with extensive logging.
#
# Why:
#   A bare `./gradlew assembleDebug` shows only the high-level summary on stdout
#   and silently buries CMake / NDK / Kotlin compiler output in
#   android/app/build/intermediates/. When a build fails on a fresh checkout —
#   especially the llama.cpp NDK compile — the actual error is in one of those
#   buried files and the user has no idea where to look.
#
# What this does:
#   1. Runs gradle with --info --stacktrace --warning-mode=all so every warning
#      and the full classpath of every failure are visible on stdout.
#   2. Tees stdout+stderr to a timestamped log file.
#   3. After the build, copies the .cxx native build dir, the CMake intermediates,
#      and the JVM crash logs (hs_err_pid*.log) into one place.
#   4. Prints a one-line summary with the path to a self-contained log bundle
#      that can be attached to a bug report or pulled by CI.
#
# Usage:
#   scripts/logging/build-with-logs.sh                    # debug APK
#   scripts/logging/build-with-logs.sh assembleRelease    # any gradle task
#   scripts/logging/build-with-logs.sh assembleDebug -PsomeFlag=true
#
# Output:
#   build-logs/
#     YYYYMMDD-HHMMSS/
#       gradle.log              # full gradle stdout+stderr
#       summary.txt             # one-screen overview (errors, timing, sizes)
#       cxx/                    # snapshot of android/app/.cxx
#       cmake/                  # snapshot of cmake intermediates
#       hs_err/                 # JVM crashes (only present on JVM crash)
#       configuration-cache/    # gradle configuration cache reports if any
#       errors.txt              # extracted error lines (see parse-build-errors.sh)
#
# Exit code: same as gradle's.
# ─────────────────────────────────────────────────────────────────────────────

set -u
set -o pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ANDROID_DIR="${REPO_ROOT}/android"
GRADLEW="${ANDROID_DIR}/gradlew"

if [ ! -x "${GRADLEW}" ]; then
    chmod +x "${GRADLEW}" 2>/dev/null || true
fi
if [ ! -f "${GRADLEW}" ]; then
    echo "ERROR: gradlew not found at ${GRADLEW}" >&2
    exit 2
fi

TS="$(date +%Y%m%d-%H%M%S)"
LOG_ROOT="${REPO_ROOT}/build-logs/${TS}"
mkdir -p "${LOG_ROOT}"

GRADLE_LOG="${LOG_ROOT}/gradle.log"
SUMMARY="${LOG_ROOT}/summary.txt"
ERRORS="${LOG_ROOT}/errors.txt"

# Default task: assembleDebug. Pass other tasks as arguments.
if [ "$#" -eq 0 ]; then
    set -- assembleDebug
fi

echo "[build-with-logs] task        : $*"            | tee    "${SUMMARY}"
echo "[build-with-logs] log root    : ${LOG_ROOT}"   | tee -a "${SUMMARY}"
echo "[build-with-logs] gradle log  : ${GRADLE_LOG}" | tee -a "${SUMMARY}"
echo "[build-with-logs] start time  : $(date -u +%FT%TZ)" | tee -a "${SUMMARY}"
echo                                                  | tee -a "${SUMMARY}"

START=$(date +%s)

# --info       : verbose enough to see CMake invocations and per-task timing
# --stacktrace : full Java stack trace on any task failure
# --warning-mode=all : every deprecation warning, not just a count
# --no-daemon  : the daemon caches state across runs which can mask
#                first-time-build failures; CI-equivalent behavior.
( cd "${ANDROID_DIR}" && \
    "${GRADLEW}" "$@" \
        --info \
        --stacktrace \
        --warning-mode=all \
        --no-daemon \
        2>&1 \
) | tee "${GRADLE_LOG}"
EXIT=${PIPESTATUS[0]}

END=$(date +%s)
DURATION=$((END - START))

echo                                                  | tee -a "${SUMMARY}"
echo "[build-with-logs] exit code   : ${EXIT}"        | tee -a "${SUMMARY}"
echo "[build-with-logs] duration    : ${DURATION}s"   | tee -a "${SUMMARY}"

# ─── Snapshot native build dirs ─────────────────────────────────────────────
# Copy not move — the user may want to re-run gradle without re-bootstrapping.
# Use rsync if present (faster for large trees); fall back to cp.
copy_tree() {
    local src="$1"; local dst="$2"
    [ -d "${src}" ] || return 0
    mkdir -p "${dst}"
    if command -v rsync >/dev/null 2>&1; then
        rsync -a --exclude '*.o' --exclude '*.obj' "${src}/" "${dst}/" 2>/dev/null || true
    else
        cp -R "${src}/." "${dst}/" 2>/dev/null || true
    fi
}

copy_tree "${ANDROID_DIR}/app/.cxx"                              "${LOG_ROOT}/cxx"
copy_tree "${ANDROID_DIR}/app/build/intermediates/cmake"         "${LOG_ROOT}/cmake"
copy_tree "${ANDROID_DIR}/app/build/reports/configuration-cache" "${LOG_ROOT}/configuration-cache"

# Hotspot crash logs from the gradle JVM (rare, but VERY hard to find).
mkdir -p "${LOG_ROOT}/hs_err"
find "${ANDROID_DIR}" -maxdepth 3 -name 'hs_err_pid*.log' -mtime -1 \
    -exec cp {} "${LOG_ROOT}/hs_err/" \; 2>/dev/null || true
# Empty hs_err dir → remove for cleanliness
[ -z "$(ls -A "${LOG_ROOT}/hs_err" 2>/dev/null)" ] && rmdir "${LOG_ROOT}/hs_err"

# ─── Extract errors ─────────────────────────────────────────────────────────
"${REPO_ROOT}/scripts/logging/parse-build-errors.sh" "${GRADLE_LOG}" > "${ERRORS}" 2>/dev/null || true
ERR_COUNT=$(wc -l < "${ERRORS}" 2>/dev/null || echo 0)
echo "[build-with-logs] error lines : ${ERR_COUNT}"  | tee -a "${SUMMARY}"

# ─── Final summary ──────────────────────────────────────────────────────────
echo                                                  | tee -a "${SUMMARY}"
if [ "${EXIT}" -eq 0 ]; then
    echo "[build-with-logs] ✓ build succeeded"        | tee -a "${SUMMARY}"
    APK="${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "${APK}" ]; then
        APK_SIZE=$(du -h "${APK}" | cut -f1)
        echo "[build-with-logs] apk         : ${APK} (${APK_SIZE})" | tee -a "${SUMMARY}"
    fi
else
    echo "[build-with-logs] ✗ build FAILED"           | tee -a "${SUMMARY}"
    echo "[build-with-logs]   first errors:"          | tee -a "${SUMMARY}"
    head -20 "${ERRORS}" | sed 's/^/    /'            | tee -a "${SUMMARY}"
fi

echo
echo "Full bundle: ${LOG_ROOT}"
exit ${EXIT}
