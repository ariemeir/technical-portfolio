# Firmware — ATmega1284P Controller

AVR C firmware for the handheld driver unit. Parses commands from the Android
console over Bluetooth serial, drives the maxon ESCON servo controller, counts
rod rotations, and streams telemetry back.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/roboimplant/firmware)**

## Target

| | |
|---|---|
| MCU | ATmega1284P |
| Clock | 16 MHz |
| Toolchain | avr-gcc / WinAVR-style Makefile |
| Programmer | `avrispv2` on `/dev/ttyACM0` |
| Serial | 57600 baud |

## Build

```sh
make          # build main.elf / main.hex
make program  # flash via avrdude
```

Requires the AVR toolchain (`avr-gcc`, `avr-libc`, `avrdude`).

## Design

**Time base.** Timer2 in CTC mode, `OCR2A = 250`, prescaler 64 → an interrupt
every 1.024 ms at 16 MHz. This is the millisecond clock for the whole system
(`TIME_COUNTS_IN_ONE_SEC`). The 2.4% error is acknowledged in a comment in
`config.h` — an external 32 kHz crystal was the intended fix.

**Speed control.** Motor speed is *not* set by the AVR's PWM in the shipped
signal path. A **DS1267 digital potentiometer** on SPI (`digitalPot.c`) feeds the
analog set-value input of the ESCON controller; `HALTING_SPEED_VALUE` (110) is
the pot step corresponding to zero speed. A hardware PWM on Timer0/OC0A (`pwm.c`)
exists alongside it and is reachable via `atpwm`.

**Rotation counting.** External interrupt INT2 on the rising edge increments
`spinCount`. Procedure targets are expressed in spin counts, derived from the
requested millimetres via `MM_TO_ROTATIONS_FACTOR`.

**Sensing.** `adc.c` reads motor current and voltage through a conditioning chain
centred at Vcc/2. Motor constants (`TORQUE_CONSTANT` 23.5 mNm/A, `SPEED_CONSTANT`
406 rpm/V) come from the maxon datasheet and let torque and speed be inferred
from current and voltage without extra sensors. Samples feed a cumulative moving
average (`HISTORY_AVG_WEIGHT`, `AVG_HISTORY_LENGTH`).

**Coupling detection.** Motor current is thresholded at
`COUPLING_THRESHOLD_CURRENT` (35 mA) to decide whether the driver is engaged with
the implant. Transitions are pushed to the console immediately via
`notifyCoupling()` rather than waiting for the next periodic update, because
losing coupling mid-procedure has to surface to the clinician at once.

**I/O.** Dual UART — stdout to the Bluetooth module on UART1, stderr to RS-232 on
UART0, switchable with `USE_RS232_UART`. A Parallax 5-way joystick
(`parallax5buttonjoystick`) provides local control without the tablet.

## What is and isn't built

The `SRC` line in the Makefile compiles:

```
main.c uart.c adc.c spi.c messageDispatcher.c utils.c
pwm.c printFloat.c power.c calibration.c digitalPot.c parallax5waybutton.c
```

Present but **excluded from the build**:

- **`sdcard/`** — SD/FAT32 datalogging, a self-contained sub-project with its own
  Makefile. Third-party (CC Dharmani); see [`../NOTICE.md`](../NOTICE.md). One
  unattributed file was removed from it before publication.
- **`adc_ad7715.c` and `ad7715/`** — driver for an AD7715 16-bit sigma-delta ADC,
  the front end for a load-cell/bridge force-sensing path that was explored but
  not adopted. **The two copies have diverged** and are kept as-is rather than
  silently reconciled: `adc_ad7715.c` in this directory is the adapted AVR
  version; `ad7715/adc_ad7715.c` is a longer variant. Neither is compiled.
- **`samples.c`** — sampling experiments.
- **`lcd.c`** — 8051/Keil-style code (`P3 = 1; Call writecom(0x30);`) pasted from
  an LCD datasheet example. Not valid AVR C; would not compile. Dead.

`0-README.txt` is the original 2013 note, kept for provenance.
