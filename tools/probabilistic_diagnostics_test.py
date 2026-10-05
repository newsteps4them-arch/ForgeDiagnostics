#!/usr/bin/env python3
"""Bounded probabilistic diagnostics experiment.

This is a research/verification harness, not a vehicle controller. The quantum
part is an explicit local state-vector simulator implemented with the Python
standard library. It does not access a real QPU and does not prove diagnostic
accuracy on a physical vehicle.
"""

from __future__ import annotations

import json
import math
import random
from collections import Counter
from pathlib import Path

SEED = 20261005
SCENARIOS = ("normal", "lean", "rich", "sensor_fault")


def normalize(values: list[float]) -> list[float]:
    total = sum(values)
    if total <= 0:
        return [1.0 / len(values)] * len(values)
    return [value / total for value in values]


def local_state_vector_probabilities(scores: list[float]) -> list[float]:
    """Encode evidence scores as amplitudes and measure probabilities."""
    amplitudes = [math.sqrt(max(score, 0.0)) for score in scores]
    norm = math.sqrt(sum(amplitude * amplitude for amplitude in amplitudes))
    if norm == 0:
        return [1.0 / len(scores)] * len(scores)
    return [(amplitude / norm) ** 2 for amplitude in amplitudes]


def sample(probabilities: list[float], rng: random.Random) -> int:
    needle = rng.random()
    cumulative = 0.0
    for index, probability in enumerate(probabilities):
        cumulative += probability
        if needle <= cumulative:
            return index
    return len(probabilities) - 1


def run(trials: int = 20000, shots: int = 256) -> dict[str, object]:
    rng = random.Random(SEED)
    classical_correct = 0
    quantum_correct = 0
    quantum_confidences: list[float] = []
    confusion = Counter()

    for _ in range(trials):
        truth = rng.randrange(len(SCENARIOS))
        scores = [0.08] * len(SCENARIOS)
        scores[truth] = 0.82
        # Bounded evidence noise models imperfect sensors/decoders.
        for index in range(len(scores)):
            scores[index] = max(0.0, scores[index] + rng.gauss(0.0, 0.12))
        classical = max(range(len(scores)), key=scores.__getitem__)
        probabilities = local_state_vector_probabilities(scores)
        measurements = Counter(sample(probabilities, rng) for _ in range(shots))
        quantum = measurements.most_common(1)[0][0]
        confidence = measurements[quantum] / shots
        classical_correct += int(classical == truth)
        quantum_correct += int(quantum == truth)
        quantum_confidences.append(confidence)
        confusion[(truth, quantum)] += 1

    quantum_accuracy = quantum_correct / trials
    classical_accuracy = classical_correct / trials
    mean_confidence = sum(quantum_confidences) / len(quantum_confidences)
    return {
        "seed": SEED,
        "trials": trials,
        "shots_per_trial": shots,
        "scenarios": list(SCENARIOS),
        "classical_argmax_accuracy": round(classical_accuracy, 6),
        "local_statevector_quantum_accuracy": round(quantum_accuracy, 6),
        "local_statevector_mean_measurement_confidence": round(mean_confidence, 6),
        "quantum_simulator": "Python standard-library state-vector probability model",
        "qpu_access": False,
        "confusion_matrix_counts": {
            f"{SCENARIOS[truth]}->{SCENARIOS[predicted]}": count
            for (truth, predicted), count in sorted(confusion.items())
        },
    }


def main() -> int:
    result = run()
    print(json.dumps(result, indent=2, sort_keys=True))
    output = Path("test-run-20261005") / "probabilistic-quantum-report.json"
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"report={output}")
    print("EVIDENCE: probabilistic local simulation only; no real QPU or vehicle was used")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
