"""Claude explanation hook for the check-engine prototype.

Turns a built-in fault-code explanation into a plainer-English one using
Anthropic's Claude (model: claude-sonnet-5-5, per
llm-provider-recommendation.md). Design rules, in order of importance:

1. HONESTY: any failure — no key, network down, timeout, bad key, rate
   limit, empty answer — raises LlmUnavailable with a plain reason. The
   caller falls back to the built-in explanation and says so. This module
   never invents an explanation.
2. COST CAP: one call per code, output capped at MAX_OUTPUT_TOKENS, and
   the estimated cost is checked against MAX_CALL_COST_USD *before* the
   call is made. No retries, ever — a retry could double-bill.
3. KEY HYGIENE: the key is read only from the FORGE_OBD_LLM_KEY
   environment variable, is sent only to api.anthropic.com, and is never
   logged, printed, or stored.

Uses only the Python standard library (urllib) so the prototype gains no
new dependency. The network transport is a module-level function so tests
can substitute a fake and exercise every path without a real key.
"""
from __future__ import annotations

import json
import math
import os
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Callable, Dict, Optional, Tuple

MODEL = "claude-sonnet-5-5"
API_URL = "https://api.anthropic.com/v1/messages"
API_VERSION = "2023-06-01"
TIMEOUT_SECONDS = 15

# Cost controls (list prices, October 2026: $2.00/M input, $10.00/M output).
INPUT_USD_PER_MTOK = 2.00
OUTPUT_USD_PER_MTOK = 10.00
MAX_OUTPUT_TOKENS = 400          # hard cap on the reply length
MAX_INPUT_CHARS = 24_000         # ~6,000 tokens of prompt, worst case
MAX_CALL_COST_USD = 0.02         # per-call ceiling (~3x the expected $0.007)

SYSTEM_PROMPT = (
    "You explain car fault codes (OBD-II diagnostic trouble codes) in plain, "
    "everyday English for the person standing next to the vehicle.\n"
    "Rules you must follow:\n"
    "- Explain what the code means, the most likely causes, and what to check "
    "first. Nothing more.\n"
    "- Plain words only. If a technical term is unavoidable, explain it in the "
    "same sentence.\n"
    "- Never claim a repair is safe, easy, or guaranteed. Never give repair "
    "instructions — explanations and checks only.\n"
    "- Stay consistent with the built-in explanation you are given; it is the "
    "ground truth. Do not add causes it does not support.\n"
    "- If the code is marked as coming from a simulator scenario, say plainly "
    "that it is a practice example, not a finding from a real vehicle.\n"
    "- Keep it under 180 words."
)


class LlmUnavailable(RuntimeError):
    """The model side failed. Carries a plain-English reason; the caller
    falls back to the built-in explanation and reports this reason."""


@dataclass
class LlmExplanation:
    text: str
    model: str
    input_tokens: int
    output_tokens: int
    cost_usd: float


# A transport takes (url, headers, payload, timeout) and returns
# (http_status, parsed_json_body). Tests replace it with a fake.
Transport = Callable[[str, Dict[str, str], Dict, float], Tuple[int, Dict]]


def _urllib_transport(url: str, headers: Dict[str, str], payload: Dict,
                      timeout: float) -> Tuple[int, Dict]:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        try:
            body = json.loads(exc.read().decode("utf-8"))
        except Exception:
            body = {}
        return exc.code, body


_TRANSPORT: Transport = _urllib_transport


def _estimate_tokens(text: str) -> int:
    return max(1, math.ceil(len(text) / 4))


def _cost_usd(input_tokens: int, output_tokens: int) -> float:
    return (input_tokens * INPUT_USD_PER_MTOK
            + output_tokens * OUTPUT_USD_PER_MTOK) / 1_000_000


def _user_prompt(code: str, builtin_text: str, source: str) -> str:
    if source == "simulator":
        origin = ("This code comes from a SIMULATOR practice scenario — it was "
                  "not read from a real vehicle.")
    else:
        origin = "This code was read from a real vehicle by an OBD-II adapter."
    return (
        f"Fault code: {code}\n"
        f"Origin: {origin}\n\n"
        f"Built-in explanation (ground truth):\n{builtin_text}\n\n"
        "Rewrite this as a plain-English explanation: what the code means, "
        "the likely causes, and what to check first."
    )


def explain_with_claude(code: str, builtin_text: str, source: str,
                        api_key: Optional[str] = None) -> LlmExplanation:
    """Ask Claude to explain one code. Raises LlmUnavailable on any failure.

    Exactly one API call is attempted — no retries. The estimated cost is
    checked against MAX_CALL_COST_USD before the call; if the estimate is
    over the cap, no call is made at all.
    """
    key = api_key if api_key is not None else os.environ.get("FORGE_OBD_LLM_KEY")
    if not key:
        raise LlmUnavailable(
            "explanations are disabled — FORGE_OBD_LLM_KEY is not set")

    user = _user_prompt(code, builtin_text, source)
    prompt_chars = len(SYSTEM_PROMPT) + len(user)
    if prompt_chars > MAX_INPUT_CHARS:
        raise LlmUnavailable(
            f"cost guard stopped the call — the prompt is too large "
            f"({prompt_chars} characters, limit {MAX_INPUT_CHARS})")

    est_input = _estimate_tokens(SYSTEM_PROMPT) + _estimate_tokens(user)
    est_cost = _cost_usd(est_input, MAX_OUTPUT_TOKENS)
    if est_cost > MAX_CALL_COST_USD:
        raise LlmUnavailable(
            f"cost guard stopped the call — estimated cost ${est_cost:.4f} "
            f"is over the per-call cap of ${MAX_CALL_COST_USD:.2f}")

    payload = {
        "model": MODEL,
        "max_tokens": MAX_OUTPUT_TOKENS,
        "system": [{
            "type": "text",
            "text": SYSTEM_PROMPT,
            "cache_control": {"type": "ephemeral"},
        }],
        "messages": [{"role": "user", "content": user}],
    }
    headers = {
        "x-api-key": key,
        "anthropic-version": API_VERSION,
        "content-type": "application/json",
    }

    try:
        status, body = _TRANSPORT(API_URL, headers, payload, TIMEOUT_SECONDS)
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        raise LlmUnavailable(
            f"the explanation model could not be reached ({exc.__class__.__name__})"
        ) from exc

    if status != 200:
        if status in (401, 403):
            reason = "the model service rejected the API key (HTTP 401/403)"
        elif status == 429:
            reason = "the model service is rate-limiting requests (HTTP 429)"
        elif status >= 500:
            reason = f"the model service had an error (HTTP {status})"
        else:
            reason = f"the model service returned HTTP {status}"
        raise LlmUnavailable(reason)

    texts = [block.get("text", "") for block in body.get("content", [])
             if isinstance(block, dict) and block.get("type") == "text"]
    text = "\n".join(t.strip() for t in texts if t.strip()).strip()
    if not text:
        raise LlmUnavailable("the model returned an empty explanation")

    usage = body.get("usage", {}) or {}
    in_tok = int(usage.get("input_tokens", est_input))
    out_tok = int(usage.get("output_tokens", 0))
    return LlmExplanation(
        text=text,
        model=body.get("model", MODEL),
        input_tokens=in_tok,
        output_tokens=out_tok,
        cost_usd=_cost_usd(in_tok, out_tok),
    )
