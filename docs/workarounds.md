# Workarounds and skipped items

Known limitations, workarounds and deliberately skipped work. Update when adding or removing one.

## Data source

| Item | Workaround | Risk / revisit |
|---|---|---|
| No official machine-readable timetable (SLR `eservices.railway.gov.lk` 404; railway.gov.lk posts image PDFs only; ICTA Lanka Gate API not public) | Crawl trainschedule.lk | Data used without the owner's permission. Ask them (`/Home/Contact`) or swap source before Play Store release |
| No JSON API on trainschedule.lk | Parse server-rendered HTML + embedded `stationsData` JSON | Markup change breaks crawler → Action fails, last good data kept |
| URL codes are opaque | Reverse-engineered in `crawler/src/codes.ts` | Site changing its encoding → 404s → Action fails |
| Full-route stops link (`...-train-stops-for-{no}`) always returns empty data | Use pair stops page origin → terminus | — |
| Train type only on route schedule pages (date-specific) | Query up to 14 upcoming dates per route | Train running only beyond 14 days → dropped |
| Train numbers not unique | Key trains by site's internal ID | — |

## Data quality

| Item | Workaround |
|---|---|
| Origin arrival / terminus departure contain the train's previous/next service times | Use origin departure and terminus arrival only |
| Times stepping back (typos) | ≤15 min → clamped to previous stop's time; more → train dropped, logged |
| Trains with no running days (discontinued) or Poya/holiday-only | Dropped, logged. Site search hides them too |
| Trains listed but hidden by site search (e.g. 2606 Peradeniya → Kandy, section suspended) | Dropped, logged |
| Stations without coordinates: Eratperiakulam, Anuradhapura Town, Mollipatana, Miriswatte, St kylas, Hataras Kotuwa, Weralugolla | Published with `lat`/`lng`/`province` `null`. App: "Other" group, excluded from nearest station, toast on location actions. Fix via `data/stations-extra.json` |
| Vandaramullai coordinates outside all province polygons | `province: null`. Fix via `data/stations-extra.json` |
| Coastal stations just outside simplified province polygons | Nearest province if ≤2 km away |

## Skipped (v1)

- Poya/public holiday filtering: weekday-only; app shows "May differ on Poya/public holidays".
- Fares, booking, live tracking, delays.
- Sinhala/Tamil UI and script search (placeholders in settings).
- Aliases beyond Pettah → Colombo Fort.
- iOS.
