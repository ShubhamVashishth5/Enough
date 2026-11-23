# Minimisation Machine — Feature Draft

Nov 23, 2026 · @Shubham

## The pitch

A home screen launcher and app blocker in one, scored on how little you use the phone, not how much. StayFree tracks and limits, but sits beside your normal home screen. Niagara declutters, but is built for speed, not less use. This app owns the home screen *and* the brakes, so it controls the moment you reach for the phone.

Design principles:

- **Own the front door.** The launcher is the first thing you see on every unlock. It decides what is one tap away and what isn't.
- **Delay before you block.** A pause with a question cuts impulse opens without the rebound that hard blocks cause (see the evidence section).
- **Make undoing it harder than doing it.** Settings changes happen when you're calm. Weak-moment you should face a cooldown, not a toggle.
- **Default to the useful thing.** Unlocking should land on today's tasks and tools, not on a feed.
- **No engagement loops in the app itself.** No badges, no endless stats to scroll, no streak shaming. Open it, glance, close it.
- **Private and offline.** Usage data never leaves the phone. No account needed.

The north-star metric is total daily unlocks and minutes in apps you've marked as distracting, both trending down week over week.

## What's already out there

Each app nails one piece; none combines a minimal launcher, escalating friction, hard commitment and a productive default in one place. That gap is the product.

| App | What it does well | Where it falls short for minimising |
| --- | --- | --- |
| [StayFree](https://stayfreeapps.com/) | Accurate usage stats across devices, app and site blocking, hiding the addictive parts of social apps (in-app blocking) | Stats-first. It sits beside your normal home screen, so every icon is still one tap away |
| [Niagara](https://www.androidpolice.com/2021/02/02/hands-on-the-minimalist-niagara-launcher-is-officially-out-of-beta/) | One-handed alphabetical list, notifications embedded in the list, hide apps, no ads | Built for speed, not less use. No limits or blocking; best bits behind Pro |
| [Olauncher](https://github.com/tanujnotes/olauncher) | Text-only home screen, hidden apps, type-to-launch, no data collection, GPLv3 open source, made in India | No friction or blocking. Feature-frozen by design. A good codebase to study or fork (fork must stay GPLv3) |
| [one sec](https://one-sec.app/research/) | A breathing pause before target apps; peer-reviewed study shows 57% fewer opens | Pause is always dismissable. Free tier covers only one app |
| [ScreenZen](https://play.google.com/store/apps/details?id=com.screenzen) | Wait timer that grows with each open, schedules, free (donation-funded) | Soft. A determined user waits it out |
| [Opal](https://apps.apple.com/IN/app/id1497465230) | Scheduled focus sessions, Deep Focus that can't be cancelled, open limits | iPhone only. Leaderboards and a Focus Score add an engagement loop of their own |
| [Brick](https://www.tomsguide.com/uk/phones/brick-review) | A $59 NFC puck: apps stay locked until you physically tap it again | Costs hardware. Only works as well as where you leave it; NFC can be flaky |

Takeaways for the build: copy Olauncher's bare home screen, ScreenZen's escalating wait, one sec's intention prompt and Brick's physical unlock. Skip Opal's leaderboards and StayFree's chart-heavy dashboard.

## What the evidence says works

Friction and design changes beat willpower, goals and dashboards. Build the app around enforced friction; treat stats as a side dish.

| Intervention | What studies found | What it means for the app |
| --- | --- | --- |
| Pause before opening an app | [one sec trial](https://pmc.ncbi.nlm.nih.gov/articles/PMC9974409) (280 people, 6 weeks): 57% fewer opens of target apps, higher satisfaction | Core mechanic. Every distracting app gets a pause screen |
| Grayscale screen | [2024 study](https://dare.uva.nl/id/aabe2259-4c55-4b25-968f-43fbcdf1ed8e) (84 people): 20 min/day less screen time, but unlocks didn't change. Other trials: 22 to 50 min/day | Grayscale cuts time in apps, not the checking habit. Pair it with unlock friction |
| Grayscale vs self-set time limits | [Randomized trial](https://pubmed.ncbi.nlm.nih.gov/36577008/) (112 people): grayscale cut screen time at once; goal-setting limits worked less and slower | Self-imposed limits are the weakest tool. Make limits enforce, not remind |
| Batched notifications | [Duke RCT](https://scholars.duke.edu/publication/1402953) (237 people in India): 3 batches a day left people more attentive, productive and less stressed. Zero notifications raised anxiety and FOMO | Deliver a digest 3 times a day instead of silencing everything |
| Bundle of 10 nudges | [McGill RCT](https://pmc.ncbi.nlm.nih.gov/articles/PMC9112639/) (grayscale, notifications off, apps moved, etc.): less problematic use, lower screen time, better sleep vs monitoring alone | Onboarding should apply a whole bundle in one go, not one tweak |
| Planning alone | [2025 RCT](https://repository.tudelft.nl/record/uuid:bb93f7c9-94c1-47cf-9822-1db593e7ac72): a planning intervention had no significant effect on total usage time | Intentions need enforcement behind them |
| Phone out of sight | [Ward 2017](https://journals.uchicago.edu/doi/10.1086/691462): a nearby phone lowered cognitive capacity, but a [2022 replication](https://pubmed.ncbi.nlm.nih.gov/36007374/) and a [2021 study](https://www.bps.org.uk/research-digest/revisiting-brain-drain-effect) didn't confirm it | Nice-to-have ("put it in another room" sessions), not a pillar |

One caution: the 112-person trial found cutting screen time didn't immediately improve wellbeing. Measure success by time reclaimed for chosen tasks, not just minutes cut.

## Features 1: the minimal launcher

The home screen shows tools and today's goal, and makes distracting apps hard to even find.

| Feature | How it works |
| --- | --- |
| Text-only home | Up to 6 essential apps as plain words (Phone, Messages, Maps, Camera, Calendar, Notes). No icons, no badges, no dock |
| Today line | Top of the home screen shows the clock, today's one goal and the next calendar item. Nothing else |
| Search, no drawer | No app grid. You type to find an app. Tool apps auto-launch on a unique match; distracting apps never do |
| The Vault | Apps you tag as distracting vanish from home and search. They open only from the Vault, behind the pause screen (Features 2). On Android 15+, optionally move them into Private Space for a true lock |
| Full-name rule | To open a Vault app you type its whole name, not 2 letters. A few seconds of typing is a speed bump |
| Unlock counter | Small text: "Unlock 23 today". Seeing the count is a nudge, and it's the habit grayscale doesn't fix |
| Notification digest | The launcher shows "4 updates waiting", not their content. Non-urgent notifications arrive in 3 batches (e.g. 9:00, 14:00, 20:00). Calls and messages from a VIP list come through live |
| Grayscale for distractions | Screen turns grayscale whenever a Vault app is open, colour everywhere else. Option for always-grayscale |
| Quick tools | Gestures for camera, torch, timer and a quick note, so the phone is a tool first |
| Plain background | Black or a single colour. No daily wallpapers or widgets that reward a glance |

## Features 2: friction, limits and blocking

Every distracting open costs a pause, a stated reason and a time box, and the cost rises through the day.

| Feature | How it works |
| --- | --- |
| Pause screen | Opening a Vault app shows a 10-second breathing screen, then "What are you here for?" with choices: reply to someone, look one thing up, bored, habit. Picking bored or habit offers an alternative first (Features 4) |
| Escalating wait | Each open today adds to the wait: 10s, 20s, 40s, 60s, capped at a few minutes. Resets at 4:00 |
| Time box | After the pause you choose 2, 5 or 10 minutes. When time is up a full-screen "Time's up" sends you home. Extending means a new pause at the next wait level |
| Shared daily budget | One budget across all Vault apps (say 45 min), not a limit per app, so you can't hop from Instagram to YouTube to dodge it |
| Taper | The budget shrinks by 5% a week until it hits a floor you set. You cut gradually instead of all at once |
| Open limits | Optional cap on opens per day per app (e.g. WhatsApp 15) |
| Feed blocking | Block the endless parts inside apps but keep the useful parts: Shorts but not YouTube search, Reels and Explore but not DMs |
| Schedules | Hard blocks during focus hours and bedtime. A "first hour" rule: no Vault apps until 60 minutes after the day's first unlock |
| Browser coverage | The same rules apply to the website versions (instagram.com, youtube.com), so switching to the browser isn't a loophole |
| Emergency pass | 3 passes a week that skip the wait, logged. Real emergencies happen; a fixed ration keeps it honest |

## Features 3: anti-cheat and commitment

Tightening a rule is instant; loosening one is slow, so a weak moment can't undo a calm decision.

| Feature | How it works |
| --- | --- |
| Loosen-later rule | Raising a budget, removing an app from the Vault or turning a rule off takes effect after 24 hours. Tightening applies at once |
| Lock period | Lock all settings for 7, 30 or 90 days. During a lock nothing can be loosened at all |
| Physical key | Pair any cheap NFC sticker or a printed QR code. Leave it in another room or at the office. Ending Deep Focus or opening the Vault early needs a tap or scan, like Brick without the hardware cost |
| Friend key | Loosening sends a code to a friend by SMS or WhatsApp. They have to read it back to you |
| Typing penance | Disabling outside a cooldown means typing a long passage exactly, with no paste |
| Uninstall guard | During a lock the app blocks its own uninstall and its App Info page, plus the accessibility and "force stop" screens that would switch it off |
| Clock-proof | Timers ignore manual changes to the phone's date and time, so moving the clock forward doesn't end a block |
| Auto-Vault new installs | Installing a new social, video or game app adds it to the Vault automatically |
| Honest limits | Show in onboarding what can't be stopped (safe mode, a factory reset). The goal is to make cheating effortful, not impossible |

## Features 4: productivity

Cutting use only helps if the time goes somewhere, so the phone steers spare moments toward what you said matters.

| Feature | How it works |
| --- | --- |
| Today's one thing | First unlock of the day asks for today's top 1 to 3 tasks. They sit on the home screen until ticked off |
| Instead list | Your own list of swaps: read 5 pages, 10 push-ups, a walk, a language lesson, call someone. When the pause screen hears "bored" or "habit", it offers one before the app |
| Earn-it mode (optional) | Vault minutes are earned by finishing a task or a focus session. Off by default, since earning can turn into its own reward loop |
| Focus sessions | 25, 50 or 90 minutes. Only an allowlist of apps opens, Do Not Disturb turns on and the screen goes grayscale. Locked sessions need the physical key to end early |
| Modes | Work, Study, Home and Sleep, each with its own allowlist and budget. Switch on a schedule, by Wi-Fi network or by location |
| Quick capture | One swipe opens a plain text box. "Look it up later" thoughts land in an inbox instead of becoming a reason to open a browser |
| Later inbox | Captured items come back at the next break or the evening digest, not mid-focus |
| Wind-down | 30 minutes before bedtime: grayscale, Vault closed, only calls, alarm and an e-book app. The morning alarm dismisses to the Today line, not a feed |
| Calendar focus | Calendar events tagged "focus" start a focus session automatically |

## Features 5: stats that don't become a new habit

Show two numbers daily and one report weekly. A dashboard you check 10 times a day is just another feed.

| Feature | How it works |
| --- | --- |
| Two numbers | Home screen shows only Vault minutes and unlocks today. No charts |
| Weekly report | Sunday evening, one screen: this week vs last, top 3 time sinks, time spent in focus sessions, tasks done |
| Trigger log | When you go over budget, one question: "What pulled you in?" (a notification, boredom, stress, an app hop). The weekly report shows the patterns, such as "most slips 22:00 to 00:00" |
| Days-under-budget count | "18 of 23 days under budget this month", not a streak. One bad day doesn't reset anything to zero |
| No social layer | No leaderboards, likes or sharing. Comparing scores is its own engagement hook |
| Stats behind a limit | The detailed stats screen opens once a day, so checking stats can't replace checking Instagram |
| Auto-tighten | Three days in a row over budget offers a slightly tighter plan, rather than a scolding |
| Local only | All data stays on the phone. CSV export for anyone who wants to dig in |

## Build notes: Android first

Build for Android. Only Android lets a third-party app replace the home screen and intercept app opens; iOS allows neither a custom launcher nor a custom pause screen of this depth. Suggested stack: Kotlin, Jetpack Compose, Room for local storage, WorkManager for the digest.

| Feature | Android mechanism | Catch |
| --- | --- | --- |
| Home screen | Register as a launcher (HOME intent, home role) | None. Olauncher's GPLv3 code is a reference |
| Usage and unlock counts | UsageStatsManager (usage access permission), screen-unlock events | The user grants usage access in Settings once |
| Pause screen on app open | AccessibilityService window events, or UsageStats polling plus an overlay | Play Store requires a [declaration form and in-app disclosure](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) for non-accessibility uses. Polling is slower but avoids the review |
| Feed blocking (Shorts, Reels) | AccessibilityService reading the screen's view structure | Breaks whenever Instagram or YouTube ship a redesign. Expect upkeep |
| Notification digest | NotificationListenerService: hold, dismiss, re-post on schedule | Some apps' notifications can't be fully recreated (reply actions) |
| Grayscale per app | Android's colour-correction setting, which needs WRITE\_SECURE\_SETTINGS | [Can't be granted in-app](https://f-droid.org/en/packages/com.lpw.graypause/): one-time ADB command or Shizuku. Fallback: monochrome launcher only |
| Hard-hiding apps | Android 15+ Private Space | Launcher needs the [hidden-profiles permission and home role](https://source.android.com/docs/security/features/private-space) |
| Website blocking | Local VpnService DNS filter, or reading the browser URL bar via accessibility | A local VPN can't run alongside a real VPN |
| Uninstall guard | Device Admin (an active admin can't be uninstalled until deactivated) plus accessibility to block that screen | Safe mode and factory reset still get around it |
| Physical key | NfcAdapter reading a tag's ID; CameraX for QR | Needs an NFC phone for the sticker option |

If you sideload your own build, Android 13+ hides accessibility and notification access for sideloaded apps until you tap "Allow restricted settings" in the app's info page.

iOS later, if ever: blocking there goes through Apple's Screen Time API, which needs a Family Controls entitlement from Apple, and [developers report](https://origin-devforums.apple.com/forums/tags/screen-time) DeviceActivity limits misfiring.

## MVP cut and roadmap

Build the launcher, the Vault and the pause screen first, since those carry the strongest evidence, and live with them before adding hard locks.

&#91;embedded content: build roadmap · 3 phases, 2 gates\]

The MVP alone tests the core bet. Feed blocking and the uninstall guard come last because they need the accessibility review and the most upkeep.

Open question: Play Store or sideload only? Sideloading skips the accessibility review but limits who else can use it.

## Sources

Studies

- [Grüning et al., one sec field experiment, PNAS 2023](https://pmc.ncbi.nlm.nih.gov/articles/PMC9974409)
- [Grayscale intervention and digital wellbeing, Mobile Media & Communication 2024](https://dare.uva.nl/id/aabe2259-4c55-4b25-968f-43fbcdf1ed8e)
- [Digital strategies for screen time reduction, randomized field experiment, 2023](https://pubmed.ncbi.nlm.nih.gov/36577008/)
- [Fitz et al., Batching smartphone notifications can improve well-being, 2019](https://scholars.duke.edu/publication/1402953)
- [Olson et al., Nudge-based intervention RCT](https://pmc.ncbi.nlm.nih.gov/articles/PMC9112639/)
- [Planning a digital detox RCT, 2025](https://repository.tudelft.nl/record/uuid:bb93f7c9-94c1-47cf-9822-1db593e7ac72)
- [Ward et al., Brain Drain, 2017](https://journals.uchicago.edu/doi/10.1086/691462), [replication, 2022](https://pubmed.ncbi.nlm.nih.gov/36007374/), [BPS summary of a 2021 non-replication](https://www.bps.org.uk/research-digest/revisiting-brain-drain-effect)

Apps

- [StayFree](https://stayfreeapps.com/)
- [Niagara Launcher hands-on, Android Police](https://www.androidpolice.com/2021/02/02/hands-on-the-minimalist-niagara-launcher-is-officially-out-of-beta/)
- [Olauncher on GitHub](https://github.com/tanujnotes/olauncher)
- [one sec research page](https://one-sec.app/research/)
- [ScreenZen on Google Play](https://play.google.com/store/apps/details?id=com.screenzen), [ScreenZen review, Lifestack](https://lifestack.ai/blog/screenzen-review)
- [Opal on the App Store](https://apps.apple.com/IN/app/id1497465230)
- [Brick review, Tom's Guide](https://www.tomsguide.com/uk/phones/brick-review)

Platform

- [Google Play AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)
- [Android Private Space for launchers](https://source.android.com/docs/security/features/private-space)
- [GrayPause on F-Droid (grayscale permission setup)](https://f-droid.org/en/packages/com.lpw.graypause/)
- [Apple developer forums, Screen Time](https://origin-devforums.apple.com/forums/tags/screen-time)
