# ForgeDiagnostics OBD-II Prototype

A runnable first prototype: read fault codes through an OBD-II adapter and get
plain-English explanations. Simulator-first — it works end to end with no
hardware and no API key.

## Quick start (simulator — no hardware needed)

```bash
cd forge-obd-prototype
python3 -m forge_obd.cli diagnose --simulator
```

You should see a JSON report explaining P0300 and P0171, clearly labeled
`SIMULATOR`.

## With a real ELM327 adapter

```bash
pip install obd
python3 -m forge_obd.cli diagnose --port /dev/ttyUSB0
```

(Use the right serial port for your system, e.g. `COM3` on Windows.)

Honest failure modes:
- **No adapter connected** → clear error, not fake data.
- **ECU doesn't answer** → "no data / timeout" error, which is *not* reported as "no faults".
- **ECU answers with no codes** → "no stored fault codes right now", not "vehicle is fault-free".

## Run the tests

```bash
python3 -m unittest discover -s tests -v
```

## Plain-English layer

Built-in explanations cover common codes (P0300, P0171/P0174, P0420/P0430,
P0442) and a generic fallback for anything else — the fallback says it has no
write-up rather than inventing one. Set `FORGE_OBD_LLM_KEY` to enable the
(pluggable) LLM enrichment hook; without it, the built-in layer is used and
the report says so.

Safety: the output explains codes and suggests checks. It never claims a
repair is safe or guaranteed.

## Privacy

Vehicle data (VIN, telemetry) stays on the machine running the tool. Nothing
is uploaded anywhere.
