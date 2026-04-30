# 08 — Logging & error capture

ARIA's logging stack is built so **no error escapes unnoticed**, whether it
happens in Gradle, in `clang`, in the Kotlin code, on the main thread, or
inside `libllama-jni.so`.

This document is the index. Each layer has its own short reference below.

```
┌─────────────────────────── HOST (your laptop / CI) ──────────────────────────┐
│  scripts/logging/build-with-logs.sh   gradle stdout/stderr → build-logs/     │
│                       │                                                       │
│                       ├─ parse-build-errors.sh  (errors.txt)                  │
│                       ├─ snapshot android/app/.cxx                            │
│                       └─ snapshot cmake intermediates                         │
│                                                                               │
│  scripts/logging/pull-logcat.sh        adb logcat → build-logs/runtime/       │
│  scripts/logging/pull-app-logs.sh      device /sdcard logs → build-logs/      │
│  scripts/logging/symbolicate-native.sh ndk-stack on a native_crashes/*.txt   │
└───────────────────────────────────────────────────────────────────────────────┘
                                       │  adb
                                       ▼
┌──────────────────────────── DEVICE (the running APK) ────────────────────────┐
│  android/app/src/main/kotlin/com/ariaagent/mobile/core/logging/              │
│    AriaLog              every log call goes through here                     │
│      └─ android.util.Log    (kept — Android Studio logcat still works)       │
│      └─ FileLogWriter        rolling app.log on /sdcard                      │
│    CrashHandler         JVM Thread.UncaughtExceptionHandler → crashes/       │
│    AnrWatchdog          main-thread watchdog → anr/                          │
│    LogcatCollector      `logcat -d` snapshot helper → logcat/                │
│    NativeCrashHandler   Kotlin facade for the native signal handler          │
│    StrictModeInstaller  debug-only invariants (disk on UI, leaks)            │
│    LogManager           install/teardown for the whole stack                 │
│                                                                               │
│  android/app/src/main/cpp/aria_crash_handler.cpp                             │
│    sigaction handler for SIGSEGV/SIGBUS/SIGFPE/SIGILL/SIGABRT/SIGPIPE        │
│    backtrace via _Unwind_Backtrace + dladdr + abi::__cxa_demangle            │
│    writes native_crashes/*.txt then re-raises so ART still records death     │
└───────────────────────────────────────────────────────────────────────────────┘
```

## On-device log layout

`LogManager.install()` (called from `MainApplication.onCreate`) creates this
tree at `getExternalFilesDir(null)/logs/`:

| Path | Source | Format |
|---|---|---|
| `app.log`, `app.log.1` … `app.log.5` | every `AriaLog.x` call | one entry per line, `2 MB` rotation, max 5 files (10 MB cap) |
| `crashes/crash-<ts>-<thread>.txt` | `CrashHandler` | JVM uncaught exception with all-thread stack dump |
| `anr/anr-<ts>.txt` | `AnrWatchdog` | main thread blocked > 5 s, all threads captured |
| `native_crashes/native-crash-<epoch>-<tid>.txt` | `aria_crash_handler.cpp` | signal name, registers, backtrace |
| `logcat/logcat-<ts>.txt` | `LogcatCollector.snapshot()` | filtered `logcat -d` snapshot |

**No root and no special permissions are needed.** This directory is the app's
external-files dir, which is writable by the app and readable by ADB and by
the system Files app.

## Pulling everything off the device

```bash
scripts/logging/pull-app-logs.sh --tar
# → build-logs/runtime/app-logs-<ts>.tar.gz
```

Or manually:

```bash
adb pull /sdcard/Android/data/com.ariaagent.mobile/files/logs ./aria-logs
```

## Tag convention

Every line emitted by `AriaLog` is prefixed with `ARIA/` so logcat filtering is
trivial:

```bash
adb logcat | grep ARIA/
adb logcat ARIA:V *:W           # verbose for ARIA, warnings+ for the system
```

Native code from `libllama-jni` continues to use the existing `LlamaJNI` and
`AriaNativeCrash` tags via `__android_log_print`.

## Build-time logs

Use `scripts/logging/build-with-logs.sh` instead of calling `gradlew` directly:

```bash
scripts/logging/build-with-logs.sh                  # debug APK
scripts/logging/build-with-logs.sh assembleRelease  # any task
```

The output bundle (`build-logs/<timestamp>/`) is self-contained — attach it to
a bug report and reviewers can see exactly what happened, including:

- the full Kotlin/Java/CMake/NDK error stream (`gradle.log`)
- a one-screen summary (`summary.txt`)
- only the actionable error lines (`errors.txt`)
- the entire native build cache (`cxx/`) for archaeology on link failures

## Sharing logs from the app (optional UI hook)

`LogManager.shareIntent(context, fileProviderAuthority)` returns a ready
`ACTION_SEND` intent that bundles the whole log tree into a zip and shares it
via the system share sheet. Wire it to a Settings → Diagnostics button.

To enable this, register a `FileProvider` in `AndroidManifest.xml`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.logfileprovider"
    android:grantUriPermissions="true"
    android:exported="false">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

…and add `res/xml/file_paths.xml`:

```xml
<paths>
    <cache-path name="aria_logs" path="." />
</paths>
```

Then call:

```kotlin
startActivity(Intent.createChooser(
    LogManager.shareIntent(context, "$packageName.logfileprovider"),
    "Share ARIA logs"
))
```

This is intentionally **not** wired automatically — it requires UI choices
that belong in the Settings screen, not in the logging subsystem.

## Symbolicating a native crash

A raw `native-crash-*.txt` looks like:

```
#03 pc 00000000004abcdef  /data/app/.../lib/arm64/libllama-jni.so (foo+12)
```

Resolve it with:

```bash
scripts/logging/symbolicate-native.sh \
    build-logs/runtime/app-logs-*/native_crashes/native-crash-*.txt
```

The script wraps `ndk-stack` against the unstripped `.so` files in
`android/app/build/intermediates/cmake/debug/obj/arm64-v8a` (which gradle
keeps after every debug build). The crash file MUST come from the same APK
build as the on-disk `.so` — symbol addresses do not survive a recompile.

## What this stack does NOT do

- **No external upload.** Everything stays on the device or your laptop. If
  you want Crashlytics / Sentry, wire it up alongside this stack — the file
  bundle from `LogManager.bundleAll(...)` is a clean attachment payload.
- **No stack-trace rewriting** for stripped release `.so`. Symbolicate
  release builds with `ndk-stack` against the matching unstripped `.so` you
  archived at release time.
- **No PII redaction.** Treat the bundle as private; don't paste it into a
  public bug report without scrubbing it first.
