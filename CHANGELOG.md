# Changelog

What changed in each version of Toddler Doodle. The newest is at the top.
Each build uses the "Next" section as its release notes on GitHub, then renames it to the new version.

## Next

- Updates now install without Android's extra "Update?" screen (Android 12 and later). The first update after this one still asks once; after that the app just closes and you open it again on the new version
- Builds are now release builds (signed with the same key, so they install over this one), which should mean fewer Google Play Protect warnings

## v0.1.3

- Automatic update check: the app quietly looks for a new version on GitHub (when the parent area opens, and at most every 6 hours otherwise). When there is one, "App updates" in the parent area turns red and says which version
- New versions screen, the same as the other apps: new versions with what changed at the top, then the installed version, older versions behind "Show older versions", and reinstall

## v0.1.2

- Home button (grey house) goes to the phone's home screen; the drawing is kept
- One brush size instead of three
- The eraser and the 10 colours are in one line, and the colours scroll across, so the drawing area is bigger

## v0.1.1

- First version: 10 colours, 3 brush sizes, eraser, undo, clear (hold for 1 second), save to the app's own gallery, PIN-protected parent area and in-app updates
