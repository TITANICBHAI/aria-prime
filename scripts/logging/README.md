# ARIA logging — host-side scripts

These scripts wrap `gradlew` and `adb` so every error from the build system
and from the running device ends up in one predictable place under
`build-logs/`.

| Script | What it does | When to run |
|---|---|---|
| `build-with-logs.sh` | Wraps `gradlew` with `--info --stacktrace`, tees output, snapshots `.cxx` and CMake intermediates, extracts errors. | Every build during dev. Use instead of `./gradlew assembleDebug`. |
| `parse-build-errors.sh` | Reads a gradle log and prints only the actionable error / warning lines (Kotlin, Java, CMake, NDK, linker, gradle FAILURE block). | Called by `build-with-logs.sh`. Also useful manually on CI logs. |
| `pull-logcat.sh` | Captures the device's logcat ring buffer with ARIA filters. Tail mode for live debugging, dump mode for post-mortem. | While the app is running, or right after a crash. |
| `pull-app-logs.sh` | Pulls `app.log`, `crashes/`, `anr/`, `native_crashes/`, `logcat/` from the device — the persistent log tree written by `LogManager`. | After any crash, especially on devices that aren't connected to Android Studio at the time of failure. |
| `symbolicate-native.sh` | Resolves the raw `pc` addresses in a `native_crashes/*.txt` to `function+offset` and `file:line` via `ndk-stack`. | Whenever you have a native crash report from the device. |

## Output layout

All scripts write under `build-logs/` at the repo root (gitignored):

```
build-logs/
├─ <build-timestamp>/
│  ├─ gradle.log              ← every line gradle printed
│  ├─ summary.txt             ← one-screen overview
│  ├─ errors.txt              ← extracted errors
│  ├─ cxx/                    ← snapshot of android/app/.cxx
│  ├─ cmake/                  ← snapshot of CMake intermediates
│  └─ hs_err/                 ← JVM crashes (rare)
└─ runtime/
   ├─ logcat-<ts>.log         ← from pull-logcat.sh
   └─ app-logs-<ts>/          ← from pull-app-logs.sh
      ├─ app.log
      ├─ crashes/
      ├─ anr/
      ├─ native_crashes/
      └─ logcat/
```

## Typical workflows

**Local dev — build broke:**
```
scripts/logging/build-with-logs.sh
# read summary.txt, then errors.txt
```

**App crashed on a real device:**
```
scripts/logging/pull-app-logs.sh --tar
# inspect app-logs-*/crashes/ or native_crashes/
scripts/logging/symbolicate-native.sh app-logs-*/native_crashes/native-crash-*.txt
```

**Live debugging while app runs:**
```
scripts/logging/pull-logcat.sh --pid
```

## Make the scripts executable

After a fresh clone:

```
chmod +x scripts/logging/*.sh
```
