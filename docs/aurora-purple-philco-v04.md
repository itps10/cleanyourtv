# Aurora Purple for PHILCO 2020/2021 UHD Android TV (TPV, Android 11)

This is a **separate experimental branch** so the existing CleanYourTV installation stays untouched.

## Features
- Violet focus spotlight that moves with the selected app, retaining a little of its platform hue.
- Native `RollingSecondsView`: a 2-digit seconds dial that rolls every second; updates stop when Home is not visible.
- Same-sized streaming cards with dark Netflix, Prime Video, Disney+, Max, YouTube, Globoplay, Spotify, Boosteroid-specific gradients; original application icons preserved.
- Zoom transitions (240 ms) when launching apps.
- Heavy blur/video/wallpaper polling intentionally avoided for ~1.3 GB RAM hardware.
- Optional `themes/aurora-purple-philco/config.json` as a lightweight no-wallpaper variant.

## Build
GitHub Actions -> **Compilar CleanYourTV oficial (APK)** -> **Run workflow**, branch `aurora-purple-philco-v04`.
Download the `CleanYourTV-APK` artifact, extract `app-release.apk`.

## Updating an existing installation

**Warning:** the workflow signs with the build runner's automatically generated debug key. A subsequent run may have a *different signing key*. In that case Android rejects `adb install -r` with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Do **not** blindly uninstall the active Home app. Switch and test the **enabled stock Google launcher** first, back up package/theme state, then carefully reinstall and reassign Home. Never disable `com.google.android.tvlauncher` before testing recovery.

The separate package [Aurora_Purple_v04_Philco.zip] from ChatGPT includes Termux scripts that try an in-place update and stop safely on signing mismatch, plus an optional purple wallpaper and theme. It does not itself include a compiled APK.

## Manual rollback

Keep `com.google.android.tvlauncher` enabled. To open the original Google Home without uninstalling the custom launcher:

```bash
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME -n com.google.android.tvlauncher/.MainActivity
```

Do not disable or uninstall stock core, HDMI, tuner, MediaTek or TPV service packages.
