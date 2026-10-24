# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Train Lanka

Offline Sri Lankan train timetables for Android (minSdk 29). Kotlin + Jetpack Compose app, Node/TS crawler, data via GitHub Action. No booking, live tracking or fares. Timetable ships in the APK; app works offline from first launch.

| Path | What | Detail |
| --- | --- | --- |
| `app/` | Android app | `.claude/rules/app.md` |
| `crawler/` | Node/TS crawler → `data/timetable.json` | `.claude/rules/crawler-data.md` |
| `data/timetable.json` | Generated timetable, never hand-edit | `.claude/rules/crawler-data.md` |
| `data/stations-extra.json` | Hand-kept aliases, province/lat/lng overrides | `.claude/rules/crawler-data.md` |
| `.github/workflows/app.yml` | PR checks: ktlint, Android lint, release build, APK artifact | `gradle-checks` skill |
| `.github/workflows/crawl.yml` | Weekly crawl, commits timetable if changed | `.claude/rules/crawler-data.md` |
| `.github/workflows/release.yml` | Manual release from master: typed version → signed `TrainLanka.apk` on a GitHub release | — |
| `.github/workflows/pages.yml` | Deploys `site/` to GitHub Pages on master | — |
| `site/` | Static landing page, Download links to `releases/latest/download/TrainLanka.apk` | — |

- Intentional code change that makes `.claude/rules/*.md`, a `.claude/skills/` file or this file wrong → update it in the same change.
- Run ktlint / Android lint / release build → `gradle-checks` skill.
- Temporary debug logs (`debugLog(...)`, `DebugLog.kt`) → `debug-logs` skill; never commit them.
- All time logic uses `Asia/Colombo`, not device zone.
- Timetable schema change → crawler, `app/.../data/Timetable.kt` and app change together; installed apps download the new file from `master`.
