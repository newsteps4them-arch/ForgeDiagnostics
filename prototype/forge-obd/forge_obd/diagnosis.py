"""Plain-English diagnosis layer.

Built-in explanations cover common codes and work with no API key. An
optional LLM key (env FORGE_OBD_LLM_KEY) can enrich wording, but the
built-in layer is always the fallback and the output always says which
layer produced it.

Safety rule: this layer explains codes and suggests checks. It never
claims a repair is safe or guaranteed.
"""
from __future__ import annotations

import os
from typing import Dict, List

from .adapter import FaultReading

DISCLAIMER = (
    "This is an explanation of what the code means and what to check — "
    "not a repair instruction. No repair is guaranteed safe; when in doubt, "
    "have a qualified technician inspect the vehicle."
)

BUILTIN_EXPLANATIONS: Dict[str, str] = {
    "P0300": (
        "Random or multiple cylinders misfiring. Common causes: worn spark plugs, "
        "a weak ignition coil, a vacuum leak, or low fuel pressure. "
        "Start with the plugs and coils. Do not keep driving with a flashing "
        "check-engine light — that can overheat and destroy the catalytic converter."
    ),
    "P0171": (
        "Engine running too lean on bank 1 (too much air or too little fuel). "
        "Usual suspects: a vacuum or intake leak, a dirty mass-airflow sensor, "
        "or a weak fuel pump. Check for hissing leaks and inspect the air intake first."
    ),
    "P0174": (
        "Engine running too lean on bank 2. Same family of causes as P0171 — "
        "look for intake/vacuum leaks affecting that bank."
    ),
    "P0420": (
        "Catalytic converter efficiency below threshold on bank 1. Often a worn "
        "converter, but rule out exhaust leaks and lazy oxygen sensors first — "
        "they can fake this code."
    ),
    "P0430": (
        "Catalytic converter efficiency below threshold on bank 2. Same advice as "
        "P0420: verify exhaust integrity and oxygen sensors before condemning the converter."
    ),
    "P0442": (
        "Small evaporative-emissions leak detected. The classic cause is a loose "
        "or cracked fuel cap — check that first. Otherwise look for cracked EVAP hoses."
    ),
}


def _generic_explanation(code: str) -> str:
    family = {
        "P0": "powertrain",
        "P1": "powertrain (manufacturer-specific)",
        "P2": "powertrain",
        "P3": "powertrain",
        "C": "chassis",
        "B": "body",
        "U": "network/communications",
    }
    kind = family.get(code[:2], family.get(code[:1], "unknown system"))
    return (
        f"{code} is a {kind} fault code with no built-in write-up yet. "
        "Look up the exact code definition for this vehicle, check any freeze-frame "
        "data, and inspect the indicated system before replacing parts."
    )


def explain_code(code: str) -> Dict[str, str]:
    """Explain one code. Always honest about which layer produced the text."""
    code = code.strip().upper()
    llm_key = os.environ.get("FORGE_OBD_LLM_KEY")
    if llm_key:
        # Pluggable enrichment point: with a key set, a provider call would go
        # here. The built-in text remains the grounded fallback either way.
        return {
            "code": code,
            "explanation": BUILTIN_EXPLANATIONS.get(code, _generic_explanation(code)),
            "layer": "builtin+llm-key-present",
            "note": "LLM enrichment hook is stubbed in this prototype; built-in explanation shown.",
        }
    return {
        "code": code,
        "explanation": BUILTIN_EXPLANATIONS.get(code, _generic_explanation(code)),
        "layer": "builtin",
    }


def diagnose_codes(readings: List[FaultReading]) -> Dict:
    """Turn fault readings into a plain-English diagnosis report."""
    if not readings:
        return {
            "source": "vehicle",
            "fault_count": 0,
            "verdict": (
                "The ECU answered and reported no stored fault codes. "
                "That means no codes right now — not that the vehicle is fault-free."
            ),
            "results": [],
            "disclaimer": DISCLAIMER,
        }
    sources = {r.source for r in readings}
    return {
        "source": "simulator" if sources == {"simulator"} else "vehicle",
        "source_detail": (
            "SIMULATOR — these codes are a scripted scenario, not from a vehicle."
            if sources == {"simulator"}
            else "Read from a live vehicle adapter."
        ),
        "fault_count": len(readings),
        "results": [
            {**explain_code(r.code), "provenance": r.source} for r in readings
        ],
        "disclaimer": DISCLAIMER,
    }
