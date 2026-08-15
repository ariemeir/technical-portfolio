# Documentation

| Document | What it is |
|---|---|
| [`3dscanner-case-study-arie-meir.pdf`](3dscanner-case-study-arie-meir.pdf) | One-page case study — the problem, the pipeline, the engineering principles |
| [`scannercam_spec.md`](scannercam_spec.md) | The ScannerCam technical specification — 1,731 lines, v0.2 |
| [`engineering-log.md`](engineering-log.md) | Decisions, bugs with root causes, and measured results |

## The spec

Written before the iPhone app, and followed. It covers the project and file model,
capture lifecycle, network and authentication model, all thirteen endpoints with
request/response shapes, concurrency and data-integrity requirements, storage
protection, acceptance criteria, and an explicit out-of-scope list.

Its final section, **Revision notes (v0.1 → v0.2)**, is the part worth reading first.
It records what a technical review of the original draft changed and why — including
the filename scheme that broke on overwrite, an underspecified HTTP scope that would
have hung clients on keep-alive, an unaddressed delete-versus-download race, and four
acceptance criteria that the original twenty never exercised.

Deployment specifics in §0 (signing team, device identifiers, network addresses) have
been replaced with placeholders for publication — see [`../NOTICE.md`](../NOTICE.md).

## The engineering log

Adapted from the project's working handoff document. It keeps the reasoning rather
than just the outcomes: why COLMAP was evaluated and removed, why a capture blocks
its own request thread, what actually caused the server to wedge, why infrared
transmitted but nothing decoded it, and which subjects reconstruct cleanly versus
which quietly fail.

It ends with corrections to claims the original notes made confidently and got wrong.
