# Ops — launchd and Tailscale

The server runs as a macOS LaunchAgent and is reachable only from the tailnet. This
directory holds the LaunchAgent template and a single operator script.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/wifesignal/ops)**

## The operator script

```
wife-signal up | down | restart | status | logs | ack
```

| Command | What it does |
|---|---|
| `up` | `launchctl bootstrap` (or `kickstart -k` if already loaded), polls `/health` 20× at 0.5 s, then `tailscale serve --bg --yes 8787` and verifies the tailnet health URL |
| `down` | `tailscale serve reset` + `launchctl bootout` |
| `restart` | `down` then `up` — can hit a launchd race immediately after `down`; wait ~2 s and run `up` again |
| `status` | Local health JSON, LaunchAgent state/pid/last exit code, and `tailscale serve status` |
| `logs` | Tails both log files |
| `ack` | Acknowledges the current signal from the command line — the CLI equivalent of pressing a button |

`up` verifies through the *public* tailnet URL rather than just `127.0.0.1`, so a
working server behind broken TLS termination is reported as broken rather than as
healthy.

## The LaunchAgent

Published as [`com.ariemeir.wife-signal.plist.example`](com.ariemeir.wife-signal.plist.example)
rather than as an installable file, because the paths must be substituted first:

| Placeholder | Replace with |
|---|---|
| `__PROJECT_DIR__` | The absolute path of the project directory |
| `__HOME__` | The absolute path of the user's home directory |

```bash
sed -e "s|__PROJECT_DIR__|$PWD|g" -e "s|__HOME__|$HOME|g" \
    ops/com.ariemeir.wife-signal.plist.example \
    > ~/Library/LaunchAgents/com.ariemeir.wife-signal.plist
ops/wife-signal up
```

Key settings: `RunAtLoad`, `KeepAlive` only on unsuccessful exit (so a clean
shutdown stays down), `ProcessType Background`, and a 10 s `ThrottleInterval`. Logs
land in `~/.wife-signal/logs/`.

### Absolute paths are a real hazard

Those placeholders exist because of a bug worth recording. The project directory was
renamed once, and the LaunchAgent — which had the old path baked in — **stopped
working without a single log line**. launchd simply declined to start a job whose
program did not exist, and the failure surfaced days later as "the light stopped
responding".

The published script derives its own location rather than hardcoding one, which is
what should have been true from the start.

## Why `serve` and not `funnel`

`tailscale serve` exposes the API to devices on the tailnet only, over a real Let's
Encrypt certificate. It is never reachable from the public internet. The second
phone joins through **node sharing** — its own account, given access to this one
host — so it resolves the server and sees nothing else on the network.

The layering is deliberate: the bearer token is the inner layer, the tailnet ACL is
the wall. Moving to `funnel` would invert that and would demand a genuinely strong
token, since the API would then be publicly reachable.

## Bluetooth permission, one more time

The launchd-run Python needs its **own** macOS Bluetooth grant — it does not inherit
the one Terminal has. Without it, BLE scans return empty with no error at all.

Run `server/run.sh` from a terminal once before installing the LaunchAgent so the
permission prompt appears while there is a human present to answer it. Note that the
grant attaches to the interpreter's path, so a Homebrew Python upgrade silently
revokes it and the symptom is, again, a lamp that stops responding.
