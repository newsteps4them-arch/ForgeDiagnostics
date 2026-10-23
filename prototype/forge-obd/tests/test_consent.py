"""Tests for the Claude-explanation consent gate (forge_obd/consent.py).

The guarantee under test: no consent, no call. Declining — or never
being asked — must leave the app fully usable on built-in explanations,
and the report must say plainly that nothing was sent to Claude.

No real API key and no network: the LLM transport is replaced with a
counting fake. Run:
    python -m unittest discover -s tests -v
"""
import io
import json
import os
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
from unittest import mock

from forge_obd import consent, llm
from forge_obd.cli import main as cli_main
from forge_obd.diagnosis import diagnose_codes, explain_code
from forge_obd.adapter import read_fault_codes


def _ok_transport(url, headers, payload, timeout):
    return 200, {
        "model": "claude-sonnet-5-5",
        "content": [{"type": "text", "text": "Plain explanation from Claude."}],
        "usage": {"input_tokens": 100, "output_tokens": 10},
    }


class ConsentEnvMixin(unittest.TestCase):
    """Isolates consent state: temp consent file, env under test control."""

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.consent_path = Path(self._tmp.name) / "consent.json"
        self._saved = {
            k: os.environ.get(k)
            for k in ("FORGE_OBD_LLM_KEY", "FORGE_OBD_LLM_CONSENT",
                      "FORGE_OBD_CONSENT_FILE")
        }
        os.environ["FORGE_OBD_LLM_KEY"] = "test-key-not-real"
        os.environ.pop("FORGE_OBD_LLM_CONSENT", None)
        os.environ["FORGE_OBD_CONSENT_FILE"] = str(self.consent_path)
        consent.set_run_override(None)
        self._saved_transport = llm._TRANSPORT
        self.calls = []
        llm._TRANSPORT = lambda *a: self.calls.append(1) or _ok_transport(*a)

    def tearDown(self):
        llm._TRANSPORT = self._saved_transport
        consent.set_run_override(None)
        for k, v in self._saved.items():
            if v is None:
                os.environ.pop(k, None)
            else:
                os.environ[k] = v
        self._tmp.cleanup()


class TestConsentGate(ConsentEnvMixin):
    def test_unknown_consent_never_calls_and_says_so(self):
        result = explain_code("P0300", source="simulator")
        self.assertEqual(self.calls, [], "no consent on record -> no Claude call")
        self.assertEqual(result["layer"], "builtin")
        self.assertEqual(result["llm"]["status"], "consent_required")
        self.assertIn("nothing was sent to Claude", result["llm"]["detail"])
        self.assertIn("misfir", result["explanation"].lower())

    def test_declined_consent_never_calls_and_app_stays_usable(self):
        os.environ["FORGE_OBD_LLM_CONSENT"] = "declined"
        result = explain_code("P0300", source="simulator")
        self.assertEqual(self.calls, [])
        self.assertEqual(result["layer"], "builtin")
        self.assertEqual(result["llm"]["status"], "declined")
        self.assertTrue(result["explanation"])

    def test_granted_consent_allows_the_call(self):
        os.environ["FORGE_OBD_LLM_CONSENT"] = "granted"
        result = explain_code("P0300", source="simulator")
        self.assertEqual(len(self.calls), 1)
        self.assertEqual(result["layer"], "builtin+llm")
        self.assertEqual(result["llm"]["status"], "ok")

    def test_report_summary_names_consent_state(self):
        report = diagnose_codes(read_fault_codes(use_simulator=True))
        self.assertEqual(report["llm"]["status"], "consent_required")
        self.assertEqual(self.calls, [])
        self.assertGreater(report["fault_count"], 0)


class TestConsentRecord(ConsentEnvMixin):
    def test_record_and_read_back(self):
        self.assertEqual(consent.get_consent(), "unknown")
        consent.record_consent(False)
        self.assertEqual(consent.get_consent(), "declined")
        consent.record_consent(True)
        self.assertEqual(consent.get_consent(), "granted")
        saved = json.loads(self.consent_path.read_text())
        self.assertEqual(saved["claude_explanations"], "granted")

    def test_env_wins_over_saved_choice(self):
        consent.record_consent(True)
        os.environ["FORGE_OBD_LLM_CONSENT"] = "declined"
        self.assertEqual(consent.get_consent(), "declined")

    def test_corrupt_consent_file_reads_as_unknown(self):
        self.consent_path.write_text("not json at all")
        self.assertEqual(consent.get_consent(), "unknown")

    def test_disclosure_names_claude_and_the_data_shared(self):
        text = consent.DISCLOSURE
        self.assertIn("Claude", text)
        self.assertIn("Anthropic", text)
        self.assertIn("fault code", text)
        self.assertIn("no VIN", text)


class _FakeTty(io.StringIO):
    def isatty(self):
        return True


class TestCliConsentPrompt(ConsentEnvMixin):
    def _run_cli(self, stdin_text):
        out, err = io.StringIO(), io.StringIO()
        fake_stdin = _FakeTty(stdin_text)
        with mock.patch.object(sys, "stdin", fake_stdin):
            with redirect_stdout(out), redirect_stderr(err):
                code = cli_main(["diagnose", "--simulator"])
        return code, out.getvalue(), err.getvalue()

    def test_interactive_decline_is_recorded_and_nothing_is_sent(self):
        code, out, err = self._run_cli("n\n")
        self.assertEqual(code, 0)
        self.assertIn("Claude", err)  # the disclosure "screen" was shown
        self.assertEqual(consent.get_consent(), "declined")
        self.assertEqual(self.calls, [])
        report = json.loads(out)
        self.assertEqual(report["llm"]["status"], "declined")
        self.assertGreater(report["fault_count"], 0)  # app still diagnosed

    def test_interactive_grant_is_recorded_and_calls_claude(self):
        code, out, err = self._run_cli("y\n")
        self.assertEqual(code, 0)
        self.assertEqual(consent.get_consent(), "granted")
        self.assertGreaterEqual(len(self.calls), 1)
        report = json.loads(out)
        self.assertEqual(report["llm"]["status"], "ok")

    def test_non_interactive_run_never_asks_and_never_sends(self):
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            code = cli_main(["diagnose", "--simulator"])
        self.assertEqual(code, 0)
        self.assertNotIn("Allow Claude explanations?", err.getvalue())
        self.assertEqual(self.calls, [])
        report = json.loads(out.getvalue())
        self.assertEqual(report["llm"]["status"], "consent_required")

    def test_no_llm_flag_forces_builtin_even_when_granted(self):
        os.environ["FORGE_OBD_LLM_CONSENT"] = "granted"
        out = io.StringIO()
        with redirect_stdout(out):
            code = cli_main(["diagnose", "--simulator", "--no-llm"])
        self.assertEqual(code, 0)
        self.assertEqual(self.calls, [])
        report = json.loads(out.getvalue())
        self.assertEqual(report["llm"]["status"], "declined")


if __name__ == "__main__":
    unittest.main()
