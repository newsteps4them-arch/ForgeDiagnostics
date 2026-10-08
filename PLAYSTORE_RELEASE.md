# ForgeDiagnostics — Play Store Release Runbook

How to take this app from this repo to a Play Store listing. Do the steps in order.

## 0. Land the honest-data fix first

The `fix/honest-hardware-mode` pull request **must be merged before any store
submission**. Play's Deceptive Behavior policy rejects apps that present
simulated readings as live diagnostics, and store screenshots/descriptions must
never show simulated gauges as real vehicle scans.

## 1. One-time: Google Play developer account

- Recommended: an **organization** account for ForgeDiagnostics LLC.
  Google requires a **D-U-N-S number** for the LLC — request it now, it can
  take up to 30 days and it is the longest lead-time item on this list.
- New **personal** accounts must run a closed test with **12 testers opted in
  continuously for 14 days** before applying for production. Organization
  accounts with a D-U-N-S number are exempt from this gate.

## 2. One-time: generate the upload keystore (Michael, on your machine)

The release build refuses to sign without this — it will never silently fall
back to the debug key. The keystore file and passwords are yours to keep;
**never commit them.**

```bash
keytool -genkeypair -v \
  -keystore ~/forge-release.keystore \
  -alias forge-upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then make the values available to the build, either as environment variables
or in `local.properties` (which is git-ignored):

| Variable / property          | Example                        |
|------------------------------|--------------------------------|
| `FORGE_RELEASE_STORE_FILE`   | `/home/michael/forge-release.keystore` |
| `FORGE_RELEASE_STORE_PASSWORD` | (your store password)        |
| `FORGE_RELEASE_KEY_ALIAS`    | `forge-upload`                 |
| `FORGE_RELEASE_KEY_PASSWORD` | (your key password)            |

Keep a backup of the keystore + passwords somewhere safe (password manager).
If you lose the upload key, Google can reset it, but it costs you days.

## 3. Build the release bundle

```bash
./gradlew :app:bundleRelease
```

This produces `app/build/outputs/bundle/release/app-release.aab`.
CI (`:app:testDebugUnitTest` at minimum) must be green before you upload.

What this branch already configured for you:
- `targetSdk` / `compileSdk` 36 (required for new Play apps)
- Application ID `com.forgediagnostics.app` (was an auto-generated
  `com.aistudio.teamforge.a1b2c` placeholder — the ID is permanent once
  published, so it was fixed before first upload)
- Release signing reads only from your upload keystore (debug key no longer
  used for releases)
- R8 minification + resource shrinking on for release builds
- Bluetooth permissions modernized: `BLUETOOTH_SCAN` (never for location) +
  `BLUETOOTH_CONNECT`; legacy permissions capped at Android 11; no location
  permission declared or requested (the app does not use location)
- Removed unused `RECORD_AUDIO` permission; kept `CAMERA` (VIN barcode scan)

## 4. Enroll in Play App Signing

In Play Console: upload the `.aab`. Google holds the final app signing key;
your upload key signs each bundle you send. This is the standard setup —
accept the defaults.

## 5. Data safety declaration (fill in truthfully in Play Console)

- VIN → NHTSA lookup: this transmits a vehicle identifier off-device.
  Declare it under "Device or other IDs", purpose "App functionality".
- DTCs, live sensor readings, freeze frames: processed on-device only.
  Not "collected" for the declaration.
- Reconcile the declaration against actual network traffic before submitting —
  a mismatch is the most common rejection reason.

## 6. Store listing

- Screenshots and video must show **real or clearly labeled simulated** data —
  never present the simulator as a live vehicle scan.
- Write the description for mechanics: what adapters are supported
  (Bluetooth ELM327 / USB), what the app does offline, what needs internet
  (NHTSA lookups).

## Still to do (functional, not in this branch)

- **Runtime Bluetooth permission flow**: the manifest declares
  `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` but no screen currently requests them
  at runtime — on Android 12+ the adapter connection will fail until a
  permission request is added to the connection flow.
- **Real-vehicle validation**: the virtual ELM327 test bench covers protocol
  behavior; at least one real car session should be run before production.
- **Monetization decision**: one-time purchase vs subscription determines
  whether the Play Billing Library must be integrated.
