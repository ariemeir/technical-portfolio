# Technical Portfolio — Arie Meir

Representative projects, published for reference.

## Projects

### [RoboImplant](roboimplant/) — non-invasive spinal implant adjustment system

A motorized orthopedic implant that lengthens a spinal distraction rod without
surgery, for treating early-onset scoliosis in growing children. Full-stack
across four disciplines:

- **Embedded** — AVR C firmware on an ATmega1284P: real-time motor control via a
  DS1267 digital pot into a maxon ESCON servo controller, interrupt-driven
  rotation counting, and current-threshold coupling detection as the primary
  safety interlock.
- **Mobile** — Android clinician console over Bluetooth SPP, with a custom
  line-oriented ASCII protocol, live telemetry, 3D visualization, and patient
  records.
- **Electronics** — EAGLE schematic and board layout for the controller,
  including the analog current/voltage sensing chain.
- **Mechanical** — SolidWorks enclosure for the handheld driver unit.

Research prototype, ~2012–2013. Not a cleared medical device.
See [`roboimplant/NOTICE.md`](roboimplant/NOTICE.md) for rights and third-party
attribution.

---

Each project directory carries its own README and rights notice. Unless stated
otherwise in a project's `NOTICE.md`, this work is published for reference only
and is not offered under an open-source license.
