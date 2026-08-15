# Notice — Rights and Third-Party Attribution

## Rights

This project is published **for reference purposes only**, as an engineering
portfolio artifact. **All rights reserved to UC Berkeley and UCSF.**

No license — open source or otherwise — is granted to use, copy, modify, or
distribute the original work in this directory. It is made visible so that the
engineering can be read and evaluated, nothing more.

This reservation applies to the **original work only**. It does not and cannot
extend to the third-party components listed below, each of which remains under
its own license and its own copyright holder.

## Not a medical device

This is archival research and prototype code from approximately 2012–2013. It
has not been cleared or approved by any regulatory body, carries no verification
or validation record, and must not be used in the care of any patient. All
sample patient records in the source are fictitious (see
`android/src/edu/ucsf/roboimplant/db/DatabaseHelper.java`).

## Third-party components

### Android application

| Component | Copyright | License |
|---|---|---|
| `src/com/quietlycoding/android/picker/NumberPicker.java`, `NumberPickerButton.java` | © 2008 The Android Open Source Project | Apache-2.0 |
| `src/edu/ucsf/roboimplantconsole/bluetooth/BluetoothSerialService.java` | © 2009 The Android Open Source Project | Apache-2.0 |
| `src/edu/ucsf/roboimplantconsole/bluetooth/DeviceListActivity.java` | © The Android Open Source Project | Apache-2.0 |
| `src/edu/ucsf/roboimplant/db/PatientDbAdapter.java`, `AdjustmentDbAdapter.java` | © 2008 Google Inc. | Apache-2.0 |
| `src/edu/ucsf/roboimplant/menu/TabsAdapter.java` | © 2011 Andreas Stuetz | Apache-2.0 |
| `src/edu/ucsf/roboimplant/generic/CSVReader.java` (opencsv) | © 2005 Bytecode Pty Ltd | Apache-2.0 |
| `libs/android-support-v4.jar` | © Google Inc. | Apache-2.0 |
| `jars/GraphView-2.5.jar` | © jjoe64 | LGPL-3.0 (GraphView 2.x) |
| `jars/com.jjoe64.graphview.graphviewdemo.jar` | © jjoe64 | LGPL-3.0 — demo app, not a dependency |

License headers are preserved verbatim in each source file.

### Firmware

| Component | Origin | License |
|---|---|---|
| `firmware/sdcard/*` — `sd_routines`, `fat32`, `spi_routines`, `i2c_routines`, `rtc_routines`, `sd_main` | CC Dharmani, Chennai — [dharmanitech.com](http://www.dharmanitech.com/2009/01/sd-card-interfacing-with-atmega8-fat32.html), v2.4.1, 2011 | **No license stated by the author.** Attribution preserved in headers. Locally modified to add `../uart.h` and `../config.h` includes. |
| `firmware/Makefile` | WinAVR sample Makefile — Eric B. Weddington, Jörg Wunsch et al., modified by Elliot Williams (*Make: AVR Programming*) | Sample/public; header preserved |
| `firmware/adc_ad7715.h` (block inside `#if 0`) | Analog Devices AD7715 datasheet sample code for the 68HC11 | Datasheet sample, reproduced for reference |

### Unresolved provenance

Flagged honestly rather than quietly asserted:

- **`android/src/edu/ucsf/roboimplantconsole/bluetooth/BlueTerm.java`** carries **no
  license header**. The upstream project of that name (pymasde.es / Adam Alexander)
  is GPLv3. The file here is a rewritten singleton wrapper rather than an evident
  copy, but the shared name and absent header leave its origin unconfirmed. It is
  therefore **not** asserted to be covered by the rights reservation above.

### Removed before publication

Deliberately excluded from this repository because their origin could not be
verified. They remain in the original working folder.

- **Anatomical spine mesh** — `spine.obj`, `spine.dae`, `spine.off`,
  `spine_sketchup7.skp`, and the runtime assets `android/res/raw/spine_obj` and
  `spine_lowres_obj`. No embedded author or copyright; possibly sourced from a
  3D model library. Their removal means the app's 3D spine view has no geometry
  to load. Rod geometry (`rod.obj`, `rod.dae`, `Metal Rod.skp`, `rod_obj`) is
  original and retained.
- **Presentation imagery** — animated gear clipart and stock spine photographs of
  unknown, likely web-sourced origin. (The images now in `docs/images/` are
  different files: original screenshots and bench photographs of this system,
  taken by the author.)
- **`firmware/sdcard/source_code_snippets_from_mozzi.c`** — no header, no
  attribution, filename implying the Mozzi Arduino library (LGPL-3.0 at the
  time), and not referenced by any build.

### Referenced but not included

Neither is redistributed here; both are external dependencies of the Eclipse project.

- **min3d** — Android OpenGL ES 3D engine (Lee Felarca), MIT. Used for the 3D spine view.
- **AChartEngine 1.0.0** — Apache-2.0. Charting.

### Vendor documentation

`docs/motor/` contains servo-controller documentation published by
**maxon motor ag** (Brünigstrasse 220, CH-6072 Sachseln, Switzerland) for the
ESCON 36/2 DC controller. These are maxon's copyrighted manuals, included here
as the hardware reference the firmware was written against. They are not original
work and are reproduced solely for reference. Current versions are available from
maxon directly.
