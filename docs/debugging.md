# Debugging on a phone

Getting logs and repro evidence from a sideloaded release build. App id: `lk.trainlanka.app`.

## Debug logs

Temporary, for device testing; removed before release. Claude does the same steps via the `debug-logs` skill (`/debug-logs add <bug> | list | remove`) — keep both in sync.

Read them live (tag `TrainLanka`, kept in release builds while present):

```bash
adb logcat -s TrainLanka
```

Save after reproducing:

```bash
adb logcat -d -s TrainLanka > trainlanka.txt
```

Crashes are logged by Android (`FATAL EXCEPTION`), not under this tag → also capture full logcat or crash buffer.

### Rules

Removal must be deleting marked lines only — nothing to rewrite or restore.

- **Add-only.** Insert new lines; never edit, split or restructure existing ones. A log needing a rewrite → log just before/after, or skip.
- **One line per call.** `debugLog(message, error = null)` from `app/src/main/java/lk/trainlanka/app/DebugLog.kt`, whole call on one line. Never `android.util.Log` elsewhere.
- **Never load-bearing.** Not the only statement of a branch/lambda/body, not inside an expression, not in a brace-less `if`/`else`. Deleting the line must leave valid, same-behavior code.
- **Mark other debug code** (effect, listener, variable, import): wrap in `// debug-log start` … `// debug-log end`, or end the line with `// debug-log`.
- Message: area prefix + what happened + key values, e.g. `"Widget update: route $route, ${trips.size} trips"`. Pass caught exceptions as `error`.
- Not in composable bodies (re-run every recomposition) → callbacks, effects, ViewModel.

### Add

1. `DebugLog.kt` missing → copy `.claude/skills/debug-logs/references/DebugLog.kt` to `app/src/main/java/lk/trainlanka/app/`.
2. Add `debugLog(...)` lines at suspected failure points: branches, early returns, catch blocks, state changes, navigation, background work entry.

### List current logs

```bash
grep -rn "debugLog\|debug-log" app/src
```

### Remove

1. Delete every line containing `debugLog` (calls + imports).
2. Delete every `// debug-log start` … `// debug-log end` block (markers included) and every line ending `// debug-log`.
3. Delete `DebugLog.kt`.
4. This must print nothing:

```bash
grep -rn "debugLog\|DebugLog\|debug-log" app/src
```

5. `git diff` of app code must show deletions only (else a rule was broken when adding). Then build.

## Developer options (ColorOS)

1. Settings → About device → Version → tap **Build number** 7 times.
2. Settings → Additional settings → **Developer options**.
3. Turn on **Show taps** (records every touch as a dot in screen recordings).

Menu paths move between ColorOS versions → search Settings for "Build number" / "Developer options".

## adb on Windows

Path: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. Git Bash:

```bash
export PATH="$PATH:$LOCALAPPDATA/Android/Sdk/platform-tools"
```

### USB debugging

1. Developer options → **USB debugging** on.
2. Connect cable; on the phone accept **Allow USB debugging** (tick "Always allow").
3. Check connection:

```bash
adb devices
```

Shows `<serial>  device`. `unauthorized` → accept the prompt on the phone.

### Wireless debugging

PC and phone on the same Wi-Fi.

1. Developer options → **Wireless debugging** on → **Pair device with pairing code**. Note IP:port and 6-digit code.
2. Pair (once per PC):

```bash
adb pair 192.168.1.20:37000
```

3. Connect using the IP:port shown on the main Wireless debugging screen (different port from pairing):

```bash
adb connect 192.168.1.20:41000
```

Wireless debugging turns off on Wi-Fi change or reboot → reconnect.

## Capture logs

Clear old logs, reproduce, then save:

```bash
adb logcat -c
```

```bash
adb logcat -d > logcat.txt
```

App process only (app must be running):

```bash
adb logcat -d --pid=$(adb shell pidof -s lk.trainlanka.app) > app.txt
```

Crashes only:

```bash
adb logcat -b crash -d > crash.txt
```

Live, while reproducing:

```bash
adb logcat --pid=$(adb shell pidof -s lk.trainlanka.app)
```

Look for `FATAL EXCEPTION`, `AndroidRuntime`, `lk.trainlanka.app`.

## Screen recording

Phone's built-in recorder (quick settings) works; with **Show taps** on it shows exactly what was tapped. Or via adb (max 3 min, stop with Ctrl+C):

```bash
adb shell screenrecord /sdcard/repro.mp4
```

```bash
adb pull /sdcard/repro.mp4
```

## Without a PC

- **Bug report:** Developer options → **Take bug report** → Interactive report. Full system logs as a zip, shared from the notification. Large, but includes logcat around the time taken; take it right after reproducing.
- **Logcat reader app** (Play Store / F-Droid): needs `READ_LOGS`, which can only be granted once from a PC:

```bash
adb shell pm grant <reader.package.id> android.permission.READ_LOGS
```

After that it reads logs on the phone alone. Recent Android may still show a one-time "allow access to device logs" prompt — accept it.

## What to send with a bug

- Screen recording with Show taps on.
- `trainlanka.txt` (app debug logs, while present); for crashes also `crash.txt` or `logcat.txt`. No PC → bug report taken right after.
- Steps, screen you were on, app version and timetable version (both on the Settings screen).
