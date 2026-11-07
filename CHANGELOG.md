# Changelog

The newest section is used as the release notes on GitHub (and shown in Obtainium). Its heading is the
version number: a new heading makes a new release.

## 1.2.1

- **Reliable alerts:** the quick setup now offers to let Clash Timers run without battery
  restrictions. Recommended on Samsung, Xiaomi, OnePlus and similar phones, whose battery
  savers can block alerts. On Pixels and most other phones it's not needed.
- **Clearer auto-import:** the app now says plainly that auto-import only reads what you copy
  while Clash of Clans is open, and ignores everything else.

## 1.2

**Import straight from the widget.** The widget now has a small paste button. Copy your
village in Clash, go to your home screen and tap it: the widget updates without opening the
app. Don't need it? Turn off "Import button" in the widget's settings.

## 1.1

**Clash Timers speaks your language.** The app is now in German, Spanish, French and
Portuguese, and follows your phone's language. Buildings, troops and heroes use the same
names as in the game. A few of the newest ones stay in English for now. On Android 13 and
newer you can also pick the app's language in Android settings.

## 1.0.42

**A friendlier first start.** New installs open with a quick setup: turn on notifications,
pick an idle-builder reminder and set up auto-import if you like. After that, a short
four-step tour shows how the app works. You can watch the tour again any time under
"How it works" at the bottom of the app.

## 1.0.37

**Goblin Builder alerts.** When the Goblin Builder (or Goblin Researcher) finishes a job, you now
get a "Goblin Builder is free" notification, so you can give him the next one. The game only
shares his work while he's busy, so export once after you give him a job.

## 1.0.35

**A brand new icon.** A golden timer ring with a hammer in the middle, made in the new Material
style. If you use themed icons, there's a matching monochrome version too.

## 1.0.32

**Your builders, right on your home screen's At a Glance**

Clash Timers now works with [Smartspacer](https://github.com/KieronQuinn/Smartspacer), the app
that adds things to the At a Glance bar above your clock. Install Smartspacer, then add
Clash Timers under Targets (the bigger card) or Complications (the small chips next to the date).

- See at a glance how many builders are free, or when the next one finishes.
- The card matches the rest of At a Glance. On Android 16 you can switch it to a colourful
  look with a chip for builders, lab and so on, and a dot for each builder.
- Small chips for builders, the lab and running potions fit right next to the date.
- Tap any of them to jump straight into Clash of Clans.

**Make it yours.** Tap the card or chip in Smartspacer to choose:
- what to track: builders, lab, pet house, Builder Base, Star Lab, active potions, or any mix;
- when it shows up: always, only when a builder is free, while everyone's busy, or shortly
  before something finishes (you pick how long before, from 15 minutes to 6 hours);
- whether it says what finishes next or how many builders are working;
- which village it follows, and whether it shows on your lock screen.

You can add the card or chips several times with different settings.

## 1.0.23

**Layout**
- Status chips and hero cards use a balanced two-column grid: with an odd number, the last
  card spans the full width, so there are no empty gaps however many heroes or buildings you
  have unlocked.
- Laboratory and Star Laboratory now sit side by side; Pet House and Builder Base follow.

**Also new since 1.0.20**
- Re-export hint only appears once something has finished since your last export, after a
  delay you choose (6 h, 12 h, 24 h, 48 h or never). An export with everything still running
  never counts as out of date.
- Link to the GitHub page and version info at the bottom of the app.
- Optional update check: a banner tells you when a new version is out (asks GitHub at most every
  12 hours; can be turned off in settings).

## 1.0.20

- Progress bars setting: wavy, straight or off (app and widgets).
- Optional finish times ("Tomorrow 12:30") instead of countdowns.
- Better contrast on the "builder free" card.
- Package name is now `dev.memehammad.clashtimers` (one-time reinstall from 1.0.16 or older).
- Tapping a notification opens Clash of Clans; timer notifications have an "Open timers" button.
- Fixed notifications sometimes arriving twice after an update.

## 1.0.14

- Per-widget settings (long-press a widget): choose the village and what it shows: status
  chips, heroes, boost banner, re-export hint, progress bars and list sections.
- Live status: shows the clock time the next builder is free; choose what the Android 16 status
  bar chip shows (countdown, finish time or free builders).
- Tidier hero cards.
- Widget no longer keeps showing old state after an update (e.g. Lab still "idle" after starting
  research).
- App refreshes by itself when an export arrives in the background.

## 1.0.10

- Several villages: every export is saved by player tag; switch, rename or remove them in the
  app, and pick a village per widget.
- Hero availability: which heroes can attack and when the upgrading ones are back.
- Boost planner: see what a Builder/Research/Pet Potion, Builder Bite or Study Soup would finish
  before using it.
- Re-export hint when your data is likely out of date.
- Optional idle-builder reminder (15 min to 2 h).
- Optional live status notification counting down to the next free builder (Live Update on
  Android 16).

## 1.0.8

- Optional auto-import with Shizuku: tap Copy in Clash and the widget updates without opening
  the app.
- New app icon with themed icon support.
- Widget sizes from 1x1 to the full list, with sizes tuned for modern launchers; 1x1 and 2x2
  show just the badge without a background.
- App-style squiggly progress in the widget.

## 1.0.5

- First public build: Material You widget and app for running upgrades, research, heroes,
  Builder Base and cooldowns, with free-builder status, potion-aware timers, boost theme and
  notifications when things finish.
