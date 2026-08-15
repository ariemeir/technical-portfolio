# Android Console

The clinician-facing tablet app. Connects to the controller over Bluetooth SPP,
runs a lengthening procedure, and displays live telemetry against a 3D render of
the spine and rod.

Eclipse ADT project, package `edu.ucsf.roboimplantconsole`, circa 2012–2013.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/roboimplant/android)**
· [`src/`](https://github.com/ariemeir/technical-portfolio/tree/main/roboimplant/android/src/edu/ucsf)

| | |
|---|---|
| minSdkVersion | 14 (Android 4.0) |
| targetSdkVersion | 15 |
| Compile target | `Google Inc.:Google APIs:16` |
| Transport | Bluetooth RFCOMM/SPP, UUID `00001101-0000-1000-8000-00805F9B34FB` |

## ⚠️ Does not build as shipped

Two dependencies are referenced by **absolute path on the original developer's
machine** and are not included in this repository:

```
.classpath  →  /home/ariemeir/unison-root/dev/android/achartengine/achartengine-1.0.0.jar
.project    →  /home/ariemeir/dev/min3d/src   (linked source folder "min3d")
```

- **min3d** — OpenGL ES 3D engine, used for the spine/rod visualization. MIT.
- **AChartEngine 1.0.0** — charting. Apache-2.0.

Building requires obtaining both and fixing the paths. The project also targets
the **Google APIs add-on** for API 16, not the plain `android-16` platform, so
`project.properties` needs editing to build against a stock SDK. Preserved
unmodified as a historical snapshot rather than retrofitted to Gradle.

## Structure

Two package trees, reflecting two development phases:

**`edu.ucsf.roboimplantconsole`** — device communication and the procedure itself.

| Class | Role |
|---|---|
| `AdjustmentActivity` | Launcher. The procedure screen — dosage entry, live telemetry, 3D view |
| `MessageDispatcher` | Parses `bt*` telemetry, fans out to `RoboMessageListener`s and static UI hooks |
| `CalibrationActivity` | Captures no-load / loaded current and speed calibration points |
| `ServiceTerminal` | Engineering view — raw terminal and graphs |
| `RoboImplantPreferences` | Settings |
| `SoundNotifier` | Audio cues for procedure events |
| `bluetooth/BluetoothSerialService` | Connection state machine (AOSP-derived) |
| `bluetooth/DeviceListActivity` | Device picker |
| `bluetooth/BlueTerm` | Singleton terminal-services wrapper — see [`../NOTICE.md`](../NOTICE.md) |

**`edu.ucsf.roboimplant`** — patient records and UI scaffolding.
`menu/` (patient and adjustment lists, tabs), `ui/` (patient and adjustment
screens), `db/` (SQLite adapters), `data/`, `generic/`, `calib/`.

## Notes on the code

- **All sample patient data is fictitious** — `db/DatabaseHelper.java` seeds
  "Jack London" and "Steve Jordan" with invented Bay Area addresses and phone
  numbers. No real patient data has ever been in this repository.
- **`db/DBSimulator.java` is vestigial.** It loads `_so.csv`, `_labor.csv`, and
  `_parts.csv` — service orders, labor, and parts — from `/sdcard/roboimplant/`.
  That is the data model of a field-service application this project borrowed
  scaffolding from, unrelated to the implant domain. No CSVs ship here. Kept
  because it is part of the honest history of the codebase.
- **Over-broad permissions.** The manifest requests `ACCESS_SURFACE_FLINGER`
  (signature-level, never granted to a normal app), `ACCESS_FINE_LOCATION`,
  `READ_PHONE_STATE`, `INTERNET`, and `ACCESS_BACKGROUND_SERVICE` — which is not
  a real Android permission. Only `BLUETOOTH`, `BLUETOOTH_ADMIN`, and `VIBRATE`
  are actually needed. Vestigial, and would not survive review today.
- **`generic/ConfigDB.java` hardcodes bench-hardware Bluetooth MACs**
  (`CAMERA_CLICKER_ADDRESS`, `MOTOR_CONTROLLER_ADDRESS`).
- `bin/` and `gen/` (build output, including a compiled APK) are excluded via
  `.gitignore`.
- **The spine meshes in `res/raw/` were removed** (`spine_obj`, `spine_lowres_obj`)
  because their provenance could not be verified — the 3D view has no spine
  geometry to load. `rod_obj` is original and retained. See [`../NOTICE.md`](../NOTICE.md).

See [`../README.md`](../README.md) for the serial protocol both sides implement.
