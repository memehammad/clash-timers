# Clash Timers

Android widget (Material You) showing running Clash of Clans upgrades, research,
hero upgrades, Builder Base, helper and boost cooldowns, with a notification when
each one finishes. Android 12 or newer.

## Use

1. In Clash of Clans: Settings > More Settings > Data Export > Copy.
2. Open Clash Timers, or tap the widget. A new village export in the clipboard is
   imported automatically. Sharing the text to the app also works.
3. Long-press the home screen > Widgets > Clash Timers.

Timers come from the export snapshot, so re-export after starting new upgrades.

## Build from the phone (GitHub Actions)

1. Create an empty **private** repository on github.com (e.g. `clash-timers`).
2. Create a personal access token: GitHub > Settings > Developer settings >
   Fine-grained tokens, access to that repo, permission "Contents: Read and write"
   and "Workflows: Read and write".
3. In Termux:

```
pkg install git unzip
termux-setup-storage
cd ~ && unzip ~/storage/downloads/ClashTimers.zip && cd ClashTimers
git init -b main
git add .
git -c user.name=me -c user.email=me@example.com commit -m "Clash Timers"
git remote add origin https://github.com/YOUR_NAME/clash-timers.git
git push -u origin main      # username + token as password
```

4. Wait about 4 minutes (Actions tab). Open the repo's **Releases**, download
   `ClashTimers.apk` and install it. Every later push builds a new version that
   installs as an update.

## Build on Linux

Needs JDK 17 or 21 (Gradle 8.11 doesn't run on Java 25) and the Android SDK
(Android Studio, or command-line tools with `ANDROID_HOME` set).

Install the JDK:

```
sudo dnf install java-21-openjdk-devel       # Fedora
sudo zypper install java-21-openjdk-devel    # openSUSE
sudo apt install openjdk-21-jdk              # Debian / Ubuntu
```

Build (point `JAVA_HOME` at the JDK if it isn't your default):

```
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleRelease   # openSUSE: /usr/lib64/jvm/...
# -> app/build/outputs/apk/release/app-release.apk
```

## Notes

- `app/release.jks` is the signing key (password `clashtimers`). Keep the repo
  private; all builds use this key so updates install over each other.
- Names and upgrade durations come from `app/src/main/assets/coc_data.json`.
  After a game update, regenerate it from the `clash-of-clans-data` npm package:
  `python3 tools/build_table.py <package>/data app/src/main/assets/coc_data.json`.
