# Enclosure — Parametric CAD as a Program

The enclosure is not an STL and not a saved CAD document. It is a **440-line Python
script that builds the entire two-part enclosure inside Autodesk Fusion 360**, and
it is the declared source of truth. The design is rebuilt from code, not edited by
hand.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/wifesignal/device/enclosure)**

| | |
|---|---|
| Current version | `v15`, build stamp `2026-07-27c` |
| Runs as | A Fusion 360 script (`AWifeSignalEnclosure.py` + `.manifest`) |
| Produces | Two separately printable bodies: **Body** (tray) and **Lid** |

## Why generate it

The same reasons code is kept in version control, in a domain where those benefits
are usually unavailable:

- **Dimensions are derived, not typed.** The body width is
  `BUTTON_HOLE_DIA + 2 × SIDE_MARGIN` (30 + 18 = 48 mm); the height is
  `BUTTON_SPACING × 2 + 2 × END_MARGIN` (74 + 42 = 116 mm). Changing the button
  diameter reshapes the enclosure consistently instead of leaving stale numbers in
  four other places.
- **Physical measurements are named constants.** The USB-C window's vertical extent
  is computed from the stack — floor, 2.0 mm spacers, 1.5 mm PCB, then the measured
  15.7 mm to the top of the connector and 7.5 mm to its underside — rather than
  eyeballed against a screen.
- **The geometry is locked to the PCB.** The four pillars are placed at
  (±16, ±18.5) mm, with a comment stating that they must match the board's C-notches
  exactly. Two CAD tools, one number, written down once.
- **The build is versioned in-band.** `VERSION` and `BUILD_STAMP` are printed to a
  summary dialog on completion, so the copy that actually ran can be identified. The
  script lives in two places — here, and in Fusion's scripts directory — and drift
  between them is otherwise invisible.
- **Fragile steps are individually guarded.** Fillets and countersinks are the
  features most likely to fail on a given geometry, so each is wrapped and reports
  `built | FAILED | skipped` instead of aborting the run and losing the whole
  design.

## What it builds

**Body** — the outer profile extruded −37 mm and shelled to a 3 mm wall with the
front face removed, plus a USB-C slot in the −X wall and four Ø9 mm pillars, each
with a press-fit M4 hex-nut pocket and a Ø4.5 mm bolt clearance bore. The nut pocket
corners are rounded (`HEX_CORNER_R`) so they print as a clean ring rather than as
fingers that curl inward — the change that produced build stamp `c`.

**Lid** — a 4 mm plate with three Ø30 mm button holes at y = +37 / 0 / −37 mm and
four countersunk M4 holes (Ø8.4 csk).

## ⚠️ Running it wipes the active design

`WIPE_FIRST = True`. The script clears the active Fusion document before building,
which is what makes it idempotent and what makes an accidental run destructive. Open
a scratch document first.

## v12 → v15

The visible payoff of iterating in code is a redesign that would have been painful
by hand:

| | v12 | v15 |
|---|---|---|
| Board mounting | Bare dev board held on edge in a printed cradle | Custom PCB bolted flat to four pillars |
| Tray depth | 30 mm | **37 mm** — the taller ESP32 stack needed to clear the lid |
| Cradle | Present | **Removed entirely** |

The v12 dimension sheet that documented the older design has been dropped rather
than published, since it contradicts the generator on every number that changed.
