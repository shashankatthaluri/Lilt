# L I L T

<p align="center">
  <strong>Talk at the speed of thought.</strong><br>
  Next-generation open-source voice dictation with mid-sentence self-correction, persistent silent alarms, automated checklist structuring, and an unobtrusive transparent floating pill.
</p>

<p align="center">
  <a href="https://github.com/shashankatthaluri/Lilt/releases"><img src="https://img.shields.io/github/v/release/shashankatthaluri/Lilt?color=red&label=release" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <img src="https://img.shields.io/badge/Platform-Windows%20%7C%20Android-black?logo=android&logoColor=white" alt="Platforms">
  <img src="https://img.shields.io/badge/Engine-Rust%20%2B%20Whisper-orange?logo=rust" alt="Rust">
  <img src="https://img.shields.io/badge/Mobile%20UI-Jetpack%20Compose%20%28Nothing%20OS%29-white" alt="Nothing OS">
</p>

<p align="center">
  <a href="https://shashankatthaluri.github.io/Lilt/"><strong>🌐 Visit Official Website & Live Demo</strong></a> •
  <a href="#downloads"><strong>⚡ Downloads</strong></a> •
  <a href="#core-features"><strong>✨ Core Features</strong></a> •
  <a href="#architecture"><strong>📐 Architecture</strong></a> •
  <a href="#building-from-source"><strong>🛠️ Build Guide</strong></a>
</p>

---

## Overview

Traditional voice typing keyboards expect humans to dictate like a prepared speech script. In real life, people hesitate, stutter, change their mind mid-sentence, and mix local dialects with English.

**Lilt** is designed for real human speech:
* **Mid-Sentence Backtracking:** Change your mind mid-phrase (*"Send report Friday... no wait, make it Monday morning"*), and Lilt automatically strips the false start to output clean, ready-to-send text.
* **Transparent Floating Pill:** Floats silently over any application on your desktop or mobile phone. Tap or hold a hotkey to dictate directly into WhatsApp, Slack, Gmail, or your code editor without context switching.
* **Persistent Silent Alarms:** Speak appointments naturally (*"Dentist tomorrow at 11 AM"*). On mobile, Lilt fires a persistent full-screen alarm overlay with **Silent Alarm Mode** (wakes the display and vibrates with zero disruptive audio) and a tactile 5-minute snooze stepper (`[-] / [+]`).
* **Auto Note Taker & Checklists:** Speak notes (*"Take a note: buy almond milk and eggs"*), and Lilt auto-structures interactive markdown checkboxes with Pin to Top (`📌`) and instant keyword search.
* **Zero Phonetic Distortion for Indian Names:** Injects device contacts and regional Indian names (*"Venkatesh"*, *"Gachibowli"*, *"Banjara Hills"*) so they never get distorted into English approximations.

---

## Core Features

### 1. 🪟 Transparent Floating Pill (Always Accessible)
* **Desktop (Windows):** A lightweight Tauri v2 + Rust pill that floats over your screen. Hold hotkey (e.g. `Ctrl+Space`), speak, and text is typed directly at your cursor location.
* **Mobile (Android):** A minimal, edge-docked Nothing OS translucent circle. Tap to dictate, copy, or send from any app.

### 2. 🔕 Persistent Silent Alarms & Smart Scheduling
* Extracts dates, times, and relative references (*"Repu 10 AM ki meeting"* or *"Call doctor tomorrow 3 PM"*).
* **Silent Alarm Mode:** Illuminates the phone screen persistently until acknowledged, demanding visual attention without ringing loud alarms during meetings or quiet hours.
* **Tactile 5-Minute Snooze Stepper:** Tap `[-]` or `[+]` on the full-screen alert to dial your snooze duration in 5-minute increments (range: 5m to 180m), or use instant chips (`5m`, `10m`, `15m`, `30m`, `1h`).

### 3. 📝 Auto Note Taker & Markdown Checklists
* Dictate unstructured brain-dumps; Lilt detects notes and structures them with clean markdown bullets (`•`).
* Tap any bullet in the app to toggle strike-through (`~done~`).
* **Pin to Top (`📌`):** Star/pin important thoughts to anchor them at the very top of your History feed.
* **Full-Text History Search:** Instant search across dictations, notes, and alerts with amber keyword match highlighting.

### 4. ⚡ Disfluency Backtracking ("Scratch That" Engine)
* Handles spoken self-corrections:
  * *"Send an email to Suresh... scratch that, make it Rajesh"* $\rightarrow$ `Send an email to Rajesh`
  * *"Kaadu kaadu / nahi nahi"* Indian multilingual negations are detected and cleaned automatically.

---

## Downloads

| Platform | Package | Architecture | Notes |
|---|---|---|---|
| **Windows 10 / 11** | [**Lilt-Setup-x64.exe**](https://github.com/shashankatthaluri/Lilt/releases) | x86_64 | Tauri v2 + Rust Desktop Client (~24 MB) |
| **Android (10+)** | [**Lilt-v1.1-Release.apk**](https://github.com/shashankatthaluri/Lilt/releases) | ARM64 / Universal | Jetpack Compose + Nothing OS Dark Luxury (~17 MB) |

---

## Architecture

Lilt is organized as a high-performance cross-platform monorepo:

```text
Lilt/
├── core/                      # High-performance Rust audio, VAD, and disfluency engine
│   ├── Cargo.toml
│   └── src/
├── desktop/                   # Windows Desktop Client (Tauri v2 + Rust + Vite/TypeScript)
│   ├── src-tauri/
│   ├── src/
│   └── package.json
├── mobile/                    # Android Mobile Client (Kotlin + Jetpack Compose + JNI)
│   ├── app/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── web/                       # Official Nothing OS Landing Page with Device Auto-Detection
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── .github/workflows/         # Automated GitHub Actions CI/CD
├── LICENSE                    # Apache 2.0 Open Source License
└── README.md
```

### Shared Engine (`core/`)
Written in pure Rust with JNI bindings (`liblilt_core.so`), `core/` provides zero-latency text cleaning, regex-based disfluency removal, and phonetic context injection across both Windows and Android.

---

## Building from Source

### Prerequisites
* **Rust**: `1.75+` (`cargo`, `rustc`)
* **Node.js**: `18+` (for desktop UI and landing page)
* **Android SDK**: API 34+ and JDK 17 (for mobile app)

### 1. Build Shared Core (Rust)
```bash
cd core
cargo build --release
```

### 2. Build Windows Desktop (Tauri)
```bash
cd desktop
npm install
npm run tauri build
```
The installer will be generated in `desktop/src-tauri/target/release/bundle/nsis/`.

### 3. Build Android Mobile (Gradle)
```bash
cd mobile
./gradlew assembleRelease
```
The verified APK will be output in `mobile/app/build/outputs/apk/release/app-release.apk`.

---

## Contributing

Contributions, feature ideas, and translations are welcome!
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/NewCapability`).
3. Commit your Changes (`git commit -m 'Add new capability'`).
4. Push to the Branch (`git push origin feature/NewCapability`).
5. Open a Pull Request.

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines.

---

## License

Distributed under the **Apache 2.0 License**. See [LICENSE](LICENSE) for more information.

Developed with precision by [Shashank Atthaluri](https://github.com/shashankatthaluri).
