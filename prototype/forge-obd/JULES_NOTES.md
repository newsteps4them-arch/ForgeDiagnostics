# Jules application notes — forge-obd-prototype

Target repo: `newsteps4them-arch/ForgeDiagnostics` (Michael's ForgeDiagnostics-only
focus; the idea card's default of `adaptive-swarm` was overridden per his
explicit direction).

## What this package is

A self-contained Python prototype (`forge_obd/` + `tests/` + `README.md`).
It is a *change package for review*, not a push: nothing here has been
committed to any repo.

## Suggested placement in the repo

Option A (recommended for a first prototype): add as `tools/obd-prototype/`
or `prototype/forge-obd/` at the repo root, keeping it separate from the
Android app until the approach is validated.

Option B: if the team later wants it as a Python service, promote
`forge_obd/` to a top-level service directory with its own requirements file.

## Files

- `forge_obd/__init__.py` — public surface
- `forge_obd/adapter.py` — `ObdAdapter` (real ELM327 via python-obd) and
  `SimulatorAdapter` (explicitly labeled; every reading tagged
  `source="simulator"`). Real adapter fails honestly: no canned responses,
  null ECU response raises instead of returning fake "no faults".
- `forge_obd/diagnosis.py` — plain-English explanations. Built-in write-ups
  for common codes; generic fallback that admits it has no write-up.
  `FORGE_OBD_LLM_KEY` env var is the enrichment hook (stubbed; built-in text
  is always the grounded fallback). Output always carries a safety
  disclaimer: explanations and checks only, never a guaranteed-safe repair.
- `forge_obd/cli.py` — `python -m forge_obd.cli diagnose [--simulator] [--port PORT]`
- `tests/test_prototype.py` — unittest suite (7 tests): simulator provenance
  labeling, known-code explanations, unknown-code honesty, empty-response
  semantics, disclaimer presence.

## Verification performed

- `python3 -m unittest discover -s tests -v` — all tests pass (run 2026-10-01)
- `python3 -m forge_obd.cli diagnose --simulator` — end-to-end JSON report
  with P0300/P0171 explanations, labeled SIMULATOR

## Open decisions for Michael

1. LLM provider for the enrichment hook (prototype works fully without one).
2. Whether he owns an ELM327 adapter for live-vehicle testing
   (simulator mode covers development until then).
3. Final placement in the repo (Option A recommended).

## Constraints honored

- Read-only toward the repo: no push, no branch, no PR from this package.
- Vehicle data stays local; nothing uploaded.
- Diagnosis layer never claims a repair is safe or guaranteed.
