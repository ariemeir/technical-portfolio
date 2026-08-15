# Server — HTTP ⇄ BLE Bridge

The whole bridge is one file, `server.py`, 290 lines: an aiohttp HTTP API and a
bleak BLE central sharing a single asyncio event loop. It exists because an iPhone
cannot reliably be a BLE central for a device in another building — so a Mac that
is already three metres from the lamp takes that role permanently.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/wifesignal/server)**

## Why one file, one loop

`bleak` is asyncio-native, and so is `aiohttp`. Running both in one process on one
event loop means there are no threads, no cross-thread queues, and no locking
beyond a single `asyncio.Lock` around the state dataclass. Splitting HTTP and BLE
into separate processes would have required inventing an IPC protocol to carry a
single byte.

The BLE task's lifetime is bound to the app's by aiohttp's `cleanup_ctx`, so it
starts and cancels with the server rather than outliving it.

## State model

`desired_state` is the single source of truth: *the colour byte (1–3) iff a signal
is active and un-acknowledged, else 0.*

HTTP handlers never touch Bluetooth. They mutate `state` under the lock and set
`write_needed`. One BLE task owns the `BleakClient` and performs every write. There
is no queue — a burst of taps coalesces into one write of the latest value, which
is correct for a lamp.

## The notify classifier

Because the state characteristic is bidirectional, three different events arrive as
an identical one-byte `NOTIFY`. They are separated by comparing against
`desired_state`:

| Notify value | vs. `desired_state` | Meaning | Action |
|---|---|---|---|
| equal | — | echo of our own write | ignore |
| `0` | non-zero | physical button press | mark acknowledged + timestamp |
| anything else | mismatch | drift, or a press from idle | re-assert `desired_state` |

No timers, no correlation IDs, no sequence numbers. See the
[project README](../README.md#telling-your-own-echo-from-a-human-being) for why
this matters.

## Reconnect loop

1. Scan **by service UUID** for 15 s — macOS reports advertised names as `None`, so
   name matching would never succeed.
2. If nothing is found *and zero advertisements were seen at all*, log the
   Bluetooth-permission diagnostic (see below) — this is the TCC failure, not an
   absent device.
3. Connect with a `disconnected_callback`; subscribe to notifications.
4. **Immediately re-write `desired_state`**, so a device that rebooted or was
   reflashed mid-signal comes back showing the right colour.
5. Race `write_needed` against `disconnected` via a small `wait_first` helper that
   cancels the loser.
6. On any failure, back off exponentially 1 → 2 → 4 … → 30 s; reset to 1 s on a
   successful connect.

## The macOS Bluetooth permission trap

macOS does not error when a process lacks Bluetooth permission. It returns an empty
scan, indistinguishable from "nothing nearby". `scan_for_device` therefore counts
advertisements seen *at all* and, on zero, logs a warning naming the real cause and
the exact System Settings pane.

The launchd-run interpreter needs its own grant — it does not inherit Terminal's —
and the grant is attached to the Python binary's path, so a Homebrew Python upgrade
silently revokes it. Run `run.sh` from a terminal once before installing the
LaunchAgent, so the prompt appears at a moment when there is a human to answer it.

## camelCase on purpose

Response keys are `sentAt` / `acknowledgedAt`, not `sent_at` / `acknowledged_at`.
The iOS app decodes with a bare `JSONDecoder` and no key strategy, so snake_case
keys would decode to `nil` **without raising** — the timestamps would silently
vanish. The constraint is commented on both sides of the boundary.

## Files

| File | Purpose |
|---|---|
| [`server.py`](server.py) | **Production.** The entire bridge: API, state model, notify classifier, reconnect supervisor. |
| [`wifesignal_cli.py`](wifesignal_cli.py) | **Operator CLI.** Standalone BLE REPL — `off\|red\|yellow\|green\|read\|watch`. Stop the server first: the device accepts one central at a time. |
| [`run.sh`](run.sh) | **Dev entry.** Creates the venv, installs requirements, sources `.env`, runs in the foreground. |
| [`service.sh`](service.sh) | **launchd entry.** No pip at boot — makes the log directory, sources `.env`, `exec`s the venv interpreter. |
| [`.env.example`](.env.example) | Template. `SIGNAL_API_TOKEN` is empty; the server refuses to start without one. |

## Running it

```bash
cp .env.example .env
openssl rand -hex 32          # paste into SIGNAL_API_TOKEN
./run.sh
```

Verify:

```bash
curl http://127.0.0.1:8787/health
TOKEN='...'
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8787/v1/status
curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"color":"yellow"}' http://127.0.0.1:8787/v1/signal
```

Auth is a single shared bearer token compared with `hmac.compare_digest`. `/health`
is exempt so ops tooling can reach it before knowing whether anything else works.
The token is the inner layer only — the outer wall is that the API is bound to
`127.0.0.1` and exposed solely to the tailnet by `tailscale serve`. See
[`../ops/`](../ops/).
