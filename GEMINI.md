# Supertonic Android Application

## Project Overview
This directory contains the **Android application** for Supertonic, a high-performance, on-device text-to-speech (TTS) system. It demonstrates how to integrate the Supertonic ONNX models into a mobile app using Kotlin and JNI.

The application serves two main purposes:
1.  **Standalone TTS Player:** A user-friendly interface to type/paste text, select voices, and generate speech.
2.  **System TTS Service:** Implements the Android `TextToSpeechService` API, allowing Supertonic to be used as the system-wide TTS engine.

## Key Features
*   **Offline Inference:** Runs entirely on-device using ONNX Runtime.
*   **Multilingual Support:** Supports multiple languages as defined by the models.
*   **Native Rust Core:** Uses a Rust backend (`supertonic_tts`) accessed via JNI for high-performance inference.
*   **Material Design 3:** Modern UI built with Jetpack Compose.
*   **System Integration:** Works as a standard Android TTS provider.
*   **Audio Export:** Save synthesized speech as WAV files.
*   **Lexicon Management:** User-defined pronunciation rules.
*   **History & Queue:** Track and manage synthesis requests.

## Architecture

### Directory Structure
*   `app/src/main/java/com/brahmadeo/supertonic/tts/`: Kotlin source code.
    *   `MainActivity.kt`, `PlaybackActivity.kt`, `QueueActivity.kt`: Key entry points.
    *   `SupertonicTTS.kt`: JNI wrapper class for the native Rust library.
    *   `ui/`: Jetpack Compose screens (`MainScreen`, `PlaybackScreen`, `HistoryScreen`, `LexiconScreen`, `QueueScreen`, `SavedAudioScreen`).
    *   `viewmodel/`: `MainViewModel.kt` for UI state management.
    *   `service/`:
        *   `PlaybackService.kt`: Foreground service handling audio playback.
        *   `SupertonicTextToSpeechService.kt`: System TTS service implementation.
    *   `utils/`: Helpers for text normalization, language detection, and file management (`TextNormalizer`, `LanguageDetector`, `WavUtils`, `AssetManager`, `QueueManager`).
*   `rust/`: Native Rust/JNI bridge.
    *   `src/lib.rs`: Main JNI interface (init, synthesize, thermal management).
    *   `src/helper.rs`: TTS engine logic.
    *   `src/thermal.rs`: Thermal management.
    *   `vendor/`: Vendored Rust dependencies for offline builds.
    *   `.cargo/config.toml`: Cargo configuration (potentially modified by build scripts).
*   `metadata/`: F-Droid build metadata.
    *   `com.brahmadeo.supertonic.tts.yml`: Build recipe and metadata.

### Native Bridge
The app uses `SupertonicTTS.kt` to communicate with the compiled Rust library (`libsupertonic_tts.so`).
*   **Initialization:** Loads ONNX models and configures the engine.
*   **Synthesis:** Generates PCM audio data from text.
*   **Thermal Management:** Monitors device state to adjust performance.

### Build Configuration
*   **Gradle:** `app/build.gradle` manages the Android build and triggers the Cargo build for the Rust library.
    *   **Plugin:** `org.mozilla.rust-android-gradle.rust-android` handles Rust compilation.
    *   **Tasks:** `extractOnnxLib` (prepares `libonnxruntime.so`), `copyRustLibs` (packages native libs).
*   **Rust:** `rust/Cargo.toml` defines dependencies (ort, ndarray, rayon, etc.).
*   **F-Droid:** The project is configured for F-Droid with offline dependency vendoring (`rust/vendor`).

## Building and Running

### Prerequisites
*   **JDK 17** (required by `app/build.gradle` options).
*   **Android SDK** (API 35).
*   **NDK** (Version 26.1.10909125 or similar).
*   **Rust Toolchain** (via `rustup`, targets: `aarch64-linux-android`, `x86_64-linux-android`, etc.).
*   **Git LFS** (Critical for `assets/` models).

### Build Process
1.  **Initialize Rust Targets:**
    ```bash
    rustup target add aarch64-linux-android
    ```
2.  **Build APK:**
    ```bash
    ./gradlew assembleDebug
    ```
    (Note: Ensure `local.properties` points to your SDK and NDK).

### F-Droid Build Notes
*   **Metadata:** `metadata/com.brahmadeo.supertonic.tts.yml`
*   **Offline Mode:** `rust/.cargo/config.toml` should be configured to use `vendor/` source replacement for reproducible, offline builds.
*   **Linker:** The F-Droid metadata might inject specific linker configurations (e.g., `aarch64-linux-android34-clang`).

## Known Issues
*   **F-Droid Build Failure:** Discrepancies between the local environment and F-Droid's build server (linker versions, NDK paths, offline vendoring) can cause failures.
*   **Metadata Mismatch:** The current code (`v2.0.0-alpha.1`+) might be ahead of the `v1.1` tag defined in `metadata/com.brahmadeo.supertonic.tts.yml`.

## Development
*   **Namespace:** `com.brahmadeo.supertonic.tts`
*   **Min SDK:** 24
*   **Target SDK:** 35
*   **Compile SDK:** 35

<!-- dgc-policy-v11 -->
# Dual-Graph Context Policy

This project uses a local dual-graph MCP server for efficient context retrieval.

## MANDATORY: Always follow this order

1. **Call `graph_continue` first** — before any file exploration, grep, or code reading.

2. **If `graph_continue` returns `needs_project=true`**: call `graph_scan` with the
   current project directory (`pwd`). Do NOT ask the user.

3. **If `graph_continue` returns `skip=true`**: project has fewer than 5 files.
   Do NOT do broad or recursive exploration. Read only specific files if their names
   are mentioned, or ask the user what to work on.

4. **Read `recommended_files`** using `graph_read` — **one call per file**.
   - `graph_read` accepts a single `file` parameter (string). Call it separately for each
     recommended file. Do NOT pass an array or batch multiple files into one call.
   - `recommended_files` may contain `file::symbol` entries (e.g. `src/auth.ts::handleLogin`).
     Pass them verbatim to `graph_read(file: "src/auth.ts::handleLogin")` — it reads only
     that symbol's lines, not the full file.
   - Example: if `recommended_files` is `["src/auth.ts::handleLogin", "src/db.ts"]`,
     call `graph_read(file: "src/auth.ts::handleLogin")` and `graph_read(file: "src/db.ts")`
     as two separate calls (they can be parallel).

5. **Check `confidence` and obey the caps strictly:**
   - `confidence=high` -> Stop. Do NOT grep or explore further.
   - `confidence=medium` -> If recommended files are insufficient, call `fallback_rg`
     at most `max_supplementary_greps` time(s) with specific terms, then `graph_read`
     at most `max_supplementary_files` additional file(s). Then stop.
   - `confidence=low` -> Call `fallback_rg` at most `max_supplementary_greps` time(s),
     then `graph_read` at most `max_supplementary_files` file(s). Then stop.

## Token Usage

A `token-counter` MCP is available for tracking live token usage.

- To check how many tokens a large file or text will cost **before** reading it:
  `count_tokens({text: "<content>"})`
- To log actual usage after a task completes (if the user asks):
  `log_usage({input_tokens: <est>, output_tokens: <est>, description: "<task>"})`
- To show the user their running session cost:
  `get_session_stats()`

Live dashboard URL is printed at startup next to "Token usage".

## Rules

- Do NOT use `rg`, `grep`, or bash file exploration before calling `graph_continue`.
- Do NOT do broad/recursive exploration at any confidence level.
- `max_supplementary_greps` and `max_supplementary_files` are hard caps - never exceed them.
- Do NOT dump full chat history.
- Do NOT call `graph_retrieve` more than once per turn.
- After edits, call `graph_register_edit` with the changed files. Use `file::symbol` notation (e.g. `src/auth.ts::handleLogin`) when the edit targets a specific function, class, or hook.

## Context Store

Whenever you make a decision, identify a task, note a next step, fact, or blocker during a conversation, call `graph_add_memory`.

**To add an entry:**
```
graph_add_memory(type="decision|task|next|fact|blocker", content="one sentence max 15 words", tags=["topic"], files=["relevant/file.ts"])
```

**Do NOT write context-store.json directly** — always use `graph_add_memory`. It applies pruning and keeps the store healthy.

**Rules:**
- Only log things worth remembering across sessions (not every minor detail)
- `content` must be under 15 words
- `files` lists the files this decision/task relates to (can be empty)
- Log immediately when the item arises — not at session end

## Session End

When the user signals they are done (e.g. "bye", "done", "wrap up", "end session"), proactively update `CONTEXT.md` in the project root with:
- **Current Task**: one sentence on what was being worked on
- **Key Decisions**: bullet list, max 3 items
- **Next Steps**: bullet list, max 3 items

Keep `CONTEXT.md` under 20 lines total. Do NOT summarize the full conversation — only what's needed to resume next session.
