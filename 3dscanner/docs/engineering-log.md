# Engineering Log

Decisions, bugs, and measurements recorded while building the scanner, adapted from
the project's working handoff document. Kept because the reasoning is often more
useful than the result — particularly the failures, which are the parts that
generalize.

Ordered roughly by subsystem rather than chronologically.

---

## Design decisions and why

### Filenames do not encode angle

The original design named files `frame_000034_angle_170.000.jpg`. This broke on
overwrite: re-capturing a frame at a corrected angle produced a *different*
filename than the one it was meant to replace, so "atomically replace the old file"
had no well-defined target and each changed-angle overwrite orphaned a JPEG.

Names are now `frame_%06d.jpg` — frame number only, the one value that never
changes. Angle lives in the manifest, and reaches clients through the
`Content-Disposition` header on download.

The general lesson: never encode a mutable attribute into an identifier that
something else relies on being stable.

### The HTTP server has no persistent connections

Every response carries `Connection: close`. There is no keep-alive and no chunked
transfer encoding, and `Content-Length` is always accurate.

This was a deliberate scope cut, taken to keep a from-scratch HTTP/1.1
implementation tractable, and it is documented in the spec rather than left for a
client author to discover. Clients — including the Python controller and any manual
`curl` — must not assume connection reuse.

### A capture blocks the request thread, on purpose

`POST /captures` holds the connection's handler for the duration of the exposure,
using a `DispatchSemaphore` to bridge AVFoundation's async completion callback into
the synchronous route handler.

This is intentional. Only one capture can physically run at a time regardless, and
the server has no concurrent connection model to surrender. It does mean a request
arriving mid-capture waits ~0.5–1 s. The semaphore wait is bounded at 25 s so a
wedged camera cannot block forever.

### Swift language mode 5, not 6

Pinned in `project.yml`, deliberately. The `NWListener`-based server and the
AVFoundation delegate patterns predate proper actor-isolation and `Sendable`
annotations, and doing that migration correctly was out of scope. The toolchain is
current; only strict-concurrency checking is relaxed.

### The controller never duplicates IR codes

`capture/controller/turntable/arduino_ir.py` loads
`firmware/turntable_ir/turntable.py` by path at runtime and reuses its gateway and
code table, rather than copying command values into the controller. It fails fast if
`START_PAUSE` is missing from the table. The firmware directory stays the single
source of truth for anything infrared.

### The idle timer is disabled while the server runs

A real bug, found in testing. The app is foreground-only by design, but nothing
stopped iOS from auto-locking the screen after a minute or two of inactivity, which
silently killed the server mid-session. Setting `isIdleTimerDisabled` while the
server is running fixed it.

A manual power-button lock still suspends the app. That is expected and documented
behaviour, not a bug.

---

## Bugs worth recording

### The server wedged completely, including `/health`

**Symptom.** During the first real controller run the entire server hung. Every
endpoint accepted the TCP connection and then never responded. Force-quit required.

**Wrong hypothesis.** The obvious suspect was the project routes, since those were
what the controller had just started exercising.

**Actual cause.** `HTTPServer` ran the `NWListener`, every connection's I/O, *and*
`router.handle` on **one shared serial `DispatchQueue`**. Several handlers block
that queue synchronously — the capture route's `semaphore.wait()`, the status
route's `sessionQueue.sync`. One blocked handler therefore starved the listener
itself, so nothing could be served, including endpoints that touch no shared state.

**Fix.** Each accepted connection now gets its own serial queue, so a blocked
request affects only itself. The capture semaphore also became bounded.

**Verification.** A 3-frame scan while hammering the previously-hung endpoints and
pinging `/health` five times a second: all endpoints 30–50 ms, zero failures.

### The image-list client looped forever

Surfaced while chasing the wedge, and unrelated to it. The Python client's
`list_images` assumed `offset`/`total` pagination; the API actually uses
`after_frame`/`has_more`. The cursor never advanced, so it re-fetched page one
indefinitely.

Fixed, and pinned with a regression test that walks a real two-page response. The
client also gained a defensive no-progress break.

### Infrared transmitted but nothing decoded it

The IR LED visibly flashed — confirmed on a phone camera — yet the turntable
ignored every command.

IRremote drives the sender's 38 kHz carrier PWM *and* the receiver's 50 µs sampling
interrupt from **the same hardware timer (Timer2)** on an Uno. With the receiver
active, its ISR steals cycles from the transmit path and the mark/space timing comes
out malformed: a signal is emitted, but not a valid NEC frame.

Every transmit is now wrapped in receiver-disable / receiver-enable. The minimal
production gateway sketch has no receiver at all and so avoids the problem entirely.

### Object Capture rejected the OBJ output path

`PhotogrammetrySession` returned `invalidOutput` at submit time for OBJ export.

RealityKit writes OBJ as a *bundle* — a directory containing `baked_mesh_<hash>.obj`,
its `.mtl`, and textures — so it requires a directory URL. Subtly, the URL must also
carry `isDirectory: true`. Building `URL(fileURLWithPath: "foo.obj")` and calling
`.deletingPathExtension()` yields a URL flagged as a *file*, which is rejected.

Two consequences, both fixed: the directory URL is now constructed explicitly, and
the shell pipeline globs for `baked_mesh_*.obj` inside the bundle instead of
expecting `<id>.obj`.

### Re-running a reconstruction failed

RealityKit's `modelFile` request refuses to overwrite an existing output and aborts
at submit. Re-running a session at a different detail level — `reduced` first for a
sanity check, then `full` — therefore always failed the second time. The pipeline
now removes prior outputs for that session ID before running.

### Two failed authentications caused by the UI, not the auth code

The Settings screen originally displayed the 43-character random API token as plain,
unselectable text. Hand-transcribing it failed twice — once with an extra character,
once missing a hyphen — and both times presented as a `401` that looked like an auth
bug.

Fixed with selectable text plus a copy button, and later a custom-token field so a
memorable value can be set instead. Worth remembering as a debugging heuristic:
suspect a mis-copied credential before suspecting the credential-checking code.

---

## Platform evaluation: COLMAP

COLMAP was the initial choice for reconstruction and was **removed after testing**.

Its dense stereo stage (`patch_match_stereo`) is CUDA-only and hard-errors on Apple
Silicon with an explicit "Dense stereo reconstruction requires CUDA". On this
hardware it could therefore only ever produce a sparse point cloud — never a mesh —
which is not a partial result but a dead end for this use case.

Replaced with Apple Object Capture, which is GPU-accelerated on M-series, purpose
built for object and turntable capture, and outputs a near-watertight *textured*
mesh directly.

**The decision reverses** only if dense reconstruction moves to an NVIDIA/Linux
machine, at which point COLMAP plus OpenMVS becomes worth revisiting.

---

## Operating the rig

### The turntable must be armed before a run

The controller only ever sends `START_PAUSE`. Everything else — direction, speed,
continuous mode — is set once by the operator over IR beforehand:

```
irctl.py send SPEED_DOWN SPEED_DOWN SPEED_DOWN CW ROTATE_CONTINUOUS --gap 1.0
```

**The trap:** `ROTATE_CONTINUOUS` is not a passive mode selection — it starts the
table spinning immediately. The arming sequence therefore leaves the table *running*,
and one `START_PAUSE` must be sent to stop it before starting a scan:

```
irctl.py send START_PAUSE
```

Skip that and the controller, which assumes the table starts stopped, inverts every
toggle for the whole run. Because `START_PAUSE` is an unreadable toggle, the only
confirmation available is visual. A completed run leaves the table armed and
stopped, so back-to-back scans need no re-arming.

### Step size

`--degrees` must divide evenly into 360. At the slowest speed a 5° step is roughly a
0.26 s pulse and still lands reliably; finer than that, motor spin-up and coast
begin to dominate the commanded interval and rotation becomes inconsistent.

---

## Results

### What makes a scan succeed

The rig and pipeline worked from the first run. Subject and scene choice turned out
to dominate outcomes.

| Round | Subject / scene | Frames used | Result |
|---|---|---|---|
| 1 | Glossy mug, cluttered desk with a **monitor** behind | — | hard failure |
| 2 | Glossy mug, light tent, matte backdrop | 10 / 36 | partial ~90° shell |
| 3 | **Matte textured ceramic flower**, light tent | 36 / 36 | clean full mesh |
| 4 | Same flower, 5° steps | 72 / 72 | clean; 87,394 triangles at `full` |

**A feature-rich static background breaks the solve.** A monitor displaying text is
the worst case. The object rotates and the background does not, and the
reconstruction cannot reconcile two rigid scenes moving relative to each other.
Anything sitting *on* the turntable disc — an alignment arrow, a ruler — is
harmless, because it rotates with the object and stays consistent.

**Surface matters more than resolution.** Matte, textured, asymmetric subjects
register reliably. Glossy, rotationally symmetric ones — a ribbed mug — drop frames
past roughly 90°, because every angle looks alike to the feature matcher and
specular highlights slide across the surface rather than staying fixed to it.

### Detail levels — 144 images at 48 MP, on an M4

| Level | Time | Triangles |
|---|---|---|
| `reduced` | 6 m 20 s | 25 k |
| `full` | 6 m 52 s | 100 k |
| `raw` | 10 m 56 s | 212 k |

**`reduced` is only 32 seconds faster than `full`.** Roughly 90% of runtime is
ingesting and solving the images, which is identical at every level; only the final
mesh and texture generation differs. `reduced` therefore buys a lighter file, not a
faster run — and `raw` is the only level that adds genuinely new geometry, at about
2.1× `full`. USDZ file size tracks texture resolution, not triangle count.

### Multiple camera elevations

One turntable ring is one camera elevation, and it never sees the top or underside
well. Additional rings are shot by tilting the *camera* between full 360° runs — the
object must not move. All rings then reconstruct together in a single session, and
Object Capture fuses the poses.

A mid ring (72 frames) plus a low "under" ring (72 frames) fused **128 of 144**
frames. The under ring alone was poor — 16 near-edge-on frames dropped, since a flat
disc is ambiguous viewed edge-on — but combined, it **corrected the object's
thickness**: the mid ring had over-rounded an edge it could never observe, and the
under ring's edge-on views pinned the true thin profile.

A top-down ring added little for a flat object, being largely redundant with the mid
ring. More rings help genuinely three-dimensional subjects. Fine surface detail comes
from `raw` detail and filling the frame, not from additional rings.

---

## Print preparation

`repair.py` (PyMeshLab) turns a reconstruction into a printable solid: weld → drop
stray components → repair non-manifold geometry → close holes → orient the flat
face's normal to +Z by PCA → scale the widest in-plane extent to a given millimetre
figure → Taubin-smooth only the bottom band → seat on Z=0.

```
repair.py in.stl out.stl --normal-to-z --diameter-mm 100 --smooth-base 3.5
```

produced a watertight 100 mm medallion, 17.4 mm thick, with its flat face
perpendicular to Z.

Object Capture's output is already *nearly* watertight — the `full`/`raw` flower had
a single 20-edge hole. The fabricated base is closed topologically but geometrically
crude, having never been photographed, which is what `--smooth-base` addresses.

### The drilling order trap

Punching see-through holes is done interactively in Blender, using a boolean
difference. The ordering matters and is easy to get backwards:

**Close-holes fills every open boundary — including the holes you just drilled.**
Running the repair pass after drilling silently undoes the drilling.

So: repair first, to a clean watertight solid; drill second. A boolean difference
through a closed solid yields clean tunnels that preserve watertightness. Drilling a
messy mesh leaves open boundaries, and an exact boolean against self-intersecting
geometry can collapse the mesh entirely.

Blender 5.x renamed the boolean solvers: `Float` (formerly "Fast"), `Exact`, and the
new **`Manifold`**, which is the right choice for watertight input. If the whole mesh
vanishes on a difference operation, the usual causes are flipped normals on the
cutter or target, an unapplied cutter scale, or a self-intersecting joined cutter.

---

## Downstream

The printed part is a master for a one-part open-pour silicone block mould — a
flat-backed medallion being the simplest case — cast in gypsum. Casting plaster
(Hydrocal or dental stone) holds detail considerably better than craft plaster of
Paris. Through-holes make thin, fragile mould posts and are best plugged for a first
mould.

---

## Corrections to the original working notes

Recorded because the original document asserted them confidently and they turned out
to be wrong.

- **Repository location.** An earlier note stated this tree had been folded into
  another repository as an ordinary subdirectory, and that tooling assuming it was
  its own git root was therefore incorrect. That is not what happened: the project
  is its own repository, with its own remote, and the repository named in that note
  does not exist. The note was stale and is superseded by this one.
- **Bonjour advertisement.** An earlier note stated that Bonjour advertisement code
  existed and that the unresolved `saru.local` hostname was therefore likely a
  Mac-side resolver quirk. Reading the code shows no `NWListener.service` is ever
  set — the service type is declared in `Info.plist` and the status endpoint reports
  a Bonjour name, but nothing advertises. The mDNS failure has a much simpler
  explanation than the one recorded.
