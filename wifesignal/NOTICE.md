# Notice — Rights and Third-Party Attribution

## Rights

This is a **personal project**, built on the author's own time and equipment. It is
published **for reference purposes only**, as an engineering portfolio artifact.
**All rights reserved to Arie Meir.**

No license — open source or otherwise — is granted to use, copy, modify, or
distribute the original work in this directory. It is made visible so that the
engineering can be read and evaluated, nothing more.

This reservation applies to the **original work only**. It does not and cannot
extend to the third-party components listed below, each of which remains under its
own license and its own copyright holder.

## Images

Every photograph in `docs/images/` is an original photograph of this device and its
construction, taken by the author, and is covered by the rights reservation above.

The illuminated arcade buttons, the speaker, and the monitor arm visible in the
photographs are **commercially manufactured products whose designs are not the
author's**, and no claim is made to them. They appear because they are the parts the
device is built from and the desk it lives on.

`docs/images/app-signal-screen.png` is a screenshot of this project's own iOS app.
The only personal information visible in it is the author's own first name, in the
app's title.

## About the other person in this project

This device is a two-person communication channel, and it was built with the
enthusiastic participation of the person at the other end of it. The published
material describes **the author's own side** of that: the office, the hardware, the
server, and the app.

Deliberately **not** published: her name, her device, her Apple ID, her TestFlight
membership, her Tailscale account, and the operational notes describing how she was
onboarded. That is her information rather than the author's, and none of it is
needed to read the engineering. The internal handoff document that contained those
details is not part of this published copy.

## Redacted before publication

This project was developed against specific physical hardware on a private network.
The following were replaced with placeholders throughout the published copy.

| Item | Replaced with |
|---|---|
| Apple Developer signing team ID | `YOUR_TEAM_ID` |
| App Store Connect API key ID | `$WIFESIGNAL_KEY_ID` (environment variable) |
| App Store Connect issuer ID | `$WIFESIGNAL_ISSUER_ID` (environment variable) |
| Tailscale tailnet identifier | `your-tailnet` in `<host>.your-tailnet.ts.net` |
| Absolute paths in the LaunchAgent | `__PROJECT_DIR__`, `__HOME__` |
| Project directory in shell scripts | derived from the script's own location |

The LaunchAgent is published as `ops/com.ariemeir.wife-signal.plist.example` rather
than as a directly installable file, because installing it requires substituting
those paths first.

**No credential, key, or token appears in this directory at any point in its
history.** The API token and the App Store Connect private keys (`.p8`) were
excluded from version control by `.gitignore` from the first commit of the original
project and were never committed. The published copy is a fresh snapshot with no
inherited history, and it carries only `server/.env.example`, whose token field is
empty.

## Removed before publication

- **The internal handoff document** — an operations runbook written for whoever
  picked the project up next. It contained the live API token, device UDIDs, local
  machine paths, and the third-party details described above. Its *technical*
  content survives in this README; the document itself does not.
- **`device/esphome_example.yaml`** — an abandoned ESPHome / Home Assistant design
  that never matched the built hardware (it drove the switch pins as lamp outputs).
  It was already marked stale in the original repo. Removed rather than published
  as a config that invites bug reports.
- **`device/WifeSignal_dimensions.txt`** — a v12 dimension sheet superseded by the
  v15 generator script, which is the CAD source of truth.
- **The generated `.xcodeproj`** — reproducible from `app/project.yml` with
  XcodeGen, and the checked-in copy duplicated the signing team ID.
- **KiCad `.kicad_prl` files** — per-user local editor state, not design data.

No original engineering work was removed.

## Third-party components

None are vendored into this repository. All are external dependencies, listed here
because the code is written against them.

### Firmware

| Component | Copyright | License |
|---|---|---|
| **NimBLE-Arduino** (v2.x) — BLE peripheral stack | © h2zero and contributors | Apache-2.0 |
| **arduino-esp32** core | © Espressif Systems | LGPL-2.1-or-later / Apache-2.0 |
| **arduino-cli** — invoked to compile and upload | © Arduino SA | AGPL-3.0 (tool, invoked not linked) |

### Server (Python)

| Component | License |
|---|---|
| **aiohttp** (≥ 3.10) — HTTP server | Apache-2.0 |
| **bleak** (3.0.2) — cross-platform BLE central | MIT |

`bleak` wraps Apple's CoreBluetooth on macOS; CoreBluetooth is a system framework
and is not redistributed here.

### iOS application

**No third-party Swift packages.** The app is built entirely against Apple system
frameworks — SwiftUI and Foundation. The API client, polling loop, and settings
storage are all original.

| Tool | Copyright | License |
|---|---|---|
| **XcodeGen** — generates `.xcodeproj` from `project.yml` | © Yonas Kolb | MIT |

### Hardware and CAD

| Component | Copyright | License / terms |
|---|---|---|
| **KiCad** — schematic capture, PCB layout, gerber export | © KiCad developers | GPL-3.0 (tool) |
| KiCad standard footprint libraries (`PinHeader_*`, `R_Axial_*`, `TO-92_Inline`) | © KiCad developers | CC-BY-SA-4.0 with an explicit exception permitting unrestricted use of boards produced with them |
| **Autodesk Fusion 360** — the enclosure script targets its Python API | © Autodesk, Inc. | Commercial; not redistributed |

The enclosure script is original work written against the Fusion 360 API. No
Autodesk code is reproduced here.

### Referenced but not included

- **Tailscale** (© Tailscale Inc.) — provides the TLS-terminated, tailnet-only
  transport via `tailscale serve`. Referenced by the ops script; not a build
  dependency and not redistributed.
- **launchd** — macOS system service manager. The published LaunchAgent is an
  original property list.
- The **ESP32-C3 SuperMini Plus** module and the 30 mm illuminated arcade buttons
  are commercially manufactured parts, described here only by their interfaces.
