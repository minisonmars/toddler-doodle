# Toddler Doodle

A simple drawing app for toddlers, with big buttons and a PIN-protected parent area.

## What it does

- **Drawing paper** that fills most of the screen. Several fingers can draw at once.
- **10 colours** in one line that scrolls across: red, orange, yellow, green, light blue, dark blue, purple, pink, brown and black. One brush size.
- **Eraser** (purple button, at the start of the colour line). Tap it again, or tap a colour, to go back to drawing.
- **Home** (grey house) goes to the phone's home screen, such as Toddler Launcher. The drawing is still there when the app is opened again.
- **Undo** (orange) takes away the last stroke. It can also bring back a cleared drawing.
- **Clear** (red bin) has to be **held for 1 second**. A ring fills up while it's held; a quick tap just makes it wiggle.
- **Save** (green) keeps the drawing in the app's own gallery and shows a big tick.
- **Gallery** (blue) shows only the saved doodles, never the phone's photos. Tap one to see it full screen, with arrows for the next and previous.
- The Back button is ignored on the drawing screen so a stray tap doesn't lose the drawing. Home still works.
- It works in portrait (two lines of buttons underneath) and landscape (two columns down the right, the colours scroll up and down).

## Parent area

Hold the grey gear in the top corner for **2 seconds**, then enter the PIN. The starting PIN is **0000**.

- Turn the Save, Eraser and Gallery buttons on or off.
- **Saved doodles**: look at, copy to Photos (Pictures > Toddler Doodle) or delete, one at a time or all at once.
- Change the PIN and set a recovery word (resets the PIN if you forget it).
- **App updates**: the app quietly checks GitHub for a new version (each time the parent area opens, and at most every 6 hours otherwise). When there is one, this line turns red and says which version. It opens the versions screen: new versions with what changed at the top, then the installed version, older versions behind "Show older versions", and reinstall.

Settings apply straight away.

## Installing

1. On the phone, open the [Releases page](https://github.com/minisonmars/toddler-doodle/releases) and download the newest `ToddlerDoodle-v….apk`.
2. Open it and allow your browser to install apps if Android asks.
3. Later updates install over the top (every build is a release build signed with the same key), and the saved doodles are kept. On Android 12 and later, once the app installed the previous version itself, updates go straight in without Android's confirm screen (the first self-update still shows it once).

If you use Toddler Launcher, you can also install and update it from the launcher's parent area under **Recommended apps**.
If Toddler Guard is on, it may close Android's install screen when updating from inside Toddler Doodle; update from the launcher instead.

## How it's built

- Native Android, Kotlin, minSdk 26 (Android 8.0), target/compile SDK 35 (needed for updates without the confirm screen; screens are padded around the system bars, see `Insets.kt`). UI and icons are drawn in code (no XML layouts, no Compose, no emoji).
- GitHub Actions builds the APK on every push to `main` and publishes a GitHub Release `v0.1.<run number>` with the APK `ToddlerDoodle-v<version>.apk` attached.
- Release notes come from `CHANGELOG.md`: add user-facing bullets under `## Next`. The build uses them as the release notes, renames the section to the version, and keeps every release's notes on GitHub in step (`.github/scripts/changelog.py`).
- The repo must stay public, because the in-app updater reads GitHub without a login.
- `app/debug.keystore` is a fixed key so new APKs install over old ones. CI builds `assembleRelease` (not debuggable), signed with it.

## Layout

| File | What it is |
| --- | --- |
| `MainActivity.kt` | Drawing screen and its buttons |
| `DoodleView.kt` | The paper: strokes, undo, clear, snapshot |
| `GalleryActivity.kt` | Gallery for the child, and the parent's manage mode |
| `Doodles.kt` | Saving, loading and copying doodles to Photos |
| `ParentActivity.kt` | Parent area |
| `Gate.kt` | PIN and recovery word dialogs |
| `Settings.kt` | Saved options and the PIN hash |
| `Updater.kt`, `InstallReceiver.kt` | Automatic update check and versions screen (GitHub Releases), same as the other apps |
| `Icons.kt` | Round buttons with icons drawn in code, and the hold-to-use button |
| `Ui.kt` | Colours, sizes and helpers for the parent area |
