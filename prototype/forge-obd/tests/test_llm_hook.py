"""Tests for the Claude explanation hook (forge_obd/llm.py) and its wiring.

No real API key and no network: the LLM transport is replaced with fakes,
and the adapter tests install a fake `obd` module. Run:
    python -m unittest discover -s tests -v
"""
import io
import os
import sys
import types
import unittest
import urllib.error
from contextlib import redirect_stderr
from unittest import mock

from forge_obd import llm
from forge_obd.adapter import ObdAdapter, read_fault_codes
from forge_obd.cli import main as cli_main
from forge_obd.diagnosis import diagnose_codes, explain_code


def _ok_transport(url, headers, payload, timeout):
    return 200, {
        "model": "claude-sonnet-5-5",
        "content": [{
            "type": "text",
            "text": "Your engine is misfiring — one or more cylinders are "
                    "not firing properly. Check the spark plugs first.",
        }],
        "usage": {"input_tokens": 1500, "output_tokens": 60},
    }


class TestLlmHook(unittest.TestCase):
    def setUp(self):
        self._saved_transport = llm._TRANSPORT
        self._saved_env = os.environ.get("FORGE_OBD_LLM_KEY")
        self._saved_consent = os.environ.get("FORGE_OBD_LLM_CONSENT")
        os.environ["FORGE_OBD_LLM_KEY"] = "test-key-not-real"
        # These tests exercise the call path; consent is granted so the
        # gate in diagnosis.explain_code lets them through. The gate
        # itself is covered by tests/test_consent.py.
        os.environ["FORGE_OBD_LLM_CONSENT"] = "granted"

    def tearDown(self):
        llm._TRANSPORT = self._saved_transport
        if self._saved_env is None:
            os.environ.pop("FORGE_OBD_LLM_KEY", None)
        else:
            os.environ["FORGE_OBD_LLM_KEY"] = self._saved_env
        if self._saved_consent is None:
            os.environ.pop("FORGE_OBD_LLM_CONSENT", None)
        else:
            os.environ["FORGE_OBD_LLM_CONSENT"] = self._saved_consent

    def test_success_returns_plain_explanation_and_costs_it(self):
        llm._TRANSPORT = _ok_transport
        result = explain_code("P0300", source="simulator")
        self.assertEqual(result["layer"], "builtin+llm")
        self.assertIn("misfiring", result["explanation"])
        self.assertEqual(result["llm"]["status"], "ok")
        # 1500 in @ $2/M + 60 out @ $10/M = $0.0036, under the cap.
        self.assertAlmostEqual(result["llm"]["cost_usd"], 0.0036, places=6)
        self.assertLessEqual(result["llm"]["cost_usd"], llm.MAX_CALL_COST_USD)
        self.assertEqual(result["llm"]["cost_cap_usd"], llm.MAX_CALL_COST_USD)

    def test_one_call_only_no_retry(self):
        calls = []

        def counting_transport(url, headers, payload, timeout):
            calls.append(1)
            raise urllib.error.URLError("connection refused")

        llm._TRANSPORT = counting_transport
        explain_code("P0300")
        self.assertEqual(len(calls), 1, "hook must never retry / double-bill")

    def test_model_unreachable_falls_back_and_names_model_side(self):
        def dead_transport(url, headers, payload, timeout):
            raise urllib.error.URLError("network unreachable")

        llm._TRANSPORT = dead_transport
        result = explain_code("P0300")
        self.assertEqual(result["layer"], "builtin")
        self.assertIn("misfir", result["explanation"].lower())  # built-in text
        self.assertEqual(result["llm"]["status"], "unavailable")
        self.assertIn("could not be reached", result["llm"]["detail"])
        self.assertIn("nothing was invented", result["llm"]["detail"])

    def test_bad_key_falls_back_and_says_so(self):
        llm._TRANSPORT = lambda url, h, p, t: (401, {"error": {"type": "authentication_error"}})
        result = explain_code("P0171")
        self.assertEqual(result["layer"], "builtin")
        self.assertEqual(result["llm"]["status"], "unavailable")
        self.assertIn("rejected the API key", result["llm"]["detail"])

    def test_empty_model_reply_falls_back(self):
        llm._TRANSPORT = lambda url, h, p, t: (
            200, {"content": [], "usage": {"input_tokens": 10, "output_tokens": 0}})
        result = explain_code("P0171")
        self.assertEqual(result["llm"]["status"], "unavailable")
        self.assertIn("empty explanation", result["llm"]["detail"])

    def test_cost_guard_blocks_call_before_it_is_made(self):
        calls = []

        def counting_transport(url, headers, payload, timeout):
            calls.append(1)
            return _ok_transport(url, headers, payload, timeout)

        llm._TRANSPORT = counting_transport
        with mock.patch.object(llm, "MAX_CALL_COST_USD", 0.0001):
            with self.assertRaises(llm.LlmUnavailable) as ctx:
                llm.explain_with_claude("P0300", "x" * 500, "vehicle")
            self.assertIn("cost guard", str(ctx.exception))
        self.assertEqual(calls, [], "cost guard must stop the call entirely")

    def test_no_key_reports_disabled_and_never_calls(self):
        calls = []
        llm._TRANSPORT = lambda *a: calls.append(1) or _ok_transport(*a)
        os.environ.pop("FORGE_OBD_LLM_KEY", None)
        result = explain_code("P0300")
        self.assertEqual(result["layer"], "builtin")
        self.assertEqual(result["llm"]["status"], "disabled")
        self.assertIn("FORGE_OBD_LLM_KEY is not set", result["llm"]["detail"])
        self.assertEqual(calls, [])

    def test_report_summary_reflects_llm_state(self):
        llm._TRANSPORT = _ok_transport
        report = diagnose_codes(read_fault_codes(use_simulator=True))
        self.assertEqual(report["llm"]["status"], "ok")
        self.assertGreater(report["llm"]["total_cost_usd"], 0)
        self.assertLessEqual(report["llm"]["total_cost_usd"],
                             2 * llm.MAX_CALL_COST_USD)
        for r in report["results"]:
            self.assertEqual(r["provenance"], "simulator")


class TestAdapterFallback(unittest.TestCase):
    """The adapter side failing must be named as the adapter side."""

    def _fake_obd(self, connected):
        fake = types.ModuleType("obd")

        class FakeConnection:
            def is_connected(self):
                return connected

            def query(self, cmd):
                raise AssertionError("must not query when not connected")

            def close(self):
                pass

        fake.OBD = lambda **kwargs: FakeConnection()
        fake.commands = types.SimpleNamespace(GET_DTC="GET_DTC")
        return fake

    def test_adapter_not_connected_is_an_adapter_error(self):
        with mock.patch.dict(sys.modules, {"obd": self._fake_obd(False)}):
            with self.assertRaises(RuntimeError) as ctx:
                ObdAdapter(port="/dev/ttyFAKE0").get_fault_codes()
        self.assertIn("No ELM327 adapter found", str(ctx.exception))

    def test_cli_reports_adapter_failure_and_exits_2(self):
        with mock.patch.dict(sys.modules, {"obd": self._fake_obd(False)}):
            err = io.StringIO()
            with redirect_stderr(err):
                code = cli_main(["diagnose", "--port", "/dev/ttyFAKE0"])
        self.assertEqual(code, 2)
        self.assertIn("No ELM327 adapter found", err.getvalue())


if __name__ == "__main__":
    unittest.main()
