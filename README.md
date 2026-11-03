# Clash Timers

A Material You home-screen widget for Clash of Clans that shows your running
upgrades, research and cooldowns, and tells you the moment a builder is free.

No account, no login, no server: it reads the village export the game itself
provides and works fully offline.

## Features

- **Free builders at a glance.** How many builders are idle, or a live
  countdown to the next one.
- **Everything that's running.** Building and hero upgrades, Gear Ups,
  laboratory research, pets, Builder Base, helper and Clock Tower cooldowns.
- **Status chips** for Laboratory, Pet House, Builder Base and Star Laboratory.
- **Boost-aware timers.** Builder, Research and Pet Potions, Builder Bite,
  Study Soup, Clock Tower and the Lab Assistant / Builder's Apprentice are
  factored into the finish times. The widget switches to a boost theme while
  one is active.
- **Notifications** when an upgrade, research or cooldown finishes.
- **Material 3 Expressive design** that follows your wallpaper colours, in
  light and dark mode. Resizable widget with a compact layout for small sizes.

## Requirements

Android 12 or newer.

## Install

Download `ClashTimers.apk` from the latest
[release](../../releases/latest) and open it on your phone. You may need to
allow installing apps from your browser or file manager.

## Usage

1. In Clash of Clans, open **Settings → More Settings → Data Export** and tap
   **Copy**.
2. Open Clash Timers, or tap the widget. A new export on the clipboard is
   imported automatically. You can also share the text to the app.
3. Add the widget: long-press the home screen → **Widgets** → **Clash Timers**.

The export is a snapshot. Copy a new one after you start new upgrades or use
a potion.

## How it works

The export lists each running upgrade with its remaining time at normal speed.
Clash Timers converts that into real finish times, applying any active boosts,
and schedules a notification for each. Names and upgrade durations come from a
bundled data file, so nothing is fetched from the internet.

The builder count is detected from your Builder's Huts and B.O.B. If it's ever
wrong, tap the builder card in the app to set it manually.

### Limitations

- Android only lets apps read the clipboard while they're open, so the app has
  to be opened (or the widget tapped) after copying an export.
- Gold Pass time reductions and boosts used after the export aren't known
  until you export again.
- The export key names for Builder Bite, Study Soup and an active Clock Tower
  boost aren't documented and are matched by name. If one isn't picked up,
  please open an issue with an export taken while it's active.

## Building from source

Releases are built automatically by GitHub Actions on every push to `main`.

To build locally you need JDK 17 or 21 (Gradle 8.11 doesn't run on Java 25)
and the Android SDK, via Android Studio or the command-line tools with
`ANDROID_HOME` set.

```
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

If your default Java is newer, point Gradle at JDK 21, for example
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleRelease`.

### Signing

Without a key the release build is signed with the debug key. To sign with
your own key, place it at `app/release.jks` (alias and passwords
`clashtimers`, or change them in `app/build.gradle.kts`). In CI, store it
base64-encoded as the `SIGNING_KEY` repository secret. Never commit the key.

### Updating game data

`app/src/main/assets/coc_data.json` is generated from the
[clash-of-clans-data](https://github.com/chiefpansancolt/clash-of-clans-data)
package:

```
npm pack clash-of-clans-data && tar xzf clash-of-clans-data-*.tgz
python3 tools/build_table.py package/data app/src/main/assets/coc_data.json
```

## Credits

- Game data: [clash-of-clans-data](https://github.com/chiefpansancolt/clash-of-clans-data)
  by Christopher Pezza (MIT).

## Disclaimer

This material is unofficial and is not endorsed by Supercell. For more
information see Supercell's
[Fan Content Policy](https://supercell.com/en/fan-content-policy/).
