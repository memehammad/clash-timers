# Changelog

The newest section is used as the release notes on GitHub (and shown in Obtainium).

## Preview: Smartspacer

Clash Timers in At a Glance, through [Smartspacer](https://github.com/KieronQuinn/Smartspacer).
This is a preview build; your normal updates are not affected.

- **Clash builders** card: free builders, or when the next one is done, plus any running boost.
- Chips (complications): **builders** ("3 free" or "2h15"), **lab** ("free" or time left) and
  **boost** (only while a potion, snack or Clock Tower is running).
- Tapping any of them opens Clash of Clans. They update after every import and timer.

Add them in Smartspacer → Targets / Complications → + → Clash Timers.

**Personalise each one** (Smartspacer → tap it → settings):
- **Show:** builders, laboratory, pet house, Builder Base builders, Star Laboratory or the
  active boost. Add the card or a chip several times to show several.
- **When:** always, only when one is free, only while all are busy, or only when something
  finishes within the hour.
- **Text:** what finishes next and when, or how many are working.
- Card extras: mention boosts in the title, show on the lock screen or not. Pick the village
  if you track more than one. A preview shows what it says right now.

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
