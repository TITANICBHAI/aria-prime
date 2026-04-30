# Build ARIA in Android Studio — From Zero

A step-by-step guide that takes you from "fresh laptop, no tools" to "ARIA APK installed on a Galaxy M31 with the real LLM running, not stub mode".

> Total time on a fresh machine: **~60–90 minutes** (most of it is downloads).
> Once installed, every subsequent rebuild takes **2–5 minutes**.

---

## 0. What you'll end up with

- Android Studio installed and configured for this repo.
- The full toolchain: JDK 17, Android SDK 34, NDK 27.1.12297006, CMake 3.22.1, glslc.
- A `app-debug.apk` (~180 MB) that contains:
  - `libllama-jni.so` — the JNI bridge into llama.cpp
  - `libllama.so`, `libllama-common.so` — llama.cpp inference
  - `libggml-vulkan.so`, `libggml-opencl.so`, `libggml-cpu.so` — GPU + CPU backends
  - `libmtmd.so` — multimodal vision (CLIP)
  - `libomp.so` — OpenMP runtime
- The APK installed on your phone, running the real on-device LLM (no "stub inference" message).

If any one of those `.so` files is missing from the APK, you'll see `stub inference — llama.cpp not compiled` in chat. The whole point of this guide is to make sure that doesn't happen.

---

## 1. Install Android Studio

1. Download Android Studio from <https://developer.android.com/studio>.
   - Pick **Hedgehog (2023.1.1)** or newer. Anything older may not handle NDK r27.
2. Run the installer. Accept the defaults.
3. On first launch, the **Setup Wizard** opens:
   - Choose **Standard** install.
   - Let it download the Android SDK Platform-Tools and a default emulator. We don't need the emulator, but the wizard installs it anyway.
4. Finish the wizard. You should land on the **Welcome to Android Studio** screen.

> **Disk requirements:** ~15 GB for Android Studio + SDK + NDK + Gradle caches. Have at least 25 GB free.

---

## 2. Install the SDK + NDK + CMake (one-time)

From the Welcome screen → **More Actions** → **SDK Manager**, or from inside a project: **Tools → SDK Manager**.

### SDK Platforms tab
Tick **Android 14 (API 34)** (this matches `compileSdk 34` in `app/build.gradle`).

### SDK Tools tab — tick all of these
| Component | Exact version |
|---|---|
| Android SDK Build-Tools | 34.0.0 |
| Android SDK Platform-Tools | latest |
| **NDK (Side by side)** | **27.1.12297006** |
| **CMake** | **3.22.1** |
| Android SDK Command-line Tools | latest |

> You **must** pick NDK exactly `27.1.12297006`. Older NDKs (r25, r26) miss C++ headers that llama.cpp's training code needs (`flash_attn_type`, `llama_set_adapters_lora`, parts of `ggml-opt`). The Gradle script also pins this version, so a different NDK simply won't be picked up.

Click **Apply**. Total download: ~3–5 GB. Go make coffee.

---

## 3. Install Git and clone the repo

Android Studio does not ship Git on Windows. Install it first:
- **macOS:** `brew install git` (or just run `git` once and accept the Xcode prompt).
- **Windows:** <https://git-scm.com/download/win> — accept the defaults.
- **Linux:** `sudo apt install git` (Debian/Ubuntu) or `sudo dnf install git` (Fedora).

Now clone the repo. Open a terminal:

```bash
git clone https://github.com/TITANICBHAI/aria-prime.git
cd aria-prime
```

Verify you're in the right place — you should see a `README.md`, `AGENT_INSTRUCTIONS.md`, and an `android/` folder.

---

## 4. **Critical step:** add the llama.cpp submodule

The repo deliberately does **not** check in llama.cpp (it's ~150 MB of upstream source). You add it once, manually:

```bash
cd android/app/src/main/cpp
git clone https://github.com/ggerganov/llama.cpp llama.cpp
cd llama.cpp
git checkout b8935        # exact commit the JNI was written against
cd ../../../../../..      # back to repo root
```

> **Why pin `b8935`?** The JNI calls APIs that exist in that commit (`llama_set_adapters_lora`, `flash_attn_type` enum, `ggml-opt` headers, `tools/mtmd`). Newer llama.cpp commits keep renaming things, and older commits are missing them. `b8935` is the version CI builds against and the one verified to compile.

Sanity check:

```bash
ls android/app/src/main/cpp/llama.cpp/CMakeLists.txt   # should exist
ls android/app/src/main/cpp/llama.cpp/tools/mtmd/      # should exist
```

If `tools/mtmd/` is missing, the multimodal build will fail. Re-checkout `b8935`.

---

## 5. Install the SPIR-V / Vulkan shader compiler (`glslc`)

The Vulkan backend pre-compiles GLSL compute shaders into SPIR-V at build time. The compiler is `glslc`, which **ships inside the NDK** but isn't on `PATH` by default.

| OS | Command |
|---|---|
| **macOS / Linux** | Add to your `~/.zshrc` or `~/.bashrc`: `export PATH="$PATH:$HOME/Library/Android/sdk/ndk/27.1.12297006/shader-tools/darwin-x86_64"` (Linux: `linux-x86_64`) |
| **Windows** | `setx PATH "%PATH%;%LOCALAPPDATA%\Android\Sdk\ndk\27.1.12297006\shader-tools\windows-x86_64"` then **restart your shell**. |

Verify:

```bash
glslc --version          # should print "shaderc ... glslang ..."
```

If you get "command not found", the build will fall back to an empty Vulkan backend and you'll lose ~5× GPU speedup but the CPU path still works. To make sure Vulkan compiles:

- macOS: `brew install glslang shaderc`
- Ubuntu/Debian: `sudo apt install glslang-tools spirv-tools libvulkan-dev`
- Windows: install the LunarG **Vulkan SDK** from <https://vulkan.lunarg.com/sdk/home>.

---

## 6. Open the project in Android Studio

1. **File → Open** → pick the `android/` folder (NOT the repo root — the repo root is a pnpm monorepo, you want the inner Android project).
2. Android Studio asks "Trust this project?" → **Trust**.
3. The bottom-right corner shows **Gradle sync running…**. First sync downloads ~1 GB of Gradle dependencies. Wait for it to say **Sync finished**.

If sync fails with "NDK not configured" or "CMake not found":
- **File → Project Structure → SDK Location → Android NDK Location** → pick the NDK 27.1.12297006 folder.
- Re-trigger sync via the elephant icon in the toolbar.

---

## 7. Verify the configuration before building

Open the **Build Variants** panel (View → Tool Windows → Build Variants) and confirm:
- **Active Build Variant: `debug`**
- **Active ABI: `arm64-v8a`**

> **Do not build for `x86_64` or any other ABI.** The Galaxy M31 is arm64 only, and the `app/build.gradle` already restricts to `arm64-v8a`. Building "universal" multiplies your build time by 4 and produces nothing useful.

---

## 8. First build — assemble debug APK

You can use either route. The CLI is more reliable for the very first build.

### Route A — Command line (recommended)

```bash
cd android
./gradlew assembleDebug --info
```

(On Windows: `gradlew.bat assembleDebug --info`.)

What to expect:
- **First build: 15–25 minutes** (CMake configures + compiles ~600 C/C++ files plus all of llama.cpp + ggml backends).
- **Incremental builds: 30 seconds – 2 minutes.**
- Cached `.cxx/` outputs live in `android/app/.cxx/`. Don't delete them between builds.

The output APK lands at:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

### Route B — Inside Android Studio

- Click the green hammer **Make Project** (⌘F9 / Ctrl-F9).
- Or **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
- A toast appears at the bottom: "APK(s) generated successfully — Locate / Analyze".

---

## 9. Verify the APK actually contains the native libraries

This is the single most important check. If `libllama-jni.so` is missing, the app will silently run in stub mode and you'll waste an evening debugging the wrong layer.

In Android Studio:

1. **Build → Analyze APK…** → pick `app-debug.apk`.
2. Expand the `lib/arm64-v8a/` folder.
3. Confirm you see **at minimum**:
   ```
   libllama-jni.so       (~70 KB)
   libllama.so           (~2.4 MB)
   libllama-common.so    (~4.3 MB)
   libggml-base.so       (~1.0 MB)
   libggml-cpu.so        (~950 KB)
   libggml-vulkan.so     (~46 MB)   ← only present if glslc was on PATH
   libggml-opencl.so     (~900 KB)
   libmtmd.so            (~880 KB)
   libomp.so             (~940 KB)
   ```

If `libllama-jni.so` is **not** there:
- Look at the **Build** panel for red CMake errors. Most common: missing `tools/mtmd/`, wrong NDK version, `glslc` not found.
- The detailed CMake log lives at `android/app/.cxx/Debug/<hash>/arm64-v8a/build_command.txt` and `android/app/.cxx/Debug/<hash>/arm64-v8a/cmake_server_log.txt`.

If only the Vulkan `.so` is missing, that's fine — the app will fall back to OpenCL or CPU.

---

## 10. Install on the Galaxy M31

### One-time phone setup
1. **Settings → About phone → Software information** → tap **Build number** 7 times. You're now a developer.
2. **Settings → Developer options** → enable **USB debugging**.
3. Plug the phone into your laptop via USB. The phone shows an "Allow USB debugging?" dialog → tap **Allow** (and tick "Always allow from this computer").

Verify ADB sees the phone:

```bash
adb devices
# List of devices attached
# RZ8N123ABCD     device       ← good. "unauthorized" means re-tap Allow on the phone.
```

### Install
From inside Android Studio:
- Pick your phone in the device dropdown (top toolbar).
- Click the green **Run** triangle (▶︎). It installs and launches in one step.

Or from the command line:
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 11. First-launch checklist on the phone

The app needs several permissions before the LLM is useful. Grant them in order:

| Permission | Where | Why |
|---|---|---|
| Notifications | First-launch popup | Foreground service indicator |
| **Accessibility** | App's Setup screen → **Enable Accessibility** → Settings → Installed services → ARIA Agent → ON | Read screen, execute taps |
| **Display over other apps** | Setup screen → **Allow overlay** | Show ARIA's HUD on top of other apps |
| **All files access** | Setup screen → **Storage** | Save the GGUF model file |
| **MediaProjection** | First time you start a task | Screen capture for OCR + vision |

Then download the model:
1. In ARIA, open the **Models** screen.
2. Tap **Download Llama-3.2-1B-Instruct (Q4_K_M, 870 MB)**.
3. Wait for the download to complete (use Wi-Fi).
4. Go to the **Chat** screen and send "hello".
5. **Expected:** real LLM response, ~3–8 tokens/sec, text streams in token by token.
6. **If you see** `{"tool":"Click","node_id":"#1","reason":"stub inference — llama.cpp not compiled"}` → your APK is missing `libllama-jni.so`. Go back to step 9 and check.

---

## 12. Troubleshooting cheat sheet

| Symptom | Likely cause | Fix |
|---|---|---|
| Gradle sync fails: "NDK not found" | NDK 27.1.12297006 not installed | SDK Manager → SDK Tools → tick exact version |
| CMake error: `Could not find tools/mtmd` | Submodule not added or wrong commit | Step 4: clone llama.cpp at `b8935` |
| CMake error: `glslc: command not found` | Vulkan shader compiler not on PATH | Step 5 — install glslc / Vulkan SDK |
| Build OK but APK is 60 MB and missing libs | CMake target failed silently — check the Build panel for warnings | Re-run `./gradlew assembleDebug --info` and read the `[CXX]` lines |
| App installs but crashes on launch with `UnsatisfiedLinkError: dlopen failed: cannot locate symbol "..."` | NDK version mismatch (built with r25, runtime expects r27) | Step 2 — confirm NDK 27.1.12297006 is the active one |
| Chat shows "stub inference — llama.cpp not compiled" | `libllama-jni.so` is not bundled (or failed to load at runtime) | Step 9 — Analyze APK; check `adb logcat -s LlamaEngine` for `UnsatisfiedLinkError` |
| `Wait` action repeats forever | Model not loaded, or accessibility tree is empty (game/Unity/WebView) | Open the Models screen and load the GGUF; for games enable SmolVLM in Modules |
| 0 tokens/sec on phone | GPU backend not selected | Settings → Engine → switch GPU backend to OpenCL (Mali) or Vulkan |

---

## 13. Reading the logs

Two complementary log surfaces:

### A. Logcat (Android Studio's bottom panel)
- Filter by tag: `LlamaEngine`, `AgentLoop`, `ARIA/*`.
- Look for these lines on launch:
  ```
  I/LlamaEngine: llama-jni loaded — real inference active
  ```
  or the bad case:
  ```
  W/LlamaEngine: llama-jni not found — running in stub mode
  ```

### B. On-device file logs (built into ARIA)
ARIA writes structured logs to its private storage via the `core/logging/FileLogWriter` we just fixed:
```
adb shell run-as com.ariaagent.mobile ls files/logs/
adb shell run-as com.ariaagent.mobile cat files/logs/aria.log
```

Or pull them off the device:
```bash
adb exec-out run-as com.ariaagent.mobile tar c -C files logs | tar xv -C ./aria-logs
```

Native crashes (SIGSEGV in llama.cpp) are also caught by `aria_crash_handler.cpp` and written into `files/logs/native-crash-*.txt` with a backtrace.

---

## 14. The fast loop after first build

Day-to-day rebuild loop:

```bash
cd android
./gradlew installDebug    # builds + installs in one shot, ~1–2 min after first time
adb logcat -s LlamaEngine AgentLoop ARIA -v time
```

If you change only Kotlin files: ~30 seconds.
If you change C/C++ files in `cpp/`: ~1 minute (only the touched files recompile, llama.cpp is cached).
If you change `CMakeLists.txt` or upgrade the submodule: ~15 minutes (full CMake reconfigure).

To force a clean NDK rebuild (rare):

```bash
rm -rf android/app/.cxx android/app/build
./gradlew assembleDebug
```

---

## 15. If you don't want to build it yourself

Every push to `main` triggers GitHub Actions which performs steps 4 → 9 in CI on a beefy Ubuntu runner. The result is downloadable as the `aria-debug-apk` artifact:

1. Go to <https://github.com/TITANICBHAI/aria-prime/actions>.
2. Click the latest green run.
3. Scroll to **Artifacts** → download `aria-debug-apk` → unzip → install via `adb install -r app-debug.apk`.

This is the recommended path if your laptop is underpowered or you just want to test the latest commit.

---

## TL;DR — the 6 things that matter

1. **NDK exactly 27.1.12297006.**
2. **CMake 3.22.1.**
3. **`git clone llama.cpp` into `cpp/` and `git checkout b8935`.**
4. **`glslc` on PATH** (or install Vulkan SDK).
5. **arm64-v8a only.**
6. **After the build, open the APK in Analyze APK and confirm `libllama-jni.so` is in `lib/arm64-v8a/`.**

Get those right and you'll never see "stub inference" again.
