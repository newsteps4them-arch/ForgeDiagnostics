"""User consent for Claude explanations.

Nothing is ever sent to Claude until the user has said yes. The rule in
this module is the whole privacy story for the explanation feature:

1. BEFORE THE FIRST CALL: the user sees DISCLOSURE — it names Claude
   (run by Anthropic), lists exactly what would be shared, and offers a
   real "no". The diagnosis flow asks interactively (see cli.py); a
   non-interactive run never silently opts in.
2. DECLINE IS REAL: saying no keeps the app fully usable — every code
   still gets its built-in explanation, and no call is attempted. The
   choice is remembered so the user is not nagged on every run.
3. OVERRIDES for scripts and tests: the FORGE_OBD_LLM_CONSENT env var
   ("granted" / "declined") wins over the saved choice, and a per-run
   override (set by the CLI's --no-llm flag) wins over both.

The saved choice lives in a small JSON file (default
~/.forge_obd_consent.json, overridable with FORGE_OBD_CONSENT_FILE). It
stores only the choice itself — never any vehicle or personal data.
"""
from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Optional

DISCLOSURE = (
    "Optional: plainer explanations from Claude\n"
    "\n"
    "ForgeDiagnostics can rewrite its built-in fault-code explanations in\n"
    "plainer English using Claude, an AI service run by Anthropic.\n"
    "\n"
    "If you allow it, then for each fault code the app sends to Claude:\n"
    "  - the fault code itself (for example P0300),\n"
    "  - whether the reading came from a real vehicle or the simulator,\n"
    "  - the app's built-in explanation text for that code.\n"
    "Nothing else is sent — no VIN, no name, no location, no vehicle\n"
    "history, no other readings.\n"
    "\n"
    "If you say no, nothing is sent anywhere and the app uses its built-in\n"
    "explanations instead. Every feature works the same either way, and\n"
    "you can change your mind later by deleting the saved choice in\n"
    "~/.forge_obd_consent.json."
)

ENV_VAR = "FORGE_OBD_LLM_CONSENT"
CONSENT_FILE_ENV = "FORGE_OBD_CONSENT_FILE"

_GRANTED_WORDS = {"granted", "grant", "yes", "y", "true", "1", "allow"}
_DECLINED_WORDS = {"declined", "decline", "no", "n", "false", "0", "deny"}

_run_override: Optional[str] = None


def set_run_override(status: Optional[str]) -> None:
    """Per-run override (e.g. the CLI's --no-llm). Wins over everything."""
    global _run_override
    _run_override = status


def _parse_word(raw: Optional[str]) -> Optional[str]:
    if raw is None:
        return None
    word = raw.strip().lower()
    if word in _GRANTED_WORDS:
        return "granted"
    if word in _DECLINED_WORDS:
        return "declined"
    return None


def consent_file_path() -> Path:
    override = os.environ.get(CONSENT_FILE_ENV)
    if override:
        return Path(override)
    return Path.home() / ".forge_obd_consent.json"


def _saved_consent() -> Optional[str]:
    try:
        data = json.loads(consent_file_path().read_text(encoding="utf-8"))
    except Exception:
        return None
    if isinstance(data, dict):
        return _parse_word(str(data.get("claude_explanations", "")))
    return None


def get_consent() -> str:
    """Return "granted", "declined", or "unknown" (never asked / no record).

    Only "granted" allows a Claude call. "unknown" behaves exactly like
    "declined" for sending — nothing goes out — but the wording shown to
    the user differs (permission not yet asked vs. permission refused).
    """
    if _run_override is not None:
        return _run_override
    env_choice = _parse_word(os.environ.get(ENV_VAR))
    if env_choice is not None:
        return env_choice
    return _saved_consent() or "unknown"


def record_consent(granted: bool) -> str:
    """Save the user's choice and return the recorded status."""
    status = "granted" if granted else "declined"
    path = consent_file_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps({"claude_explanations": status}, indent=2) + "\n",
        encoding="utf-8",
    )
    return status
