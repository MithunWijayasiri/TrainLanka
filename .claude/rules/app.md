---
paths:
  - "app/**"
---

# App architecture (`app/src/main/java/lk/trainlanka/app`)

Single-activity Compose, no DI framework. `TrainLankaApp` holds singletons (`prefs`, `timetables`, app `scope`); access via `TrainLankaApp.from(context)`.

- `data/TimetableRepository`: current timetable = newer of bundled asset and `filesDir/timetable.json`. Update check downloads to `timetable-pending.json` (never replaces current directly); `applyPending()` renames it to current. `Mutex` guards file swaps.
  - Background (`UpdateWorker`, daily, network-constrained): download only → home banner offers Update.
  - Manual check (Settings, `AppViewModel.checkNow`): applies immediately.
  - After apply, routes/picked stations missing from new timetable are filtered/cleared.
- `data/Prefs`: DataStore; starred/recent routes stored as `"from-to,..."` strings. Recent max 3, never contains starred.
- `domain/Trips.kt`: trip computation in `Asia/Colombo` (`nowInColombo()`). `tripsOn` includes trains that started the previous day; `upcoming` rolls over to next day with trips (≤ 7 days).
- `domain/StationSearch.kt`: name/alias matching with misspelling tolerance.
- `ui/AppViewModel`: one shared `AndroidViewModel` for all screens (picked from/to, settings, starred/recent, update state, minute-ticking `now`).
- `ui/AppNav` + `ui/Routes.kt`: type-safe Navigation Compose (`@Serializable` destinations). Nav actions wrapped in `ifResumed` so repeated taps mid-transition can't pop Home. Widget intents (`EXTRA_FROM/TO/TRAIN`) arrive via `MainActivity` channel → open Results (and Stops).
- `widget/RouteWidget`: Glance 4×2 widget, next 3 departures of first starred route. Self-schedules refresh with `AlarmManager` (`WidgetRefreshReceiver`). Any change to starred routes, clock format or timetable → call `RouteWidget.refresh`.
- `location/`: single on-demand location fix only on Nearest tap; never on start or in background.
- `ui/theme/Theme.kt`: Material 3, Light/Dark/System, Atkinson Hyperlegible font (OFL; `app/licenses/` shipped as assets).
