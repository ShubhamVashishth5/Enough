# Minimisation Machine — Beyond Plan

Nov 24, 2026 · @Shubham

## Where Enough stands

Phases 1 to 3 shipped nearly all of the [feature draft](Minimisation%20Machine%20—%20Feature%20Draft.md): launcher, Vault, pause, budget, commitment, focus, digest, feed and browser blocking, stats and the release pipeline. This plan covers what comes next, in three kinds of work:

- **Draft leftovers.** A few items from the draft were never built: the torch gesture, VIP contacts that skip the digest, and switching modes by Wi-Fi or location.
- **New features.** Ideas from other apps and from recent studies that fit the product's principles.
- **Keeping it working.** Android and Google Play keep changing the rules for accessibility-based blockers. Some of this plan is defence, not features.

The north star doesn't change: unlocks and Vault minutes trend down week over week, and the time goes to tasks the user chose.

## How the ideas were chosen

Every idea here passed five filters. Ideas that failed one are listed under "Left out" with the filter they failed.

1. **Offline and private.** No `INTERNET` permission, no account, no Google Play Services, nothing leaves the phone.
2. **Fits the architecture.** It is a new `Rule`, `UnlockMethod`, `SettingChange`, event type or screen, or it replaces a fragile part with a sturdier one.
3. **No engagement loop.** No streaks, scores, badges, leaderboards or a dashboard worth checking.
4. **A reason behind it.** A study, a loophole users actually use, or a platform change. "Other apps have it" alone isn't enough.
5. **Survives the platform.** It works under Google Play review and Android 17's tighter accessibility rules, or switches itself off cleanly.

Sources: the feature draft's unbuilt items; current versions of one sec, ScreenZen, Opal, Brick, Clearspace, Jomo and MonoPhone; Android 17 and Google's own Digital Wellbeing; and field studies from 2023 to 2025. Each feature is marked **Must** (protects what already works), **Should** (strong reason, modest cost) or **Could** (worth it if time allows).

## What the research found

Two findings shape this plan. Friction works, but use rebounds quickly whenever the friction stops. And Android is closing the accessibility door that Enough's gate depends on, so the gate needs a second way in.

### Studies

| Study | What it found | What it means for Enough |
| --- | --- | --- |
| [Haliburton et al., CHI 2024](https://www.medien.ifi.lmu.de/pubdb/publications/pub/haliburton2024chi/haliburton2024chi.pdf): logs from 1,039 one sec users over 13.4 weeks on average | Short frictions cut attempts to open target apps, and opens became more intentional over time. Users took breaks from the friction and quickly rebounded into overuse when they came back | Gaps in protection are where use comes back. Keep a gate running without accessibility, and treat the return from a gap as a fresh start, not a free pass |
| [Terzimehić et al., MindPhone, DIS 2022](https://www.medien.ifi.lmu.de/pubdb/publications/pub/terzimehic2022dis/terzimehic2022dis.pdf): 28 people, 2-week field study | Asking at unlock "What activity do you want to do after you finish using your smartphone?" significantly reduced use. "Why do you want to use your phone right now?" raised awareness but didn't. Use rose again after the prompt stopped | Add the real-world question as a pause variant and as an optional unlock prompt. Keep it running; don't make it a one-off |
| [Lu et al., InteractOut, CHI 2024](https://arxiv.org/html/2401.16668v2): 42 people, 5 weeks | Delaying and shifting taps and swipes in target apps cut opens by 16.5% and time by 15.6%, more than a timed lockout, and was accepted more often (61.6% against 36.3%) | Strong result, but it needs an overlay that replays every touch through accessibility. Left out for now (filter 5) |
| [Castelo et al., PNAS Nexus 2025](https://www.news-medical.net/news/20250219/Blocking-mobile-internet-for-two-weeks-improves-mental-health-and-well-being.aspx): 467 people, 4-week randomised trial | Two weeks of a phone limited to calls and texts improved mental health, wellbeing and sustained attention; 91% improved on at least one. Only 57% installed the blocker and about a quarter stayed offline the full two weeks | A structured two-week reset is worth offering, and Enough's lock fixes the drop-out the study ran into |

### Android and other apps

| Source | What's new | What it means for Enough |
| --- | --- | --- |
| [Android 17 Advanced Protection](https://thehackernews.com/2026/03/android-17-blocks-non-accessibility.html) (opt-in, seen in Beta 2) | Apps that aren't accessibility tools can't be granted accessibility, and lose it if they had it. Launchers and monitoring apps are named as not qualifying | **Must.** Users who turn it on lose the gate outside the launcher. Lite protection (usage access) keeps it working |
| [Sensitive views, Android 16](https://android-developers.googleblog.com/2025/12/enhancing-android-security-stop-malware.html) | Apps can mark views as sensitive; services without `isAccessibilityTool` can't read them | Feed and address-bar rules may go blind in apps that adopt it. Rules should fail safe (treat "can't read" as "can't tell", not "allowed") |
| [Android 17 App Lock](https://nokiapoweruser.com/google-android-17-qpr3-beta-native-app-lock-guide/), [QPR3, Q1 2027](https://www.droid-life.com/2026/11/18/app-lock-is-back-on-android-17-heres-how-i-got-it/) | System lock behind fingerprint or PIN, with APIs for third-party launchers | An OS-level speed bump for Vault apps when the gate can't see them |
| [Pause Point](https://www.digitaltrends.com/phones/androids-new-pause-point-feature-puts-a-10-second-speed-bump-between-you-and-mindless-scrolling/), Google Digital Wellbeing | A 10-second check-in when opening chosen apps, with breathing, a timer, photo memories or a suggested alternative | A plain pause is now built into Android. Enough's edge is the launcher, the shared budget and commitment, so build on those |
| [one sec](https://apps.apple.com/app/id1532875441) | Many pause types beyond breathing: journaling, a sum, follow the dot, typing random text, conversational reflection. An [intention picker](https://tutorials.one-sec.app/intentions-tracking-editing) after the pause, and an optional reminder later asking whether you're still doing what you planned | Varied pauses and a check-back on the reason. Conversational reflection needs a language model, so not that one |
| [ScreenZen Halo](https://screenzen.co/halo) and [Brick Night](https://www.channelnews.com.au/brick-night-automatically-blocks-distracting-apps-in-the-bedroom/) | Bluetooth devices that block chosen apps while the phone is in the bedroom, and lift the block when it leaves. Halo is $49 with a range of up to 50 ft | Room-based blocking works without new hardware: tie a mode to a Bluetooth device already in the room |
| [Clearspace](https://www.ycombinator.com/launches/KxS-clearspace-earn-your-screen-time-with-exercise) and [Jomo](https://apps.apple.com/CA/app/id1609960918) | "Every pushup you do earns a minute of scrolling." Jomo's Routines unlock apps when you complete habits, such as "Go for a 4 km run → unlock Netflix for the evening" | Movement as a way past a block fits as an `UnlockMethod`. Earning minutes per rep or chore is a reward loop, so not that part |

## New features

Nineteen features in six groups. One is a Must: a gate that works without accessibility. The rest are independent, so build them in priority order and drop any that don't earn their place. The Step column points to the [Beyond Implementation Plan](Minimisation%20Machine%20—%20Beyond%20Implementation%20Plan.md).

### Keep the gate working

| Feature | What it does | Why | Priority | Step |
| --- | --- | --- | --- | --- |
| Lite protection | When accessibility is off or taken away, the gate falls back to usage access: it checks the app in front once a second and shows the pause over it. Feed, website and settings guards need accessibility, so they switch off and say so | Use rebounds when friction stops, and Android 17's Advanced Protection removes accessibility from apps like this one | Must | 4.2 |
| OS app lock | Where Android 17's app lock exists, Vault apps get the system fingerprint lock whenever the gate can't see them | A speed bump that also hides their notification content while protection is down | Should | 4.3 |
| Gap notice | Home and the weekly report show when protection was off and for how long. The first Vault open after a gap starts at the longest wait | People who take a break from friction rebound fast. A gap that shows is harder to drift into | Should | 5.6 |
| Backup and restore | Export every setting to one file and import it on a new phone. Import obeys the loosen-later rule | A new phone means setting everything up again today | Should | 4.4 |

### Finish the draft

| Feature | What it does | Why | Priority | Step |
| --- | --- | --- | --- | --- |
| VIP contacts | Messages from chosen people skip the digest and arrive at once, like calls | In the draft. Without it, people exempt a whole messaging app, which lets everyone through | Should | 5.1 |
| Torch gesture | A gesture on home turns the torch on and off | In the draft's quick tools | Should | 5.2 |
| Zones | A mode switches on while the phone is connected to a chosen Bluetooth device (car, bedroom speaker) or, with location permission, a Wi-Fi network | In the draft. Halo and Brick Night sell the bedroom version as hardware | Should | 5.3 |

### A pause that keeps working

| Feature | What it does | Why | Priority | Step |
| --- | --- | --- | --- | --- |
| Varied pauses | Besides breathing, the pause can ask "What will you do after this?", take a one-line note or set a small sum. It rotates through the ones the user picks | The real-world question cut use in MindPhone; one sec offers many pause types | Should | 5.4 |
| Check-back | When a time box ends, one tap: "Did you get what you came for?" The weekly report shows how often the answer was yes | Closes the loop on the reason picked at the pause | Should | 5.5 |
| Unlock question | Optional: on unlock, ask "What will you do after this?", at most once an hour | MindPhone asked at unlock; use rose again once it stopped | Could | 6.2 |

### Insight without a habit

| Feature | What it does | Why | Priority | Step |
| --- | --- | --- | --- | --- |
| Since you started | The weekly report compares this week with the first full week of use: "Vault time down 41% since you started" | The north star is a trend. One number against the start shows it without a chart | Should | 5.7 |
| One suggestion | The weekly report offers one tightening drawn from the data, such as moving wind-down earlier when most slips are late at night. One tap applies it | Auto-tighten already does this for the budget; this extends it to schedules and modes | Should | 5.7 |

### Bigger commitments

| Feature | What it does | Why | Priority | Step |
| --- | --- | --- | --- | --- |
| Two-week reset | A 14-day programme: Vault and browsers closed, digest and grayscale on, only Tool apps such as calls, texts and maps. Starting it starts a 14-day lock. The report compares before and after | Two weeks without mobile internet improved wellbeing and attention in a 467-person trial, but most people dropped out | Should | 6.1 |
| Move to unlock | Walking a set number of steps ends a block early, as an `UnlockMethod` | Clearspace and Jomo tie unlocks to movement. As a way past a block, not a way to earn minutes | Could | 6.3 |
| Phone down | Turn the phone face down to start a "phone down" stretch. Picking it up shows how long it lasted, logged as time reclaimed | The draft wanted "put it in another room" sessions, and success is meant to be time reclaimed | Could | 6.4 |
| Network brake | Vault apps get no internet outside a time box, through a local VPN that drops their traffic | Stops feeds refreshing and pinging in the background. Can't run alongside a real VPN | Could | 6.6 |

### Small tools and reach

| Feature | What it does | Why | Priority | Step |
| --- | --- | --- | --- | --- |
| Translations and TalkBack | Plural forms, translations from contributors, and a launcher that works with a screen reader | A launcher replaces the home screen for everyone who installs it | Should | 4.5 |
| Quick Settings tiles | Tiles for quick capture and for starting a focus session | Capture and focus from anywhere, without opening an app | Could | 6.5 |
| Grayscale through Shizuku | Grant the grayscale permission on the phone through Shizuku, with no computer | The draft named Shizuku as the other way to grant it | Could | 6.7 |

## Left out

These came up in the research and failed a filter. They can come back if the filter stops applying.

| Idea | Seen in | Filter | Why not now |
| --- | --- | --- | --- |
| Input friction (delayed taps and swipes) | InteractOut study | 5 | Needs an overlay that catches every touch and replays it through accessibility. Fragile under Advanced Protection and Play's accessibility rules. Revisit once Lite protection is proven |
| Conversational reflection with a language model | one sec | 1 | Needs a server, or Google's on-device model, which comes through Play Services |
| Earning minutes per pushup or chore | Clearspace, Jomo | 3 | Makes Vault minutes a reward. Earn-it already exists and stays off by default |
| Scores, streaks and leaderboards | Common in the category | 3 | The draft rules out any social or score layer |
| Our own Bluetooth puck | Halo, Brick Night | 2 | Hardware to make and ship. Zones reach the same result with a device the user already owns |
| Modes by GPS location | The draft | 5 | Needs background location, which is heavy on battery and hard to justify in Play review. Wi-Fi and Bluetooth zones cover home, office and bedroom |
| Home screen widgets | Other launchers | 3 | The draft rules out anything that rewards a glance |
| Sync or a web dashboard | StayFree | 1 | Needs the network and an account |
| iPhone version | The draft | 5 | iOS allows no custom launcher, and blocking needs Apple's Family Controls entitlement |
| Parental controls | Other blockers | 2 | A different product: rules set for someone else, not by a person for themselves |

## Open questions

- [ ] **Play Store, or F-Droid and GitHub first?** Advanced Protection and Play's accessibility review both push towards Lite protection as the default on Play, with accessibility as an opt-in upgrade.
- [ ] **Application ID and name.** Keep `app.minimise` and "Enough"? The ID can't change after the first public release without everyone reinstalling.
- [ ] **Two-week reset overrides.** Allow emergency passes during the reset, or only a friend code?
- [ ] **Check-back frequency.** After every time box, or one in three, so it doesn't nag?
- [ ] **Network brake and real VPNs.** Offer it only when no other VPN is set up, or let the user choose which one wins?

## Sources

Studies

- [Haliburton et al., A Longitudinal In-the-Wild Investigation of Design Frictions to Prevent Smartphone Overuse, CHI 2024](https://www.medien.ifi.lmu.de/pubdb/publications/pub/haliburton2024chi/haliburton2024chi.pdf)
- [Terzimehić et al., MindPhone: Mindful Reflection at Unlock Can Reduce Absentminded Smartphone Use, DIS 2022](https://www.medien.ifi.lmu.de/pubdb/publications/pub/terzimehic2022dis/terzimehic2022dis.pdf)
- [Lu et al., InteractOut, CHI 2024](https://arxiv.org/html/2401.16668v2)
- [Castelo et al., Blocking mobile internet on smartphones, PNAS Nexus 2025 (News-Medical summary)](https://www.news-medical.net/news/20250219/Blocking-mobile-internet-for-two-weeks-improves-mental-health-and-well-being.aspx)

Android

- [Android 17 blocks non-accessibility apps under Advanced Protection, The Hacker News](https://thehackernews.com/2026/03/android-17-blocks-non-accessibility.html)
- [Enhancing Android security: stop malware from snooping on your app data, Android Developers Blog](https://android-developers.googleblog.com/2025/12/enhancing-android-security-stop-malware.html)
- [Android 17 QPR2 app lock, 9to5Google](https://9to5google.com/2026/08/14/android-17-qpr2-app-lock/)
- [App lock is back on Android 17, Droid Life](https://www.droid-life.com/2026/11/18/app-lock-is-back-on-android-17-heres-how-i-got-it/)
- [Android 17 QPR3 native app lock guide, NPowerUser](https://nokiapoweruser.com/google-android-17-qpr3-beta-native-app-lock-guide/)
- [Pause Point, Digital Trends](https://www.digitaltrends.com/phones/androids-new-pause-point-feature-puts-a-10-second-speed-bump-between-you-and-mindless-scrolling/)

Apps

- [one sec on the App Store](https://apps.apple.com/app/id1532875441) and [its intention tracking guide](https://tutorials.one-sec.app/intentions-tracking-editing)
- [ScreenZen Halo](https://screenzen.co/halo)
- [Brick Night, ChannelNews](https://www.channelnews.com.au/brick-night-automatically-blocks-distracting-apps-in-the-bedroom/)
- [Clearspace launch, Y Combinator](https://www.ycombinator.com/launches/KxS-clearspace-earn-your-screen-time-with-exercise)
- [Jomo on the App Store](https://apps.apple.com/CA/app/id1609960918)
