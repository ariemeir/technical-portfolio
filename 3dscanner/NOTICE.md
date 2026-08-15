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

## Redacted before publication

This project was developed against specific physical hardware on a private network.
The following were replaced with placeholders throughout the published copy. They
are not secrets in the cryptographic sense — no credential, key, or token appears in
this repository at any point in its history — but they identify real devices and
were not worth publishing.

| Item | Replaced with |
|---|---|
| Apple Developer signing team ID | `YOUR_TEAM_ID` |
| Tailscale IP addresses (two devices) | `100.x.y.z` |
| Device UDID | `<DEVICE_UDID>` |
| Paired device name | `<paired iPhone>` |
| USB serial port paths | `/dev/cu.usbmodemXXXXX` |

Also removed: a passage describing the *format* used for the API token on the
author's own device. The token itself was never committed — the live configuration
file (`config/scanner.yaml`) is and always has been excluded from version control,
and the token is read from an environment variable at runtime. Only the template,
`config/scanner.example.yaml`, is published.

## Removed before publication

Excluded because their origin could not be verified. They remain in the author's
original working folder.

- **`hardware/enclosure/Uno_-_CaseBase.stl`** — an Arduino Uno enclosure base
  (2.2 MB, 44,924 triangles). It matches the Uno footprint and is near-certainly a
  third-party model downloaded from a 3D-printing model library, but no source,
  author, or license was recorded at the time. The `hardware/` directory contained
  nothing else and has been dropped entirely.
- **`firmware/turntable_ir/Side 1 with larger pad.stl`** and
  **`Side 2 with larger pad.stl`** — a mirrored two-part printed fixture
  (137.5 × 35 × 125 mm each). Their binary STL headers identify an **Autodesk**
  export toolchain, so they were not produced by this project's own pipeline, and
  nothing anywhere in the repository documents what they are or where they came
  from. Removed rather than published with an invented provenance.

No original work was removed. The scanner's own output geometry is not included
here for a different reason — see below.

## Not included: scan output

Captured images, reconstructed meshes, and packaged scan sessions are excluded by
`.gitignore` as generated data, not for rights reasons. The reconstruction and
print-preparation code is complete and published; only its output is absent.

The pipeline was first verified end-to-end against a **third-party sample
photogrammetry dataset** (the "gingerbread" image set distributed by
RealityCapture) before being run on the rig's own captures. That dataset is not
redistributed here and is referenced only in the engineering log.

## Third-party components

None are vendored into this repository. All are external dependencies, listed here
because the code is written against them.

### Firmware

| Component | Copyright | License |
|---|---|---|
| **IRremote** (v4.x) — Arduino infrared send/receive | © Armin Joachimsmeyer and contributors | MIT |
| **arduino-cli** — invoked by the `burn` build script | © Arduino SA | AGPL-3.0 (tool, invoked not linked) |

The infrared command values in `firmware/turntable_ir/turntable_codes.json` were
captured by the author from a consumer turntable's own remote control, using the
`capture_remote_signals.ino` sketch in this repository. They are measurements of a
physical device's NEC-protocol output — recorded facts about hardware behaviour, not
copied code.

### Controller (Python)

| Component | License |
|---|---|
| **PyYAML** | MIT |
| **pyserial** | BSD-3-Clause |
| **pytest** | MIT |
| **PyMeshLab** — mesh repair bindings | GPL-3.0 |
| **NumPy** | BSD-3-Clause |

`repair.py` imports PyMeshLab, which is GPL-3.0. That is a runtime dependency of a
script published here for reference; no PyMeshLab code is reproduced in this
repository.

### iOS application

**No third-party Swift packages.** ScannerCam is built entirely against Apple system
frameworks — SwiftUI, AVFoundation, Network.framework, CryptoKit, and Security. The
HTTP/1.1 server, its router, request parser, response serializer, and bearer
authentication are all original.

| Tool | Copyright | License |
|---|---|---|
| **XcodeGen** — generates `.xcodeproj` from `project.yml` | © Yonas Kolb | MIT |

### Reconstruction

| Component | Copyright | License |
|---|---|---|
| **RealityKit / Object Capture** (`PhotogrammetrySession`) | © Apple Inc. | Apple system framework — macOS only |
| **Open Asset Import Library (assimp)** — OBJ→STL conversion | © assimp team | BSD-3-Clause |

Apple's Object Capture is a system framework, not redistributed. `objcap` is an
original CLI wrapper around it.

### Referenced but not included

- **MeshLab** and **Blender** — used interactively during mesh cleanup and for the
  boolean drilling step described in the engineering log. Neither is a build
  dependency of anything in this repository.
