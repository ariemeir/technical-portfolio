# Configuration

[`scanner.example.yaml`](scanner.example.yaml) is the template. Copy it to
`scanner.yaml` — which is excluded from version control, because it identifies
specific hardware and a specific network — and fill in the real values.

```bash
cp config/scanner.example.yaml config/scanner.yaml
```

The API token is **never** stored in either file. The config names an environment
variable (`token_env`, default `SCANNERCAM_TOKEN`) and the controller reads the token
from there at runtime, failing with a pointer to the app's Settings screen if it is
missing.

## Groups

| Group | Controls |
|---|---|
| `camera` | Phone base URL, timeouts, and `require_locks` — whether a scan may proceed with focus/exposure/white balance unlocked |
| `turntable` | Driver (`arduino_ir` or `noop`), serial port, and the paths to the firmware directory that owns the IR codes |
| `turntable.movement` | The timing model — see below |
| `capture` | Degrees per frame, settle times, retry counts, whether to verify SHA-256 |
| `session` | Directory roots for in-progress, completed, and failed scans; package format |

## The timing model

Rotation is open loop. There is no encoder and no feedback, so a step is a timed
pulse:

```
run_seconds = degrees / degrees_per_second
            − toggle_command_latency_seconds
            − stop_coast_seconds
```

floored at `minimum_run_seconds`.

`degrees_per_second` is measured by timing one full revolution at the speed the table
will actually run at — 13.3 °/s in the reference setup, from a 27-second revolution
at the slowest setting. The latency and coast terms compensate for the round trip to
the Arduino and the motor's deceleration after the stop toggle.

This is approximate by construction. The seam will not land exactly at 360°, and that
is fine: the reconstruction stage recovers true camera pose from image features, so
nominal angles only need to be even enough to give good coverage.

An optional calibration profile at `calibration/turntable/default.yaml` can override
`degrees_per_second` and the latency/coast terms, or supply exact per-step
`run_seconds` values that win outright. **The hook is implemented; no profile has
been measured**, so the nominal figure is what runs.

`degrees_per_frame` must divide evenly into 360.
