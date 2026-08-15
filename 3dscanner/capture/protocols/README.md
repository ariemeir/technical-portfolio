# Shared API Contract

The interface between the two halves of the capture system: the Python controller on
the Mac and the Swift camera server on the phone. Kept here, outside both, because
neither owns it.

**→ [Browse this directory on GitHub](https://github.com/ariemeir/technical-portfolio/tree/main/3dscanner/capture/protocols)**

| File | Role |
|---|---|
| [`api_v1.md`](api_v1.md) | Quick reference — endpoints, headers, error codes, examples |
| [`constants.json`](constants.json) | Machine-readable shared values, mirrored into both implementations |

The full design rationale — why each decision was made, and what a technical review
changed — lives in [`docs/scannercam_spec.md`](../../docs/scannercam_spec.md).

## Why a separate contract

Two languages implement two halves of one protocol. Port numbers, the project-ID
pattern, frame bounds, storage thresholds, and the error-code vocabulary all have to
agree exactly, and a drift between them shows up as a runtime `400` rather than a
compile error.

`constants.json` is the declared source of truth. The Swift side mirrors it in
`Models/APIModels.swift`, carrying a comment pointing back here; the Python side
reads its own copies of the same values. The mirroring is by convention, not
generated — an honest limitation, though the values do currently match.

## Conventions

| | |
|---|---|
| Base URL | `http://<phone>:8765/api/v1` |
| Encoding | `snake_case` JSON; ISO-8601 timestamps with offset |
| Auth | `Authorization: Bearer <token>` on every route except `GET /health` |
| Connections | `Connection: close` on every response — **no keep-alive, no chunked encoding** |
| Errors | `{"error": {"code": "...", "message": "...", "details": {...}}}` |

## Two contract details worth reading

**Idempotency.** `POST /captures` accepts a `request_id`; the server retains the last
1,000 and returns the original result rather than firing the shutter again. The
controller derives it deterministically as `uuid5(session_id, frame)`, so a retry of
a given frame is byte-identical.

Note that `api_v1.md` advises generating a *fresh* `request_id` per logical attempt,
which is the opposite of what the controller does. The implementation's choice is the
better one — it makes retries genuinely idempotent against physical hardware — but
the document has not been updated to match, and the two disagree.

**Cursor pagination.** `GET /projects/{id}/images` pages with `after_frame` /
`has_more` / `next_after_frame`, not `offset` / `total`. An early client assumed the
latter, never advanced the cursor, and re-fetched page one indefinitely. The contract
was always right; the client was wrong. It is now pinned by a regression test.
