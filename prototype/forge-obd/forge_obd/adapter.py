"""Adapter layer: real ELM327 via python-obd, with an explicit simulator fallback.

Nothing here uploads vehicle data anywhere. The simulator is clearly labeled
and can never be mistaken for a live vehicle: every reading it produces is
tagged with source="simulator".
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Optional


@dataclass
class FaultReading:
    code: str
    source: str  # "vehicle" or "simulator" — provenance is never dropped


@dataclass
class SimulatorAdapter:
    """Explicitly-labeled simulator. Produces a fixed, documented scenario."""

    scenario_codes: List[str] = field(default_factory=lambda: ["P0300", "P0171"])

    def get_fault_codes(self) -> List[FaultReading]:
        return [FaultReading(code=c, source="simulator") for c in self.scenario_codes]

    def describe(self) -> str:
        return "SIMULATOR — no vehicle connected (scenario: P0300 + P0171)"


@dataclass
class ObdAdapter:
    """Real ELM327 adapter via python-obd. Fails honestly when unavailable."""

    port: Optional[str] = None

    def get_fault_codes(self) -> List[FaultReading]:
        try:
            import obd  # type: ignore
        except ImportError as exc:
            raise RuntimeError(
                "python-obd is not installed. Install it with "
                "'pip install obd' or run with --simulator."
            ) from exc

        connection = obd.OBD(portstr=self.port, fast=False, timeout=10)
        try:
            if not connection.is_connected():
                raise RuntimeError(
                    "No ELM327 adapter found"
                    + (f" on {self.port}" if self.port else "")
                    + ". Connect the adapter and try again, or run with --simulator."
                )
            response = connection.query(obd.commands.GET_DTC)
            if response.is_null():
                # Null response = the ECU did not answer. That is NOT zero faults.
                raise RuntimeError(
                    "ECU did not respond to the fault-code request "
                    "(no data / timeout). This is not the same as 'no faults'."
                )
            codes = [str(c) for c in (response.value or [])]
            return [FaultReading(code=c, source="vehicle") for c in codes]
        finally:
            connection.close()

    def describe(self) -> str:
        return f"ELM327 adapter on {self.port or 'auto-detected port'}"


def read_fault_codes(use_simulator: bool, port: Optional[str] = None) -> List[FaultReading]:
    if use_simulator:
        return SimulatorAdapter().get_fault_codes()
    return ObdAdapter(port=port).get_fault_codes()
