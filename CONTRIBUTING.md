# Contributing to Lilt

Thank you for your interest in contributing to Lilt! We welcome code contributions, documentation improvements, issue reports, and design suggestions.

---

## Code of Conduct

We are committed to providing a welcoming, inclusive, and harassment-free environment for everyone. Please be respectful and considerate in all communications.

---

## Getting Started

1. **Fork the Repository:** Create a fork of `https://github.com/shashankatthaluri/Lilt`.
2. **Clone your Fork:**
   ```bash
   git clone https://github.com/<your-username>/Lilt.git
   cd Lilt
   ```
3. **Branching Convention:**
   * `feature/<name>` for new features
   * `fix/<name>` for bug fixes
   * `docs/<name>` for documentation updates

---

## Monorepo Structure

* **`core/`**: High-performance Rust crate providing text normalization, disfluency backtracking, and temporal parsing.
* **`desktop/`**: Tauri v2 desktop application written in Rust and TypeScript/Vite.
* **`mobile/`**: Android application written in Kotlin using Jetpack Compose following Nothing OS monochrome AMOLED design guidelines.
* **`web/`**: Landing page with device auto-detection and live simulations.

---

## Submitting Pull Requests

1. Keep PRs focused on a single change or fix.
2. Ensure builds pass:
   * Rust: `cargo check` & `cargo test` in `core/`
   * Android: `./gradlew test` in `mobile/`
3. Provide a clear PR description explaining the motivation and changes made.
4. Reference any open issues resolved by the PR.
