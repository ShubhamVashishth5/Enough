# Minimisation Machine — Implementation Plan

Nov 23, 2026 · @Shubham

## How to use this plan

Build one step at a time, in order. Every step ends with an app that installs and runs on a real phone. The product spec is the [feature draft](https://claude.ai/artifact/QwauRfMuEc278B4Yb4mdew); feature names here match it.

Rules for whoever (or whatever agent) is building:

1. Read sections 2 to 5 before writing code. They define the architecture every step plugs into.
2. One step per session or commit. Don't start the next step until the current one meets its **Done when** and passes the device checklist (last section).
3. Build only what the step asks. No extra screens, settings or abstractions beyond the ones in section 4.
4. Anything that delays or blocks an app is a new **Rule** (section 4). Never special-case it inside the accessibility service or an Activity.
5. Anything that gates an unlock (waiting, NFC tag, QR, friend code, typing) is a new **UnlockMethod**.
6. If a step is unclear, pick the simplest option that fits the architecture and leave a `// TODO(question): ...` comment.
7. No network permission, no analytics, no Google Play Services or Firebase. Everything stays on the phone, which also keeps F-Droid open as a release channel.

### Terms

| Term | Meaning |
| --- | --- |
| Tool app | An app the user keeps freely available (Phone, Maps, Camera) |
| Vault app | An app tagged as distracting. Hidden from home and search, opened only through the gate |
| Gate | The check that runs whenever a Vault app comes to the foreground |
| Pause screen | Full-screen breathing timer plus "What are you here for?" shown by the gate |
| Time box | The minutes chosen after a pause. When it ends, the user is sent home |
| Budget | One shared daily allowance of minutes across all Vault apps |
| Tighten / loosen | A settings change that makes rules stricter (applies now) or weaker (applies after a delay) |
| Day boundary | Days reset at 04:00 local time, not midnight |

## Architecture at a glance

Every feature that limits use plugs into one path: an app open is detected, the GateEngine decides, an intervention shows, and the outcome is logged.

&#91;embedded content: architecture · the path of one app open\]

Opens from the launcher reach the gate before the app starts; opens from anywhere else are caught by the accessibility service. Rules read settings and the event log, and from Phase 3 an unlock method can apply a pending settings change early.

## Tech stack and project structure

One Gradle module, organised by feature, using standard Android libraries an agent already knows well.

| Concern | Choice | Note |
| --- | --- | --- |
| Language | Kotlin | Coroutines and Flow for all async work |
| UI | Jetpack Compose, Material 3 | One monochrome theme, text-first, no icons on home |
| Pattern | MVVM, one-way data flow | ViewModel exposes a single `StateFlow<UiState>` and takes events as function calls |
| Dependency injection | Hilt | Every interface in section 4 is bound in a Hilt module |
| Database | Room | Events, apps, tasks, pending changes, captured notes |
| Simple settings | DataStore (Preferences) | Scalars only: budget minutes, wait steps, bedtime |
| Timed work | AlarmManager | Only where something must happen at a set time (the digest). Everything else is computed lazily |
| QR scanning | ZXing (zxing-android-embedded) | Open source, no Google Play Services |
| NFC | Android `NfcAdapter` | Platform API, no library |
| Logging | Timber | Debug builds only |
| SDK levels | minSdk 29, target the latest stable | Private Space features check for API 35 at runtime |
| Build | Gradle Kotlin DSL, version catalog | All versions in `libs.versions.toml` |

Package layout (rename `app.minimise` to taste):

```text
app/src/main/java/app/minimise/
├── MinimiseApp.kt          Application class, Hilt entry point
├── core/
│   ├── time/               TimeSource, DayBoundary
│   ├── db/                 AppDatabase, DAOs, migrations
│   ├── prefs/              SettingsStore (DataStore wrapper)
│   ├── di/                 Hilt modules
│   └── ui/                 theme, shared composables
├── apps/                   installed apps, Tool vs Vault classification
├── launcher/               home screen, search, today line
├── monitor/                ForegroundAppSource, accessibility service
├── gate/                   GateEngine, Rule, Decision, rules/
├── intervention/           pause, time's-up and blocked screens
├── unlock/                 UnlockMethod and its implementations
├── usage/                  EventLog, budget and unlock counters
├── tasks/                  today's tasks
├── settings/               settings screens, SettingsGuard
└── later phases add:       digest/ display/ focus/ capture/ keys/ feeds/ stats/
```

Inside a feature, add `ui/`, `domain/` and `data/` subpackages only when the feature needs them. Dependencies point one way: `ui` uses `domain`, `data` implements `domain`. Features talk to each other only through `domain` interfaces. Android components (services, activities, receivers) are thin adapters that hand work to `domain` classes.

## Core building blocks

Six abstractions carry the whole app. Later phases add implementations of these; they almost never change the abstractions themselves. The signatures below are sketches: keep the shape, adjust names as needed.

### 1. TimeSource

All code asks `TimeSource` for time, never `System.currentTimeMillis()` directly. This makes the clock-tamper protection in Phase 3 a one-class change.

```kotlin
interface TimeSource {
    fun now(): Instant        // wall clock: display, day boundary, schedules
    fun elapsed(): Duration   // monotonic (SystemClock.elapsedRealtime): timers, waits
}
```

`DayBoundary.dayOf(instant)` returns the "usage day", which starts at 04:00 local time.

### 2. Apps and their kind

```kotlin
enum class AppKind { TOOL, VAULT, HIDDEN }
data class AppEntry(val packageName: String, val label: String, val kind: AppKind, val homeSlot: Int?)
```

`AppRepository` merges installed apps (via `LauncherApps` plus package add/remove broadcasts) with the stored kind for each package. New installs default to `TOOL` until Phase 2 adds auto-Vault.

### 3. ForegroundAppSource

```kotlin
interface ForegroundAppSource { val events: Flow<ForegroundEvent> }  // packageName, at
```

The accessibility service is the only producer. It filters out its own package, the launcher and system UI, and emits only real app changes. A `UsageTracker` turns these events into time spans per app and writes them to the event log.

### 4. The gate: Rule, Decision, GateEngine

Every blocking or delaying feature is a `Rule`. Rules are registered with a Hilt `@IntoSet` binding, so adding a feature means adding one class and one binding.

```kotlin
interface Rule {
    val id: String
    suspend fun evaluate(ctx: GateContext): Decision
}

data class GateContext(val app: AppEntry, val today: TodayUsage, val now: Instant)

sealed interface Decision {
    data object Allow : Decision
    data class Pause(val wait: Duration) : Decision
    data class Block(val reason: String, val overrides: List<UnlockMethodId>) : Decision
}
```

`GateEngine.check(app)` runs every enabled rule and keeps the strictest result: any `Block` wins, otherwise the longest `Pause`, otherwise `Allow`. An active **grant** (a running time box for that app) short-circuits to `Allow`. `GrantStore` keeps grants in Room so they survive the service restarting.

### 5. Interventions and UnlockMethods

`InterventionLauncher` turns a decision into a screen: `PauseActivity`, `TimeUpActivity` or `BlockedActivity`. Each is a full-screen Activity started from the accessibility service and excluded from recents. Each screen reports one outcome back (`Proceeded(timeBox)`, `Cancelled`, `Overridden(method)`), which is written to the event log and, if access is granted, to `GrantStore`. Cancelling always sends the user to the home screen.

```kotlin
interface UnlockMethod {
    val id: UnlockMethodId          // WAIT, NFC_KEY, QR_KEY, FRIEND_CODE, TYPING, EMERGENCY_PASS
    suspend fun isAvailable(): Boolean
}
```

Each method has a domain class (validates the attempt) and a composable screen, both registered in a map keyed by `UnlockMethodId`. Phase 1 has only `WAIT`.

### 6. Event log and SettingsGuard

**EventLog** is one Room table that every feature writes to and every stat is computed from. Never store a counter that can be derived from events.

| Field | Type | Note |
| --- | --- | --- |
| id | Long | Auto |
| type | enum | `SCREEN_UNLOCK`, `APP_SPAN`, `GATE_SHOWN`, `GATE_PROCEEDED`, `GATE_CANCELLED`, `TIME_UP`, `OVERRIDE_USED`, `SETTING_CHANGED`, more later |
| packageName | String? | When an app is involved |
| startAt / endAt | Instant | `endAt` only for spans |
| data | String? | Small JSON for extras (chosen reason, time box minutes) |

**SettingsGuard** is the only way to change a rule setting.

```kotlin
sealed interface SettingChange { val direction: Direction }   // TIGHTEN or LOOSEN

class SettingsGuard {
    suspend fun request(change: SettingChange): GuardResult    // AppliedNow | Scheduled(applyAt) | Refused(reason)
    suspend fun applyDue()                                     // applies scheduled changes whose time has come
}
```

Tightening applies at once. Loosening is stored in a `pending_changes` table with `applyAt = now + 24h`. `applyDue()` runs lazily: on app start, on screen unlock and before each gate check, so no background job is needed.

### Database tables by the end of Phase 1

| Table | Holds |
| --- | --- |
| `apps` | packageName, kind, homeSlot |
| `events` | the event log above |
| `grants` | packageName, startAt, endAt |
| `pending_changes` | change type, JSON payload, requestedAt, applyAt |
| `tasks` | text, day, done, order |

Later phases add their own tables (buffered notifications, notes, keys, modes, schedules). Use Room auto-migrations where possible.

## Conventions

Prefer boring, readable code that a new agent session can understand without context.

Code:

- One job per class. Keep files under about 300 lines; split when they grow.
- Use the domain words from the Terms table in class and function names (`VaultRepository`, `TimeBox`, `BudgetRule`). No abbreviations.
- `domain` packages import no Android classes, apart from `java.time`. This keeps rules plain Kotlin and easy to test.
- State is immutable data classes. Each ViewModel exposes one `UiState`; composables are stateless and take state plus callbacks.
- Expected outcomes are sealed result types (`GuardResult`, `Decision`). Exceptions are for bugs only.
- The accessibility service must never crash. Wrap each event handler in `runCatching`, log the failure and carry on.
- All tunable numbers (wait steps, default budget, time box options, digest times) live in one `Defaults.kt`.
- User-facing text goes in `strings.xml`.
- Permissions: one `PermissionsScreen` lists each permission, its status and a button to the right system settings page. Features check status through a `PermissionStatus` class and turn themselves off cleanly when a permission is missing.

Testing, kept light:

- Unit tests only for pure logic: `GateEngine`, each `Rule`, `SettingsGuard`, `DayBoundary` and budget maths. Plain JUnit with a fake `TimeSource`.
- No UI or instrumentation tests. Device behaviour is covered by the checklist in the last section.

Git: one commit per step, named after it (`Phase 1.4: pause screen`).

## Phase 0: project setup

A buildable skeleton with the core plumbing in place and no features yet.

### 0.1 Create the project

- New Android project, Compose, Kotlin DSL, version catalog.
- Add Hilt, Room (with KSP), DataStore, Coroutines, Navigation Compose and Timber.
- No `INTERNET` permission, now or later.

### 0.2 Core plumbing

- `MinimiseApp` with `@HiltAndroidApp`; Timber planted in debug builds only.
- `core/time`: `TimeSource`, `SystemTimeSource`, `DayBoundary` (04:00 cutoff), plus a debug-only time offset for testing day resets.
- `core/db`: `AppDatabase` with the `events` table and `EventLog` (insert plus a few `Flow` queries).
- `core/prefs`: `SettingsStore` wrapping DataStore.
- `core/ui`: a monochrome theme (black background, white text, one typeface) and dark mode as the default.
- `Defaults.kt` with the starting numbers: budget 45 min, wait steps 10s / 20s / 40s / 60s / 120s, time box options 2 / 5 / 10 min, day reset 04:00.

**Done when:** the app installs, opens a blank themed screen, and the `DayBoundary` unit test passes.

## Phase 1: MVP

Phase 1 ends with a launcher you can live in: Vault apps sit behind a pause screen, a shared daily budget, and settings that are slow to loosen.

### 1.1 Launcher shell

- `HomeActivity` declares the `HOME` and `DEFAULT` intent categories, `launchMode="singleTask"`; Back does nothing on home. Declare a queries entry for launcher (MAIN/LAUNCHER) intents in the manifest so app listing works under Android 11+ package visibility.
- `AppRepository` lists launchable apps through `LauncherApps` (including work profile) and listens for installs and removals with `LauncherApps.Callback`. Store each app in the `apps` table; all start as `TOOL`.
- Home screen: clock and date at the top, then up to 6 home slots as plain text rows. Tap launches the app; long-press a row to pick a different Tool app for that slot.
- Ask to become the default home app with `RoleManager.createRequestRoleIntent(ROLE_HOME)`.

**Done when:** it's set as the default home app, shows 6 text rows, and the Home button always returns to it.

### 1.2 Search

- Swipe up on home opens search with the keyboard already up.
- `TOOL` apps match on any part of the label. When exactly one Tool app matches, launch it at once.
- `HIDDEN` apps never show.
- `VAULT` apps show only when the typed text equals the full label (ignoring case), and they never auto-launch.

**Done when:** typing "ma" opens Maps if it's the only match, and Instagram stays invisible until "instagram" is typed in full.

### 1.3 App kinds (the Vault)

- Long-press empty space on home to open Settings. First item: **Apps**, a list of every app with a Tool / Vault / Hidden choice.
- Vault and Hidden apps disappear from home slots.
- For now, write changes straight to the `apps` table. Step 1.4 routes them through the guard.

**Done when:** tagging Instagram as Vault removes it from home and from partial search results.

### 1.4 SettingsGuard (loosen-later rule)

- Implement `SettingsGuard`, the `pending_changes` table and two change types: `SetAppKind` and `SetBudget`.
- Direction: Tool to Vault or Hidden is tighten; Vault to Tool is loosen; a lower budget is tighten, a higher one loosen.
- Settings shows a **Pending changes** list with the time each applies. A pending loosen can be cancelled at any time.
- Call `applyDue()` on app start and screen unlock (the gate call is added in 1.6).
- Debug builds get a hidden switch that shortens the 24-hour delay to 1 minute.

**Done when:** moving an app out of the Vault shows "Applies in 24 h", and with the debug delay it applies after a minute.

### 1.5 Foreground monitor, usage tracking, permissions

- `GateAccessibilityService` listens for `TYPE_WINDOW_STATE_CHANGED` and pushes `ForegroundEvent`s into a Hilt singleton that implements `ForegroundAppSource`. Ignore its own package, the system UI and keyboard (IME) packages.
- Inside the service, register receivers for `SCREEN_OFF` (closes the current span) and `USER_PRESENT` (writes a `SCREEN_UNLOCK` event).
- `UsageTracker` writes one `APP_SPAN` event per continuous stretch of app use.
- `PermissionsScreen` covers: accessibility (with a note about "Allow restricted settings" for sideloaded installs), default home app, and battery optimisation exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).
- A debug-only screen lists today's spans and unlock count.

**Done when:** after using a few apps and unlocking several times, the debug screen shows accurate spans and the right unlock count, and it survives a reboot.

### 1.6 Gate and pause screen

- `GateEngine` with the Rule set, plus the first rule, `VaultPauseRule`: a Vault app gets `Pause(wait)`. The wait steps up with each Vault open already proceeded today, using the steps from `Defaults.kt`.
- From the launcher or search: tapping a Vault app shows `PauseActivity` first and starts the app only after the user proceeds. That way the app never flashes on screen.
- From anywhere else (notification, recents, another app): the service sees the Vault app, asks `GateEngine`, and `InterventionLauncher` shows `PauseActivity` on top.
- `PauseActivity`: breathing animation with a countdown, "Open 4 today", then the reason choices (Reply to someone, Look one thing up, Bored, Habit). Cancel goes home and logs `GATE_CANCELLED`.
- Call `SettingsGuard.applyDue()` before every gate check.

**Done when:** opening a Vault app from search, a notification and recents all show the pause screen, Cancel goes home, and the wait grows with each open.

### 1.7 Time box and time's up

- After the reason, the user picks 2, 5 or 10 minutes. `GrantStore` saves a grant until `now + timeBox`, then the app opens (or the pause screen closes to reveal it).
- While a granted Vault app is in front, the service runs a coroutine that watches the grant. When it ends, show `TimeUpActivity` ("Time's up") and go home.
- Leaving and coming back within the grant is allowed. Opening again after it ends means a new pause at the next wait step.

**Done when:** choosing 2 minutes sends you home after 2 minutes, and the next open has a longer wait.

### 1.8 Daily budget

- `BudgetRule`: add up today's `APP_SPAN` time for Vault apps. At or over budget, return `Block("Daily budget used", overrides = emptyList())`.
- The pause screen shows the budget left, and time box choices are capped at it.
- The grant watcher also ends the session when the budget runs out mid-box.
- Budget changes go through `SettingsGuard`.

**Done when:** with a 5-minute budget, Vault apps block after 5 minutes in total across all of them, and raising the budget shows it as pending.

### 1.9 Today line, tasks and the two numbers

- The first unlock of each usage day opens a skippable **Today** screen: enter 1 to 3 tasks.
- Home shows the tasks under the clock. Tap to tick off, long-press to edit.
- A footer line on home: "Vault 23 min · Unlock 31".

**Done when:** the first unlock after 04:00 asks for tasks, they appear on home, and the footer numbers match the debug screen.

### 1.10 Onboarding

- First run: welcome, then permissions, then pick home apps, then tag Vault apps, then set the budget.
- On the Vault step, pre-tick installed apps from a short built-in list of common time sinks (Instagram, YouTube, Facebook, X, Reddit, Snapchat, Netflix, a few games).

**Done when:** a fresh install reaches a fully working setup in under 2 minutes.

## Phase 2: v1 (daily driver)

Phase 2 adds the evidence-backed extras and the first real unlock methods. Almost every step is a new `Rule`, a new `UnlockMethod` or a new screen; the core from Phase 1 should barely change.

### 2.1 Instead list

- `swaps` table: text plus an optional linked Tool app ("Read 5 pages" opens the e-book app).
- When the pause screen reason is Bored or Habit, show one swap (rotate through the list) with **Do this instead** and **Open anyway**. Do this instead opens the linked app or goes home, and logs `SWAP_TAKEN`.
- Settings: edit the list.

**Done when:** picking Habit offers a swap, and taking it never opens the Vault app.

### 2.2 Emergency pass and open limits

- `EmergencyPassMethod` (first `UnlockMethod` beyond `WAIT`): 3 passes per week from `Defaults`, each logged as `OVERRIDE_USED`.
- `BlockedActivity` lists the overrides a `Block` allows, each drawn from the method registry. `BudgetRule` now allows `EMERGENCY_PASS`.
- `OpenLimitRule`: optional per-app daily cap on opens. Over the cap returns `Block` with `EMERGENCY_PASS`. Limits are set in the Apps screen and go through `SettingsGuard`.

**Done when:** with the budget used up, an emergency pass grants one time box, and the remaining pass count drops.

### 2.3 Schedules and the first-hour rule

- `schedules` table: name, days of week, start, end. `ScheduleRule` blocks all Vault apps inside any active window, with overrides `NFC_KEY`, `QR_KEY` (from 2.4) and `EMERGENCY_PASS`.
- `FirstHourRule`: blocks Vault apps until 60 minutes after the day's first `SCREEN_UNLOCK`.
- New `SettingChange` types for adding, editing and removing schedules. Removing or shrinking a window is loosening.

**Done when:** a 22:00 to 07:00 schedule blocks Vault apps overnight, and Vault apps stay blocked for the first hour after the morning unlock.

### 2.4 Physical key (NFC and QR)

- `keys` table: type, label, hashed secret.
- NFC pairing: read a tag's ID in reader mode (`NfcAdapter.enableReaderMode`) and store its hash. QR pairing: generate a random secret, show it as a QR to print or screenshot, and store its hash.
- `NfcKeyMethod` and `QrKeyMethod` check a tap or scan against stored hashes. QR scanning uses ZXing.
- Adding a key is tightening; removing one is loosening.

**Done when:** a scheduled block can be ended only by tapping the paired sticker or scanning the printed code (or a pass).

### 2.5 Grayscale

- `DisplayFilter` interface with two implementations: `SecureSettingsDisplayFilter` (writes Android's colour-correction settings to monochrome) and `NoOpDisplayFilter`.
- It needs `WRITE_SECURE_SETTINGS`, which only ADB or Shizuku can grant. `PermissionsScreen` shows the exact `adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS` command and detects when it's granted.
- The service turns grayscale on while a Vault app is in front and off otherwise. Option: always grayscale. Save and restore the user's previous colour-correction state.

**Done when:** Instagram shows in grayscale, home and Maps in colour, and turning the feature off restores the original setting.

### 2.6 Focus sessions

- `FocusSession`: start, end, allowlist, locked flag. Start from a **Focus** item on home with 25, 50 or 90 minutes.
- `FocusRule`: during a session, any app not on the allowlist (Tool apps included) is blocked. Unlocked sessions offer `WAIT` (120 s); locked sessions offer only `NFC_KEY` or `QR_KEY`.
- Turn on Do Not Disturb with `NotificationManager.setInterruptionFilter` (needs notification policy access) and grayscale if available. Restore both at the end.
- Log `FOCUS_SESSION` with its length. Show a one-line summary when it ends.

**Done when:** in a locked 25-minute session only allowlisted apps open, and ending it early needs the key.

### 2.7 Modes

- `modes` table: name, allowlist, budget, wait steps. Ship Work, Study, Home and Sleep.
- Rules read the active mode's values through one `ActiveModeProvider`, so no rule needs to know about modes directly.
- Switch modes manually or by schedule. Switching to a stricter mode is tightening; to a looser one is loosening.
- Optional: switch by Wi-Fi network name. That needs location permission, so keep it off by default.

**Done when:** Work mode from 10:00 to 18:00 applies its smaller budget, and switching to Home early shows as pending.

### 2.8 Notification digest

- `DigestListener` (`NotificationListenerService`). For apps not on the exempt list, save the notification to `buffered_notifications` (package, title, text, time) and cancel it. Calls (`CATEGORY_CALL`) and exempt apps always pass straight through.
- `AlarmManager` inexact window alarms at 09:00, 14:00 and 20:00 post one summary notification ("12 updates from 5 apps") that opens a **Digest** screen. Re-arm after each alarm and on `BOOT_COMPLETED`.
- The Digest screen groups items by app. Tapping one opens that app, through the gate if it's in the Vault.
- Home shows "4 updates waiting" without their content.

**Done when:** a WhatsApp group message (WhatsApp not exempt) waits until the next digest, while a phone call rings normally.

### 2.9 Quick capture, later inbox and gestures

- Swipe left on home opens a single text box; Enter saves to a `notes` table.
- Notes come back in an **Inbox** screen, shown at the end of a focus session and linked from the evening digest. Each note can be ticked, deleted or shared.
- Gestures: swipe right for camera, double-tap to lock the screen (accessibility `GLOBAL_ACTION_LOCK_SCREEN`), long-press the clock for a timer.

**Done when:** a thought captured mid-focus shows up in the inbox when the session ends.

### 2.10 Auto-Vault and calendar on the Today line

- On install, apps whose `ApplicationInfo.category` is social, video, game or news become Vault apps automatically (tightening, so immediate). Show a small notice.
- With calendar permission, the Today line shows the next event from `CalendarContract.Instances`.

**Done when:** installing a game puts it straight into the Vault, and the next meeting shows under the clock.

## Phase 3: v2 (hardening and release)

Phase 3 closes the loopholes, adds the remaining features from the spec, and gets the app ready to share. Feed and browser blocking read other apps' screens, so they need the most upkeep; keep their matching rules in data files, not code.

### 3.1 Feed blocking (Shorts, Reels, Explore)

- Turn on `canRetrieveWindowContent` and listen for `TYPE_WINDOW_CONTENT_CHANGED`, but only for packages that have an active feed rule.
- Feed rules live in `assets/feed_rules.json`: package, feature name ("YouTube Shorts"), matchers (view resource IDs, content descriptions or text) and an action (`BACK`, `HOME` or show a "Feed blocked" screen).
- `FeedMatcher` walks the node tree with a depth limit and at most one check every 300 ms.
- Settings lists each feed feature with a switch. Turning one off is loosening.
- Debug builds get a **Dump screen** action that logs the current window's node tree, to find new view IDs fast when an app updates.

**Done when:** opening Shorts bounces back while YouTube search and Instagram DMs still work.

### 3.2 Browser coverage

- Read the address bar of known browsers (Chrome, Firefox, Samsung Internet, Brave) using IDs kept in the same JSON format.
- Websites become **web targets** with ids like `web:instagram.com`. They are tagged Vault in the Apps screen and treated exactly like Vault apps by every rule, including the budget.

**Done when:** instagram.com in Chrome shows the pause screen and its time counts towards the shared budget.

### 3.3 Lock period, friend key and typing penance

- **Lock period:** `SettingsGuard` refuses every loosening change until `lockedUntil`. Starting a lock (7, 30 or 90 days) is tightening.
- **Friend key:** setup generates 10 one-time codes, shares them to a friend through the share sheet, and keeps only their hashes. The setup screen uses `FLAG_SECURE` so it can't be screenshotted. `FriendCodeMethod` accepts any unused code.
- **Typing penance:** `TypingMethod` asks for a random 300-character passage typed exactly. The input field rejects paste and large inserts.
- Outside a lock period, `SettingsGuard.applyNow(change, method)` lets a friend code, typing or physical key apply a pending loosen immediately.

**Done when:** during a lock nothing can be loosened, and outside one a friend code applies a pending change at once.

### 3.4 Uninstall guard and tamper-proof time

- Add a `DeviceAdminReceiver`. While the app is an active device admin, Android won't uninstall it until admin is switched off.
- During a lock period, the service sends the user home if they open: this app's App Info page, the device admin screen for it, or its accessibility settings. Keep these screen matchers in the JSON rules, since every phone brand's Settings app differs.
- `TamperAwareTimeSource` keeps an anchor of wall-clock time against `elapsedRealtime`. If the wall clock jumps by more than 5 minutes compared with elapsed time, it trusts the anchor. Swap it in through Hilt; nothing else changes.
- Add an **Honest limits** screen to onboarding: safe mode, factory reset and ADB can still get around everything.

**Done when:** during a lock you can't uninstall or disable the app from Settings, and moving the clock forward doesn't end a block.

### 3.5 Stats: weekly report, trigger log, export

- **Trigger log:** when a budget block appears, ask "What pulled you in?" (notification, boredom, stress, hopped from another app) and log a `TRIGGER` event.
- **Weekly report:** one screen built from events. This week vs last for Vault minutes, unlocks, focus minutes and tasks done; top 3 time sinks; slips by hour of day; "18 of 23 days under budget this month". A notification links to it on Sunday at 20:00.
- The detailed stats screen opens once per usage day (log `STATS_VIEWED`). Home keeps only the two numbers.
- **CSV export** of the event log through the system file picker (`ACTION_CREATE_DOCUMENT`), so no storage permission is needed.

**Done when:** the Sunday report matches the event log, and a second stats visit the same day shows "Come back tomorrow".

### 3.6 Taper, auto-tighten and earn-it

- **Taper:** on the first unlock of each week, lower the budget by 5% until it reaches the floor the user set. It's a tightening change applied automatically and logged.
- **Auto-tighten:** after 3 days in a row over budget, the Today screen offers a 10% smaller budget. Offer only; never force it.
- **Earn-it (off by default):** finishing a focus session or ticking a task logs a `BUDGET_BONUS` event that `BudgetRule` adds to today's budget.

**Done when:** a 45-minute budget becomes about 43 minutes the following week, and stops at the floor.

### 3.7 Wind-down and calendar focus

- **Wind-down:** from 30 minutes before bedtime, switch to Sleep mode with grayscale, Vault blocked, and only calls, alarm and an e-book app allowed. The first unlock after the wake time lands on the Today screen.
- **Calendar focus:** events with "focus" in the title (or from a chosen calendar) start a focus session for their length. Check on each screen unlock, plus an alarm at the start of each upcoming focus event, re-armed daily.

**Done when:** a calendar event called "Focus: report" starts a session on time, and bedtime blocks Vault apps.

### 3.8 Private Space support (optional, Android 15+)

- Show Private Space apps only while it's unlocked, and offer lock and unlock from settings. Guide the user to install the worst Vault apps there for a hard, OS-level lock.
- Check the API level at runtime; the feature is hidden below Android 15.

**Done when:** locking Private Space from the launcher hides its apps everywhere.

### 3.9 Open-source release

- Pick a licence. GPLv3 is required if any Olauncher code was reused; otherwise choose freely.
- README: what it does, how to build, why each permission is needed, honest limits, screenshots.
- Release builds strip debug screens and shortened delays. The signing key and its config stay out of the repo.
- F-Droid: no proprietary libraries (true by design), no build timestamps, store-listing text under `fastlane/metadata/android/en-US`, then open a merge request to F-Droid's `fdroiddata` repository.

**Done when:** a clean checkout builds a release APK with one Gradle command, and the repo is public.

### 3.10 Play Store (optional)

- Add a prominent in-app disclosure screen before the accessibility setting, explaining what is read and that nothing leaves the phone. Fill in the [accessibility declaration](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) in Play Console.
- If your personal developer account was created after 13 November 2023, run the [closed test with 12 testers for 14 days](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) first.
- Grayscale still needs the ADB grant on Play installs; keep the instructions in the app.

**Done when:** the app is live on a Play track and the declaration is approved.

## Device checklist

Run this on the real phone after every step. It takes about 10 minutes and catches the bugs unit tests can't. When something fails, capture `adb logcat` output and hand it to the agent with the failing item.

- [ ] Reboot: the service comes back, this is still the home app, today's numbers survive.
- [ ] Battery saver on and the phone idle for 30+ minutes: the pause screen still appears on the next Vault open.
- [ ] Open a Vault app four ways: launcher search, a notification, recents, and a link from another app.
- [ ] Split-screen and picture-in-picture (YouTube): the gate still applies and the time still counts.
- [ ] Switch quickly between two Vault apps: no double pause screens, no loops.
- [ ] Rotate the phone on the pause screen: the countdown doesn't reset.
- [ ] Turn accessibility off: home shows a clear warning that links to the Permissions screen.
- [ ] Uninstall and reinstall a Vault app: it's still in the Vault.
- [ ] Use the debug time offset to cross 04:00: counters, waits and the Today prompt reset.

On Xiaomi, Oppo, Vivo, Realme and Samsung phones, also turn on autostart for the app and set its battery use to Unrestricted. These brands stop background services more aggressively than stock Android.
