# maxon ESCON 36/2 DC — Vendor Documentation

Reference documentation for the servo controller the firmware drives. The board
commands it through an analog set-value input fed by a DS1267 digital
potentiometer; motor constants derived from these documents live in
`firmware/config.h` (`TORQUE_CONSTANT` 23.5 mNm/A, `SPEED_CONSTANT` 406 rpm/V).

| File | |
|---|---|
| `403112_ESCON_36_2_DC_Hardware_Reference_En.pdf` | Hardware reference — wiring, I/O, specifications |
| `403112_ESCON_Feature_Comparison_Chart_En.pdf` | Feature comparison across the ESCON range |
| `403112_ESCON_36_2_DC_Firmware_Version_Readme_En.pdf` | Controller firmware version notes |
| `Prod.ino_ESCON.pdf` | Product flyer |
| `escon_controller_smaller.pdf` | Condensed controller overview |
| `Release_Notes_en.pdf` | ESCON Studio (Windows configuration tool) release notes |

## Copyright

These are **not original work.** They are published by maxon motor ag
(Brünigstrasse 220, CH-6072 Sachseln, Switzerland) and are reproduced here
solely as the hardware reference this project was built against. All rights
remain with maxon. Current versions are available from maxon directly.

See [`../../NOTICE.md`](../../NOTICE.md).
