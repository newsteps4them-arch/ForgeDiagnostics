"""Plain-English diagnosis layer.

Built-in explanations cover common codes and work with no API key. With a
Claude API key set (env FORGE_OBD_LLM_KEY) AND the user's consent (see
consent.py — nothing is sent before the user says yes), each code also
gets a plain-English explanation from Claude (see llm.py), cost-capped
per call.
The built-in layer is always the fallback, every failure is reported
honestly with which side failed, and the output always says which layer
produced the text.

Safety rule: this layer explains codes and suggests checks. It never
claims a repair is safe or guaranteed.
"""
from __future__ import annotations

import os
from typing import Dict, List

from . import consent, llm
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


def explain_code(code: str, source: str = "vehicle") -> Dict:
    """Explain one code. Always honest about which layer produced the text.

    With FORGE_OBD_LLM_KEY set and consent granted, Claude writes the
    explanation (cost-capped, see llm.py). Any model-side failure falls back to the built-in text and
    the result's "llm" field says exactly what failed — nothing is invented.
    With no key, the built-in text is used and "llm" reports explanations
    as disabled until the key is set.
    """
    code = code.strip().upper()
    builtin = BUILTIN_EXPLANATIONS.get(code, _generic_explanation(code))
    if not os.environ.get("FORGE_OBD_LLM_KEY"):
        return {
            "code": code,
            "explanation": builtin,
            "layer": "builtin",
            "llm": {
                "status": "disabled",
                "detail": (
                    "Plain-English explanations from Claude are off — "
                    "FORGE_OBD_LLM_KEY is not set. Built-in explanation shown."
                ),
            },
        }
    # Consent gate: the key being set is not permission. Until the user
    # has granted consent (see consent.py), nothing is sent to Claude.
    consent_status = consent.get_consent()
    if consent_status == "declined":
        return {
            "code": code,
            "explanation": builtin,
            "layer": "builtin",
            "llm": {
                "status": "declined",
                "detail": (
                    "You declined Claude explanations, so nothing was sent "
                    "to Claude. Built-in explanation shown; the app works "
                    "the same without them."
                ),
            },
        }
    if consent_status != "granted":
        return {
            "code": code,
            "explanation": builtin,
            "layer": "builtin",
            "llm": {
                "status": "consent_required",
                "detail": (
                    "Claude explanations need your permission first, so "
                    "nothing was sent to Claude. Built-in explanation "
                    "shown. Run the tool interactively to be asked, or set "
                    "FORGE_OBD_LLM_CONSENT=granted to allow them."
                ),
            },
        }
    try:
        enriched = llm.explain_with_claude(code, builtin, source)
    except llm.LlmUnavailable as exc:
        return {
            "code": code,
            "explanation": builtin,
            "layer": "builtin",
            "llm": {
                "status": "unavailable",
                "detail": (
                    f"Claude explanation unavailable: {exc}. "
                    "Built-in explanation shown; nothing was invented."
                ),
            },
        }
    return {
        "code": code,
        "explanation": enriched.text,
        "layer": "builtin+llm",
        "llm": {
            "status": "ok",
            "model": enriched.model,
            "cost_usd": round(enriched.cost_usd, 6),
            "cost_cap_usd": llm.MAX_CALL_COST_USD,
            "input_tokens": enriched.input_tokens,
            "output_tokens": enriched.output_tokens,
        },
    }


def _llm_summary(results: List[Dict]) -> Dict:
    """One honest status line for the whole report's explanation layer."""
    if not results:
        if not os.environ.get("FORGE_OBD_LLM_KEY"):
            return {
                "status": "disabled",
                "detail": (
                    "Claude explanations are disabled until FORGE_OBD_LLM_KEY "
                    "is set."
                ),
            }
        return {"status": "ok", "detail": "No fault codes to explain."}
    statuses = [r.get("llm", {}).get("status") for r in results]
    if statuses and all(s == "disabled" for s in statuses):
        return {
            "status": "disabled",
            "detail": (
                "Claude explanations are disabled until FORGE_OBD_LLM_KEY "
                "is set; built-in explanations were used."
            ),
        }
    if any(s == "ok" for s in statuses):
        total = sum(r["llm"].get("cost_usd", 0.0) for r in results)
        return {
            "status": "ok",
            "detail": "Claude wrote the explanations; built-ins grounded them.",
            "total_cost_usd": round(total, 6),
        }
    if any(s == "consent_required" for s in statuses):
        return {
            "status": "consent_required",
            "detail": (
                "Claude explanations were not used because your permission "
                "has not been given; nothing was sent to Claude and "
                "built-in explanations were used."
            ),
        }
    if statuses and all(s == "declined" for s in statuses):
        return {
            "status": "declined",
            "detail": (
                "You declined Claude explanations; nothing was sent to "
                "Claude and built-in explanations were used."
            ),
        }
    return {
        "status": "unavailable",
        "detail": (
            "The explanation model could not be reached; built-in "
            "explanations were used and no explanation was invented."
        ),
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
            "llm": _llm_summary([]),
            "disclaimer": DISCLAIMER,
        }
    sources = {r.source for r in readings}
    results = [
        {**explain_code(r.code, source=r.source), "provenance": r.source}
        for r in readings
    ]
    return {
        "source": "simulator" if sources == {"simulator"} else "vehicle",
        "source_detail": (
            "SIMULATOR — these codes are a scripted scenario, not from a vehicle."
            if sources == {"simulator"}
            else "Read from a live vehicle adapter."
        ),
        "fault_count": len(readings),
        "results": results,
        "llm": _llm_summary(results),
        "disclaimer": DISCLAIMER,
    }
