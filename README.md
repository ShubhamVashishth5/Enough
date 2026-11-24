# Enough

A home screen and app blocker in one, scored on how little you use your phone, not how much.

Enough replaces your launcher, so it controls the moment you reach for the phone. Distracting apps go in
the **Vault**: they leave the home screen and search, and every open starts with a breathing pause that
grows through the day. One daily budget covers all of them. Making a rule stricter is instant; making it
looser waits 24 hours, so a weak moment can't undo a calm decision.

It's Android only, works fully offline and has no internet permission. Nothing you do leaves the phone.

## What it does

- **A quiet home screen.** Up to six tool apps as plain words, today's one to three tasks, the next
  calendar item, and two numbers: Vault minutes and unlocks today. No icons, badges or app grid; you type
  to find an app.
- **The Vault.** A pause screen ("What are you here for?"), an escalating wait, a 2, 5 or 10 minute time
  box, and a shared daily budget. Answer *bored* or *habit* and it offers something from your Instead list
  first.
- **Websites too.** A website in the Vault (instagram.com, youtube.com) gets the same pause, budget and
  rules in Chrome, Firefox, Samsung Internet, Brave, Edge and others.
- **Feed blocking.** Block YouTube Shorts, Instagram Reels and Explore while search and messages keep
  working.
- **Limits and schedules.** Open limits per app, bedtime and focus-hour schedules, a first-hour rule,
  modes (Work, Study, Home, Sleep), wind-down at bedtime, focus sessions (including ones started by
  calendar events with "focus" in the title), and a notification digest three times a day.
- **Commitment.** Lock every setting for 7, 30 or 90 days. Skip a 24-hour wait only with a code a friend
  keeps, by typing out a 300-character passage, or with an NFC sticker or printed QR code you leave in
  another room. An uninstall guard and a clock that ignores the phone's date being changed.
- **Budgets that move.** Taper the budget by 5% a week down to a floor, get offered a smaller budget after
  three days over, or (off by default) earn minutes by finishing tasks and focus sessions.
- **Stats that don't become a habit.** One weekly report (this week against last, top time sinks, when
  you slip and why), which opens once a day, and a CSV export of the whole event log.
- **The system app lock.** On Android 17 QPR2 and later, while protection is below Full, home offers to
  put Vault apps behind the phone's own fingerprint lock, and to take it off again once it's back. Android
  asks you to confirm each one. Enough only ever removes locks it added.
- **Backup and restore.** Every setting in one JSON file (the event log stays in the CSV export). Restoring
  it on a new phone follows the same rule as everything else: stricter settings apply at once, looser ones
  wait 24 hours, and nothing loosens during a lock period. A file from a newer version is refused.
- **Private space.** On Android 15+, lock and unlock Private space from the launcher; its apps are hidden
  everywhere while it's locked.

The product spec is in [Minimisation Machine — Feature Draft.md](Minimisation%20Machine%20—%20Feature%20Draft.md)
and the build plan in [Minimisation Machine — Implementation Plan.md](Minimisation%20Machine%20—%20Implementation%20Plan.md).
What comes next is in [Minimisation Machine — Beyond Plan.md](Minimisation%20Machine%20—%20Beyond%20Plan.md) and
[Minimisation Machine — Beyond Implementation Plan.md](Minimisation%20Machine%20—%20Beyond%20Implementation%20Plan.md).

## Install

Download the APK from the [Releases](../../releases) page and open it on the phone. Then:

1. Make Enough your default home app when it asks.
2. Turn on its accessibility service. **Installed outside the Play Store on Android 13+?** The setting is
   greyed out until you open Enough's App info, tap ⋮ and choose *Allow restricted settings*. The same goes
   for notification access.
3. Allow unrestricted battery use. On Xiaomi, Oppo, Vivo, Realme and Samsung phones also turn on autostart
   for the app; these brands stop background services more aggressively than stock Android.

Grayscale needs a one-time grant from a computer with USB debugging on:

```sh
adb shell pm grant app.minimise android.permission.WRITE_SECURE_SETTINGS
```

## Permissions and why

Every permission is optional except the default home app, and each feature switches itself off cleanly
when its permission is missing. The Permissions screen in the app shows the status of each, and which
protection level that adds up to: **Full** (accessibility), **Lite** (usage access plus display over other
apps) or **home screen only**.

| Permission | Why |
| --- | --- |
| Accessibility service | Sees which app comes to the front, so a Vault app opened from a notification, recents or a link still gets the pause screen. Reads screen content only where a rule you switched on needs it: inside an app with a blocked feed, a browser's address bar while a website is in the Vault, and this app's own Settings pages during a lock period. Nothing is stored or sent anywhere. |
| Usage access and display over other apps | **Lite protection**, for when accessibility is off or Android takes it away (Android 17's Advanced Protection does). Enough checks once a second which app is in front and shows the pause over a Vault app, from a foreground service with one quiet notification. It reads no screen content, so feed blocking, website rules and the lock guard are off at this level. |
| Default home app | Every unlock lands on Enough, so Vault apps aren't one tap away. Also lets it list and lock Private space (`ACCESS_HIDDEN_PROFILES`), and on Android 17 QPR2 put apps behind the system app lock (`LOCK_APPS`). |
| Unrestricted battery | Stops the system pausing the service, which would switch protection off. |
| Notification access | The digest: holds non-urgent notifications and releases them at 9:00, 14:00 and 20:00. |
| Post notifications | The digest summary, the weekly report, and "an app went into the Vault". |
| Do Not Disturb access | Turned on during focus sessions and put back afterwards. |
| Calendar | The next event on the home screen, and calendar focus. |
| NFC and camera | Pairing and checking a physical key (an NFC sticker or a printed QR code). |
| Device admin | The uninstall guard. It asks for no device policies; being an admin is what blocks uninstalling. |
| Write secure settings | Grayscale. Only ADB can grant it. |
| Boot completed, set alarm | Re-arming the digest, weekly report and calendar focus alarms after a restart; opening the timer from home. |

There is no `INTERNET` permission, now or later. The app uses no Google Play Services, Firebase or
analytics.

## Honest limits

Enough makes cheating take effort. It can't make it impossible:

- Safe mode starts the phone without any downloaded app, this one included.
- A factory reset wipes everything.
- A computer with USB debugging can switch off or uninstall any app.
- Changing the phone's clock is ignored while the phone stays on, but not across a restart.
- Feed blocking, website rules and the uninstall guard read other apps' screens. When those apps change
  their design, a rule can stop working until it's updated (see below).
- Some phones stop background apps to save battery despite the settings above.

## Build

You need JDK 17 or newer and the Android SDK with platform `android-37.2` and build-tools `37.0.0`. Point
Gradle at the SDK with `ANDROID_HOME` or a `local.properties` file (`sdk.dir=/path/to/sdk`).

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # app/build/outputs/apk/release/, minified, debug screens stripped
```

Without signing details, `assembleRelease` produces `app-release-unsigned.apk`. To sign it, create a
keystore once and keep it (and its passwords) out of the repo; every update must be signed with the same
key:

```sh
keytool -genkeypair -keystore release.jks -alias enough -keyalg RSA -keysize 4096 -validity 10000
```

Then either put a `keystore.properties` file next to `settings.gradle.kts` (it's git-ignored):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=enough
keyPassword=...
```

or set `ENOUGH_KEYSTORE_FILE`, `ENOUGH_KEYSTORE_PASSWORD`, `ENOUGH_KEY_ALIAS` and `ENOUGH_KEY_PASSWORD`.

Debug builds have a few extra Settings rows: shorten the 24-hour loosen delay to a minute, shift the
clock to test day resets, see today's raw usage, and dump the screen's node tree to logcat. None of them
exist in release builds.

## Releases with GitHub Actions

- **CI** (`.github/workflows/ci.yml`) runs the unit tests and builds a debug APK on every push and pull
  request. The APK is attached to the run as an artifact.
- **Release** (`.github/workflows/release.yml`) builds a signed release APK. Pushing a tag such as
  `v0.1.0` runs the tests, signs the APK and publishes it with a SHA-256 checksum as a GitHub Release.
  Running it by hand from the Actions tab uploads the APK as an artifact only.

The release workflow needs four repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | The keystore, base64-encoded: `base64 -w0 release.jks` (macOS: `base64 -i release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password |
| `RELEASE_KEY_ALIAS` | The key alias, e.g. `enough` |
| `RELEASE_KEY_PASSWORD` | The key password |

Tag builds fail without them, rather than publish an APK that can't be installed. To cut a release, bump
`versionCode` and `versionName` in `app/build.gradle.kts`, add
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, commit, then:

```sh
git tag v0.1.0 && git push origin v0.1.0
```

## Translations

Translations are welcome. Copy `app/src/main/res/values/strings.xml` to
`app/src/main/res/values-xx/strings.xml`, where `xx` is the language code (`de`, `pt-rBR`), translate the
text and open a pull request. Counts are `<plurals>`: give each one every quantity your language uses
(`one`, `two`, `few`, `many`, `other`). Strings marked `translatable="false"` stay as they are, and any
string you leave out falls back to English. The typing penance word list (`typing_words`) is worth
translating too, so the passage is in the user's language.

## Accessibility

The launcher replaces the home screen for everyone who installs it, TalkBack users included. Every home
row, the pause countdown, the time box choices and the Blocked screen's overrides have labels and roles.
TalkBack takes over swipes and double taps, so the home gestures (search, settings, capture, camera,
lock) are also custom actions on the clock. The breathing circle holds still when *Remove animations* is
on.

## Keeping the screen rules up to date

Rules that read other apps' screens are data, not code, so a redesign of YouTube or Chrome is a one-line
fix:

| File | What it matches |
| --- | --- |
| `app/src/main/assets/feed_rules.json` | Blocked feeds (Shorts, Reels, Explore). A feed is on screen when any matcher matches. |
| `app/src/main/assets/browser_rules.json` | Each browser's address bar. |
| `app/src/main/assets/guard_rules.json` | Settings pages blocked during a lock. A page matches only when every matcher does. |

A matcher can check `viewId`, `text`, `textContains`, `contentDescription` and `descriptionContains`; every
field you set must match. To find new view IDs, use a debug build: Settings → *Debug: dump screen in 5 s*,
switch to the app, then read logcat with `adb logcat -s ScreenDump`.

## Project layout

One Gradle module, organised by feature under `app/src/main/java/app/minimise/`. Every feature that delays
or blocks an app is a `Rule` in `gate/rules/`; every way past a block (wait, keys, friend code, typing,
emergency pass) is an `UnlockMethod`; every setting change goes through `SettingsGuard`, which applies
tightening at once and makes loosening wait. All stats are computed from one event log. Domain packages are
plain Kotlin and carry the unit tests.

## F-Droid

The app uses no proprietary libraries and embeds no build timestamps or dependency metadata. Store-listing
text lives in `fastlane/metadata/android/en-US`. Submitting it means opening a merge request against
F-Droid's [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository.

## Play Store

Before the accessibility setting opens, the app shows a prominent disclosure (what is read, why, and that
nothing leaves the phone) and continues only on *Agree*, as Google Play's
[accessibility policy](https://support.google.com/googleplay/android-developer/answer/10964491) requires.
Publishing there also needs the accessibility declaration filled in on Play Console, and, for personal
developer accounts created after 13 November 2023, a closed test with 12 testers for 14 days first.
Grayscale still needs the ADB grant on Play installs.

## Licence

[Apache License 2.0](LICENSE).
