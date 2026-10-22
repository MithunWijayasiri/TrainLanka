---
paths:
  - "crawler/**"
  - "data/**"
  - ".github/workflows/crawl.yml"
  - "app/src/main/java/lk/trainlanka/app/data/Timetable.kt"
  - "app/src/main/java/lk/trainlanka/app/data/TimetableRepository.kt"
---

# Crawler and timetable data

## Commands

From `crawler/` (Node ≥ 22.18, runs `.ts` directly via type stripping, no build step):

| Task | Command |
| --- | --- |
| Crawl → `data/timetable.json` | `npm run crawl` |
| Typecheck | `npm run typecheck` |
| Reuse responses across runs (dev) | `CACHE_DIR=<dir> npm run crawl` |

Full crawl ≈ 1000+ requests at 1/s (`http.ts` `DELAY_MS`) → long; use `CACHE_DIR` when iterating.

## Data flow

```
trainschedule.lk ─crawler─▶ data/timetable.json ─copyTimetable─▶ APK assets
                                   │
                                   └─ raw.githubusercontent.com/.../master/data/timetable.json ─▶ in-app update
```

- `crawl.yml`: weekly (Mon 02:00 Colombo) + manual. `crawl` job has no write token (npm deps run there); separate `publish` job commits `data/timetable.json` only if changed.
- `data/timetable.json` is generated — never hand-edit. Hand-kept overrides (aliases, province, lat/lng) → `data/stations-extra.json`, keyed by exact station name.
- `app/build.gradle.kts` `copyTimetable` task copies `data/timetable.json` into generated assets.

## Schema

`{ version: "YYYY-MM-DD", stations: [...], trains: [...] }`, one station/train per line (readable diffs).

- Station: `id, name, province, lat, lng, aliases, phone`.
- Train: `id, no, type, days, stops`; `stops` = `[stationId, arr, dep]` in minutes since midnight of the train's **start day**, can exceed 1440 (overnight).
- `id` = site's internal train ID; train numbers (`no`) not unique.
- `days`: `Mon`…`Sun`, `Poya`, `Holi`. App filters by weekday only; Poya/holiday tokens ignored.
- `version` compared as string (ISO date) to pick newest timetable.
- `Timetable.parse` (app) validates; schema change → crawler, `Timetable.kt` and app change together.

## Crawler (`crawler/src`)

- `codes.ts`: reverse-engineered trainschedule.lk URL codes (base-36 ids + check chars) for `/station`, `/stops`, `/schedule`.
- `crawl.ts` pipeline: home page → station list; station pages → trains + phones; stops pages → routes + coords; schedule pages (next 14 dates) → train types. `province.ts` assigns provinces from pinned geoBoundaries ADM1 GeoJSON.
- Fail-fast by design: unknown station/day, train data differing between pages, < 300 stations, < `MIN_TRAINS` trains, > `MAX_UNLOCATED` unlocated stations → exit 1, nothing written.
- Per-train data problems (backward times, big gaps, missing times, no weekday days, absent from site search) → train dropped and logged, not fatal. Backward times ≤ 15 min are clamped; ≥ 12 h = midnight wrap.
- Origin arrival / terminus departure on the site belong to adjacent services → crawler uses origin dep and terminus arr only.
- Writes file only if stations/trains changed; `version` = today in Colombo.
