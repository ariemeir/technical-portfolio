# Turntable IR Control

The turntable is a consumer motorized photography table. It has no data port — its
only input is a 14-button infrared remote. So the Arduino **impersonates the remote**:
nothing here is wired into the turntable electrically.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/3dscanner/firmware/turntable_ir)**

## Hardware

<p align="center">
  <img src="../../docs/images/arduino-ir-emitter.jpg" width="420"
       alt="An Arduino Uno with a clear infrared LED wired through a series resistor into the digital header, connected by a blue USB cable.">
  <br>
  <em>The emitter, mid bring-up — an IR LED on a series resistor off the digital
  header. Because the LED emits outside the visible band it looks identical working or
  dead, which is what <code>ir_dc_test.ino</code> is for: it holds the pin high in a
  3 s / 1 s pattern so the emitter reads as a steady violet glow through a phone
  camera and a multimeter has a stable window to probe.</em>
</p>

| | |
|---|---|
| Board | Arduino Uno, USB serial @ 115200 |
| Library | IRremote v4.x |
| Receiver | KY-022 on **D2** — used for learning codes only |
| Emitter | IR LED on **D3** (fixed — the Uno's Timer2 output), via a 2N2222 |

```
D3 ──[1k]── base ; emitter ── GND ; collector ── LED cathode
                               LED anode ──[100R]── 5V
```

## Reverse-engineering the remote

`capture_remote_signals.ino` walks the operator through all 14 buttons, asking for
each to be pressed **three times** and taking a **majority vote** — a value must
appear at least twice to be accepted — which rejects misreads from a marginal
receiver angle. Repeat frames are filtered so one physical press yields one sample.
It prints a ready-to-paste JSON block.

The result is [`turntable_codes.json`](turntable_codes.json): all 14 buttons, all
NEC protocol, address 0, 32 bits. This file is the **single source of truth** for
infrared in this project — the scan controller loads it at runtime rather than
duplicating any command values.

Replay sends the captured 32-bit raw NEC word verbatim rather than re-encoding it
from address and command, so an unrecognized or slightly odd protocol still
round-trips correctly.

## Serial protocol

Line-oriented ASCII, `\n` terminated. Replies begin `OK`, `ERR`, `LEARNED`, or
`PONG` so the host can parse them.

| Command | Effect |
|---|---|
| `PING` | → `PONG` — used by the controller's preflight to hard-verify the board |
| `R <rawHex> [repeats]` | Send a 32-bit NEC frame as captured, e.g. `R B847FF00` |
| `N <addrHex> <cmdHex> [repeats]` | Send NEC from address + command |
| `LEARN` | Wait for one press, print `LEARNED {json}` |
| `MON` | Stream every decoded frame until a serial byte arrives |
| `S <protocol> <addr> <cmd>` | Send Sony/RC5/RC6/Samsung/JVC/Panasonic |

The Uno reboots when the serial port is opened, so both Python clients wait two
seconds after connecting before sending anything.

## ⚠️ The Timer2 conflict

IRremote drives the **sender's 38 kHz carrier PWM** and the **receiver's 50 µs
sampling interrupt** from the *same* hardware timer. If the receive ISR keeps firing
during a transmission, it steals cycles from the mark/space timing and the frame
comes out malformed — the LED still visibly flashes, but nothing decodes it.

Every transmit in `ir_blaster.ino` is therefore wrapped in receiver-disable /
receiver-enable. `ir_send_gateway.ino` has no receiver at all and sidesteps the
problem entirely, which is part of why it is the sketch used for scanning.

## Files

| File | Status |
|---|---|
| [`ir_send_gateway.ino`](ir_send_gateway.ino) | **Production.** Minimal transmit-only gateway — what the scan controller drives. |
| [`turntable_codes.json`](turntable_codes.json) | **Production data.** The captured IR code table. |
| [`turntable.py`](turntable.py) | **Production library.** Gateway abstraction (`SerialGateway` / `MockGateway`) and a `Turntable` class mirroring the physical remote. Loaded by the scan controller. |
| [`irctl.py`](irctl.py) | **Operator CLI.** Port autodetection, prefix matching, tab completion, `--dry-run`. Used to arm the table before a run and to learn new remotes. |
| [`ir_blaster.ino`](ir_blaster.ino) | **Production tooling.** Learn *and* transmit in one sketch, so adding a device never needs reflashing. |
| [`burn`](burn) | **Dev tool.** `arduino-cli` wrapper — autodetects board and FQBN, stages the sketch into a correctly named temp directory, compiles, confirms, uploads. `--check` compiles only. |
| [`capture_remote_signals.ino`](capture_remote_signals.ino) | **Bring-up tool**, superseded by `ir_blaster.ino`'s `LEARN`. Its output is already in the JSON. |
| [`ir_smoketest.ino`](ir_smoketest.ino) | **Bench test.** Fires the hardcoded `POWER` code every 2 s so you can watch the table respond. |
| [`ir_dc_test.ino`](ir_dc_test.ino) | **Hardware debugging.** No IRremote, no carrier — holds D3 high for 3 s so the LED reads as a steady glow on a phone camera and a multimeter has a stable window. Includes expected readings. |

## Arming the table before a scan

The scan controller only ever sends `START_PAUSE`. Direction, speed, and continuous
mode are set once beforehand:

```bash
irctl.py send SPEED_DOWN SPEED_DOWN SPEED_DOWN CW ROTATE_CONTINUOUS --gap 1.0
irctl.py send START_PAUSE     # ROTATE_CONTINUOUS starts it spinning — stop it first
```

`ROTATE_CONTINUOUS` is not a passive mode select: it starts rotation immediately. The
arming sequence therefore leaves the table running, and it must be stopped before a
scan begins — the controller assumes it starts stopped, and getting this wrong
inverts every toggle for the entire run.

## Why rotation is timed, not commanded

The remote does have `ANGLE_45`, `ANGLE_90`, `ANGLE_180`, and `STEP_90` buttons, and
they are captured in the code table. The scanner does not use them.

Instead the table is armed to continuous rotation at its slowest speed, and each step
is a **timed `START_PAUSE` pulse**: `run_seconds = degrees / degrees_per_second`,
minus measured command latency and coast time. This gives an arbitrary step size
rather than the four the remote offers.

It is open loop and approximate — there is no encoder and no feedback of any kind —
which is acceptable because the photogrammetry stage recovers true camera pose from
image features. Nominal angles only need to be even enough for good coverage. The
controller's handling of the resulting uncertainty is described in the
[project README](../../README.md#driving-an-actuator-you-cannot-observe).
