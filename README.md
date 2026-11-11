# FRONX OS

Native Android (Kotlin, no third-party libraries) infotainment launcher for the Maruti Suzuki FRONX Sigma.
Design inspired by premium rugged SUV systems: graphite, charcoal panels, champagne-orange accent, ivory type.

## Build with GitHub Actions (no Android Studio)
1. Create a GitHub repo and upload everything in this folder (the GitHub mobile site / app works).
2. Open **Actions → Build FRONX OS → Run workflow** (it also runs on every push).
3. When it finishes, download **fronx-os-debug-apk** from the run's *Artifacts*. Install the APK on the head unit.

### Optional: signed release APK
Add these repository secrets (Settings → Secrets and variables → Actions):
`KEYSTORE_BASE64` (base64 of your .jks), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
The workflow then also uploads **fronx-os-release-apk**.

## Your FRONX image
Add a transparent PNG (about 1600 px wide) at:
`app/src/main/res/drawable-nodpi/fronx_hero.png`
It is picked up automatically. Until then a simple vector placeholder is shown.

## Customising colours
Settings → Theme: pick a preset, or tap **Custom colour…** for swatches plus Hue / Saturation / Brightness sliders.
Apply stores it; Reset returns to the preset.

## Using it as the head unit's home screen
FRONX OS registers as a launcher. Press Home and choose FRONX OS → Always.

## What is real and what is not
- Real: time/date, Bluetooth / Wi-Fi / GPS state, now-playing + controls (needs "Media access" in Settings),
  GPS speed/position (needs location permission), contacts (needs permission), installed apps, app launching.
- Not available, and labelled as such: fuel level, range, outside temperature, weather, in-app route drawing.
  They need vehicle (CAN/OBD) or online data sources that this app does not have.
- No AC / climate controls: the Sigma has none to control.
- Permissions are requested only when you tap the matching button.

## Layout
`app/src/main/java/com/fronx/os/`: `MainActivity` (shell, top bar, rail), one `*Screen` per module,
`ThemeManager` (+ `Prefs`), `ColorPickerDialog`, `MediaBridge`, `LocationBridge`, `AppAdapter`, `IconView`.
