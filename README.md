# Toddler Doodle

A simple drawing app for toddlers, with big buttons and a PIN-protected parent area.

## What it does

- **Drawing paper** that fills most of the screen. Several fingers can draw at once.
- **10 colours**: red, orange, yellow, green, light blue, dark blue, purple, pink, brown and black.
- **3 brush sizes** (small, medium, big). The size buttons show a dot in the current colour.
- **Eraser** (purple button). Tap it again, or tap a colour, to go back to drawing.
- **Undo** (orange) takes away the last stroke. It can also bring back a cleared drawing.
- **Clear** (red bin) has to be **held for 1 second**. A ring fills up while it's held; a quick tap just makes it wiggle.
- **Save** (green) keeps the drawing in the app's own gallery and shows a big tick.
- **Gallery** (blue) shows only the saved doodles, never the phone's photos. Tap one to see it full screen, with arrows for the next and previous.
- The Back button is ignored on the drawing screen so a stray tap doesn't lose the drawing. Home still works.
- It works in portrait (buttons underneath) and landscape (buttons down the right).

## Parent area

Hold the grey gear in the top corner for **2 seconds**, then enter the PIN. The starting PIN is **0000**.

- Turn the Save, Eraser and Gallery buttons on or off.
- **Saved doodles**: look at, copy to Photos (Pictures > Toddler Doodle) or delete, one at a time or all at once.
- Change the PIN and set a recovery word (resets the PIN if you forget it).
- **App updates**: install the latest version from GitHub, or pick another one.

Settings apply straight away.

## Installing

1. On the phone, open the [Releases page](https://github.com/minisonmars/toddler-doodle/releases) and download the newest `ToddlerDoodle-v….apk`.
2. Open it and allow your browser to install apps if Android asks.
3. Later updates install over the top (every build is signed with the same key), and the saved doodles are kept.

If you use Toddler Launcher, you can also install and update it from the launcher's parent area under **Recommended apps**.
If Toddler Guard is on, it may close Android's install screen when updating from inside Toddler Doodle; update from the launcher instead.

## How it's built

- Native Android, Kotlin, minSdk 26 (Android 8.0), target/compile SDK 34. UI and icons are drawn in code (no XML layouts, no Compose, no emoji).
- GitHub Actions builds the APK on every push to `main` and publishes a GitHub Release `v0.1.<run number>` with the APK attached.
- `app/debug.keystore` is a fixed debug key so new APKs install over old ones.

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
| `Updater.kt`, `InstallReceiver.kt` | In-app updater from GitHub Releases |
| `Icons.kt` | Round buttons with icons drawn in code, and the hold-to-use button |
| `Ui.kt` | Colours, sizes and helpers for the parent area |
