#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# parse-build-errors.sh — extract every error/failure line from a gradle log.
#
# Usage:
#   parse-build-errors.sh path/to/gradle.log [> errors.txt]
#
# What it picks out:
#   - Kotlin compiler errors    (e: file://...)
#   - Java compiler errors      (... error: ...)
#   - CMake errors              (CMake Error: ..., CMake Warning: ...)
#   - NDK / clang errors        (... error: ..., ... fatal error: ...)
#   - linker errors             (ld: error:, undefined symbol)
#   - Gradle task failures      (FAILURE:, * What went wrong:, * Where:)
#   - General "BUILD FAILED"    block (the gradle task summary at the bottom)
#
# Output format:
#   Each match is printed with the source file:line preserved so editors can
#   jump to it. A `--- gradle FAILURE block ---` header introduces the gradle
#   task summary, which is usually the most actionable.
# ─────────────────────────────────────────────────────────────────────────────

set -u

if [ "$#" -ne 1 ]; then
    echo "usage: $0 <gradle.log>" >&2
    exit 2
fi

LOG="$1"
if [ ! -f "${LOG}" ]; then
    echo "ERROR: log not found: ${LOG}" >&2
    exit 2
fi

# 1. Kotlin / Java / clang style file:line errors.
#    Patterns:
#      e: file:///path/foo.kt:42:5 Some message
#      /abs/path/foo.cpp:42:5: error: bad thing
#      /abs/path/foo.cpp:42:5: fatal error: bad thing
#      /abs/path/Foo.java:42: error: bad thing
grep -nE '^(e: |.*\.(kt|java|cpp|c|h|hpp|cc):[0-9]+(:[0-9]+)?: (fatal )?(error|warning):)' "${LOG}" \
    || true

# 2. CMake errors / warnings (multi-line — print 5 lines of context).
grep -nE 'CMake (Error|Warning)' "${LOG}" \
    | sed 's/^/[cmake] /' \
    || true

# 3. Linker problems.
grep -nE '(undefined (reference|symbol)|ld: error|ld\.lld:)' "${LOG}" \
    | sed 's/^/[link] /' \
    || true

# 4. Gradle "BUILD FAILED" tail block — usually the actionable summary.
#    Print everything from the last "FAILURE:" to end-of-file.
LAST_FAILURE_LINE=$(grep -n '^FAILURE:' "${LOG}" | tail -1 | cut -d: -f1)
if [ -n "${LAST_FAILURE_LINE}" ]; then
    echo "--- gradle FAILURE block ---"
    sed -n "${LAST_FAILURE_LINE},\$p" "${LOG}"
fi

# 5. ABI / NDK setup failures (these come from gradle, not the compiler).
grep -nE '(NDK at .* did not have a source.properties|Could not determine NDK|Failed to install)' "${LOG}" \
    | sed 's/^/[ndk]  /' \
    || true

exit 0
