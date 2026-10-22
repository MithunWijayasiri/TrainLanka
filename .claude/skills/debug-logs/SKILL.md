---
name: debug-logs
description: Add, list or remove temporary debug logs (`debugLog(...)`) in the Train Lanka Android app. Use when a device bug needs logs, when asked to add/list/remove/strip debug logs, and when preparing a release, release build to publish, or merge to master (remind only).
argument-hint: "add <bug or area> | list | remove"
---

# Debug logs

Temporary logging for device testing, removed before release. Manual version of these steps: `docs/debugging.md` → "Debug logs" — keep both in sync.

## Rules

Goal: removal = deleting marked lines only, no judgment, nothing missed, no code to restore. Every rule below serves that.

- **Add-only.** Insert new lines; never edit, split, reformat or restructure an existing line. A log that needs a rewrite to fit → log just before/after instead, or skip it.
- **One line per call.** `debugLog(...)` from `app/src/main/java/lk/trainlanka/app/DebugLog.kt` (tag `TrainLanka`), whole call on one line (long is fine). Never `android.util.Log` elsewhere.
- **Never load-bearing.** A `debugLog` line is never the only statement of a branch/lambda/body, never part of an expression, never in an `if`/`else` without braces. Deleting the line must leave valid code with the same behavior.
- **Mark other debug code.** Anything else that exists only for logging (effect, listener, variable, import) → wrap in `// debug-log start` … `// debug-log end` lines, or end a single line with `// debug-log`.
- **Message:** area prefix + what happened + key values (ids, versions, lifecycle state), e.g. `"Widget update: route $route, ${trips.size} trips"`. Catch blocks: pass the exception as `error`.
- **Not in composable bodies** — they re-run on every recomposition and flood logcat. Log in callbacks, effects or the ViewModel.

## Add

1. `DebugLog.kt` missing → copy `references/DebugLog.kt` there unchanged.
2. Place logs where the bug could come from: branch decisions, early returns, catch blocks, state changes, navigation, entry of background work (worker, receivers, alarms).
3. Tell the user the filter command `adb logcat -s TrainLanka` and what each new log shows. Build only if asked (check-bot).

## List

```bash
grep -rn "debugLog\|debug-log" app/src
```

Report grouped by file/area, not raw output.

## Remove

Only when the user asks. That request covers deleting `DebugLog.kt`.

1. Delete every line containing `debugLog` (calls and `import lk.trainlanka.app.debugLog`).
2. Delete every `// debug-log start` … `// debug-log end` block (markers included) and every line ending `// debug-log`.
3. Delete `DebugLog.kt`.
4. Check — this must print nothing:

```bash
grep -rn "debugLog\|DebugLog\|debug-log" app/src
```

5. `git diff` of app code must be deletions only. Any edited line → a rule was broken when adding; fix it and tell the user.
6. Report files changed; offer a build via check-bot.

## Release reminder

User asks for a release, a release build to publish, a GitHub release/tag, or a merge to master → run List. Anything found → tell the user the count and ask whether to remove. Never remove without that answer.
