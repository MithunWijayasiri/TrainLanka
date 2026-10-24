---
name: gradle-checks
description: Run the Train Lanka app's CI checks and builds — ktlint, Android lint, release APK — or fix ktlint with ktlintFormat. Use when asked to run checks, lint, ktlint, build the APK, or reproduce a CI (`app.yml`) failure. Never run after edits unless asked.
---

# Gradle checks

Same checks as CI (`.github/workflows/app.yml`). Run as 3 separate commands so each reports on its own:

```bash
./gradlew --console=plain ktlintCheck
./gradlew --console=plain :app:lintRelease
./gradlew --console=plain :app:assembleRelease
```

- APK → `app/build/outputs/apk/release/TrainLanka-<version>-release.apk`.
- Release = R8 minify + shrinkResources, signed with debug key (sideload only). Types R8 must not rename need `@Keep` (e.g. `PickerField`: Navigation resolves enum route args by class name).
- No unit tests yet (`junit` declared, no `app/src/test`).
- Needs JDK 17 and `sdk.dir` in `local.properties`.

## ktlint

- No baseline: all code is `ktlintFormat`-clean, so any violation fails `ktlintCheck`.
- Fix → `./gradlew --console=plain ktlintFormat`. Run before committing.
- `.editorconfig`: function naming rule ignores `@Composable`.

## Debug code

- Working tree may hold uncommitted `debugLog` lines (`debug-logs` skill); checks run on them as-is, CI only on committed code.
- Local failures only in uncommitted debug code (e.g. lint `RestrictedApi` on `nav.currentBackStack` in `AppNav.kt` debug block, ktlint in `DebugLog.kt`) → not real. Committed debug code fails CI like any other code.
- CI-equivalent run → debug-free `git worktree` of the branch (copy `local.properties` into it).
