# Firmware — ESP32-C3 BLE Peripheral

A BLE peripheral exposing one service with one characteristic carrying one byte.
134 lines. It drives three illuminated arcade buttons and reports physical presses
back to the server.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/wifesignal/firmware)**

| | |
|---|---|
| Board | ESP32-C3 SuperMini Plus (`ESP32C3 Dev Module`, USB CDC On Boot = Enabled) |
| BLE stack | NimBLE-Arduino v2.x |
| Service UUID | `6d5f0001-4b6b-4a3a-9e1e-2a7b1c9f0001` |
| STATE characteristic | `6d5f0002-4b6b-4a3a-9e1e-2a7b1c9f0002` — `READ` \| `WRITE` \| `NOTIFY`, 1 byte |
| State byte | `0` all off, `1` red, `2` yellow, `3` green |

## Pins as built

| | Red | Yellow | Green |
|---|---|---|---|
| **switch** (`INPUT_PULLUP`, press = LOW, COM → GND) | GPIO3 | GPIO4 | GPIO5 |
| **lamp** (via 1 kΩ into a transistor base) | GPIO6 | GPIO7 | GPIO10 |

Plus the onboard LED on GPIO8 (inverted, LOW = ON) mirroring `state != 0`.

**GPIO 2, 8 and 9 are strapping pins; 20 and 21 are UART0.** All five are avoided
for channel use. Strapping pins are sampled at reset to select boot mode — hanging
a lamp driver off one yields a board that works perfectly until the day it refuses
to start.

## ⚠️ Lamp polarity is inverted on purpose

The soldered drivers are **S8550 PNP**. The schematic says **S8050 NPN** — an
assortment-kit mixup found after the board was populated. The parts on the board
win:

```c
digitalWrite(LAMP[i], (state == i + 1) ? LOW : HIGH);   // PNP: LOW = ON
```

**Do not "correct" this to NPN logic.** Reworking three transistors on a working
board buys nothing but agreement with a drawing.

### The boot-flash fix

A pin that has not yet been configured as an output floats, and a floating base on
a PNP low-side driver reads as *on* — so every boot flashed all three lamps. The
fix is an ordering trick worth remembering:

```c
digitalWrite(LAMP[i], HIGH);   // latch the level into the output register first
pinMode(LAMP[i], OUTPUT);      // then enable the driver
digitalWrite(LAMP[i], HIGH);   // and assert it again
```

Writing before `pinMode` sets the output register while the pin is still an input,
so the pin never passes through an undefined state on its way to being driven.

## Button semantics

These must stay in lockstep with the server's notify classifier:

| Condition | Result |
|---|---|
| press any button while `state != 0` | `state = 0` + notify — **this is the acknowledgement** |
| press a button while `state == 0` | `state` = that button's colour; the server writes it back off (reserved "ping back" hook) |

Debounce is 40 ms with a per-button `handled` flag, so holding a button produces one
event rather than a stream.

## Files

| File | Status |
|---|---|
| [`wifesignal_3ch/wifesignal_3ch.ino`](wifesignal_3ch/wifesignal_3ch.ino) | **Production.** The 3-channel firmware running on the Rev A board. |
| [`wifesignal_bletest.ino`](wifesignal_bletest.ino) | **Bring-up, superseded.** The 1-channel sketch that proved the BLE service, the button semantics and the inverted polarity before the PCB existed. Same UUIDs, a boolean state, one button on GPIO3 and one lamp on GPIO6. Kept as design history — it is a *different pins and polarity era* and should not be flashed to the current board. |

## Building and flashing

```bash
arduino-cli compile --fqbn esp32:esp32:esp32c3:CDCOnBoot=cdc wifesignal_3ch
```

The board enumerates as `/dev/cu.usbmodem*` over native USB CDC. Two practical
notes, both learned the hard way:

- **Close any serial monitor first.** The port is exclusive, and a stale
  `arduino-cli monitor` produces a "port busy" upload failure that does not name the
  culprit. `lsof /dev/cu.usbmodem*` finds it.
- **Positively identify the board before flashing** if other ESP32 projects share
  the machine. This firmware prints `write -> state N` at 115200 when toggled
  through the API, which is an unambiguous test.

The server reconnects by itself after a flash, and re-asserts the correct lamp state
on connect — so reflashing mid-signal is safe.
