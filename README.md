# Remote Control

Android kiosk shell that locks a tablet to a single web app (e.g. a POS PWA)
and keeps it locked across reboots. Built for tablets you own and provision
yourself — not distributed through the Play Store.

## How it works

- **Device Owner + Lock Task Mode** — once provisioned (see below), the app
  pins itself as the only allowed app, disables the home/recents/status bar,
  and blocks factory reset and safe boot from Settings.
- **WebView, not a browser** — no address bar, tabs, or menu; loads the
  configured URL full-screen and keeps all navigation inside itself.
- **First run** asks for a URL and an admin PIN, then locks immediately.
- **Reboot recovery** — a `BOOT_COMPLETED` receiver relaunches the app, and it
  registers itself as the device's Home app so Android returns to it if it's
  ever killed.
- **Changing the URL later** — tap the top-left corner 5 times within 2
  seconds to bring up the PIN prompt, then the admin menu (edit URL/PIN, or
  exit kiosk mode for maintenance).
- **Maintenance mode** — "Exit kiosk mode" genuinely leaves: it drops the
  app's claim on the Home role (both the Device Owner persistent-preferred
  entry and the regular one — clearing only one left Android silently
  routing the Home button back to this app) and stops lock task, so the
  device is fully navigable until "Resume kiosk" is tapped. Survives the
  app process being killed mid-maintenance.
- **HTTPS only** — the setup screen rejects `http://` input outright rather
  than silently passing it to the WebView, since the manifest disables
  cleartext traffic; a cleartext URL there would blank-screen with no
  explanation, which was the exact failure mode a prior third-party kiosk
  tool hit against this same POS PWA.

## Build

```bash
./gradlew assembleDebug     # unsigned debug APK, app/build/outputs/apk/debug/
./gradlew assembleRelease   # needs a signing config added to app/build.gradle.kts first
```

Requires JDK 17+, Android SDK with `platforms;android-36` and
`build-tools;36.0.0`+ installed.

## Provisioning a tablet as Device Owner

Device Owner mode requires the device to have **no Google account added yet**
— either fresh out of the box, or after a factory reset.

**For a handful of tablets (manual, via adb):**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell dpm set-device-owner com.remotecontrol.kiosk/.KioskDeviceAdminReceiver
```

If the tablet has already been through Android's setup wizard (common on an
emulator, or a device you've been testing with), `dpm set-device-owner` will
refuse with "already some accounts on the device" even with zero accounts
present — it also requires setup to be incomplete. Work around it for
testing with:

```bash
adb shell settings put secure user_setup_complete 0
adb shell settings put global device_provisioned 0
adb shell dpm set-device-owner com.remotecontrol.kiosk/.KioskDeviceAdminReceiver
adb shell settings put secure user_setup_complete 1
adb shell settings put global device_provisioned 1
```

A real tablet provisioned via QR code or zero-touch during its actual
first boot won't hit this.

**For a larger fleet:** use QR-code provisioning at the Android setup wizard's
"tap 6 times" screen, or zero-touch enrollment if the tablets came from a
zero-touch-registered reseller — both trigger the same Device Owner grant
without needing a computer per device.

Without Device Owner, the app still runs normally (useful for local
development) — it just won't lock the device.

## Verified on-device

Built and exercised end-to-end on an Android 37-preview emulator (Device
Owner provisioning, first-run setup with the on-screen keyboard, WebView
load, PIN dialog, exit/resume maintenance cycle including the real
"Select a Home app" chooser, and the `http://` rejection) — not just
compiled. Still worth a pass on real tablet hardware before a production
rollout, particularly `adb shell dpm set-device-owner` against whatever
specific tablet model you're deploying, since cheap/non-certified firmware
can strip that support even when the emulator behaves.

## Known follow-ups

- No release signing config yet — add one before distributing outside adb.
- Admin PIN is stored in plain `SharedPreferences`; fine for a PIN-length
  secret gating a physically-controlled device, not intended as strong
  security.
- No remote fleet dashboard / MDM integration (deferred — see project notes).
