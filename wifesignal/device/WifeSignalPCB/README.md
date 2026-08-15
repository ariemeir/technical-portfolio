# KiCad Project

Schematic, layout and fabrication output for the lamp-driver board.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/wifesignal/device/WifeSignalPCB)**

## Which files matter

| File set | Status |
|---|---|
| `WifeSignal3ch.*` | **The board that was fabricated.** 3 channels, routed (62 track segments), outlined, gerbers exported. |
| `WifeSignal.*` | **Design history.** A stripped 1-channel variant saved minutes apart from the 3-channel schematic during exploration. Never routed, no board outline, never made. |
| `output/` | Gerbers, drill files and the job file sent for fabrication (KiCad 10.0.4). |
| `ERC.json` | Electrical rules check output. |

Only `WifeSignal3ch` exists physically. The 1-channel variant is kept because it
shows the design being narrowed before it was widened — the project briefly
considered proving one channel in copper before committing to three, and decided the
protoboard prototype had already answered that question.

## Board summary

| | |
|---|---|
| Size | 41.0 × 88.5 mm |
| Layers | 2 (F.Cu / B.Cu), 2 copper zones |
| Vias | none |
| Technology | entirely through-hole |
| Drill tools | 0.75 / 0.80 / 1.00 mm; **no** non-plated holes |
| Outline | flat one end, R20.5 arc the other, four R4.7 C-notches at (±16, ±18.5) mm |

## Reading the schematic

The design is three identical low-side switches. Rather than rely on the reader to
infer it, the schematic carries its own notes in the drawing:

```
Each channel identical: GPIO ->1k-> S8050 base; emitter->GND; collector-> button LED-.
Button conn (J3/J4/J5): pin1 LED+ ->+5V, pin2 LED- ->collector, pin3 COM ->GND, pin4 NO ->switch GPIO.
RED CHANNEL     lamp GPIO6  / switch GPIO3
YELLOW CHANNEL  lamp GPIO7  / switch GPIO4
GREEN CHANNEL   lamp GPIO10 / switch GPIO5
Refs R1/Q1/J3=RED  R2/Q2/J4=YELLOW  R3/Q3/J5=GREEN  (carry to PCB silkscreen).
```

That last line is the useful habit: the reference-designator-to-channel mapping is
written into the drawing *and* onto the silkscreen, so the board can be assembled
and debugged without the schematic open.

## ⚠️ Q1–Q3 are wrong on this schematic

The drawing specifies **S8050 NPN**. The board was populated with **S8550 PNP** from
an assortment kit, which inverts the drive logic (GPIO LOW = lamp on). The firmware
is written against the parts that are actually soldered.

This has deliberately not been corrected, and the discrepancy is documented here, in
[`device/README.md`](../README.md), and in the firmware header. If the board is ever
re-spun, this is the first thing to fix — and the firmware's `applyLamps()` must be
inverted in the same commit.

## Regenerating the fabrication output

`output/` is generated from `WifeSignal3ch.kicad_pcb` and is committed only so the
board that was actually ordered is recoverable. Re-export from KiCad's plot dialog
rather than editing anything in it by hand.
