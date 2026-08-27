#!/usr/bin/env bash
# ==============================================================================
# ForgeDiagnostic - In-Repo Integration Script
# Run this directly inside your cloned `team.forge` repository.
# ==============================================================================

set -e

echo "[+] Injecting Bot Collective & Diagnostic Core into current repository..."

# 1. Create Agent, Protocol, and Testing Directories
mkdir -p agents/accounts
mkdir -p agents/aegis_orchestrator/dags
mkdir -p agents/nexus_architect/specs
mkdir -p agents/sage_protocols/protocols
mkdir -p agents/vector_hal/drivers
mkdir -p agents/lexicon_database/schemas
mkdir -p agents/cortex_ai/models
mkdir -p agents/vulcan_coder/diffs
mkdir -p agents/guardian_qa/reports
mkdir -p agents/sentinel_security/rules
mkdir -p agents/hermes_ops/builds

mkdir -p shared/a2a_bus
mkdir -p shared/mcp_schemas
mkdir -p shared/dtc_definitions

mkdir -p src/protocols
mkdir -p test/hil_emulator
mkdir -p test/unit
mkdir -p .github/workflows

# 2. Add Bot Accounts Registry
cat << 'JSON' > agents/accounts/registry.json
{
  "organization": "Forge Diagnostics Collective",
  "repository": "newsteps4them-arch/team.forge",
  "bots": [
    { "id": "bot_01", "name": "Orchestrator-Prime", "codename": "Aegis-1", "role": "CEO & Sprint Orchestrator", "email": "aegis.orchestrator@forgediagnostics.com", "workspace": "agents/aegis_orchestrator" },
    { "id": "bot_02", "name": "Nexus-Arch", "codename": "Nexus", "role": "Lead Systems Architect", "email": "nexus.architect@forgediagnostics.com", "workspace": "agents/nexus_architect" },
    { "id": "bot_03", "name": "OBD-Sage", "codename": "Sage", "role": "VP Automotive Protocols", "email": "sage.protocols@forgediagnostics.com", "workspace": "agents/sage_protocols" },
    { "id": "bot_04", "name": "BusMaster-Driver", "codename": "Vector", "role": "HAL & Hardware Interface Lead", "email": "vector.hal@forgediagnostics.com", "workspace": "agents/vector_hal" },
    { "id": "bot_05", "name": "DataForge-Curator", "codename": "Lexicon", "role": "DTC & PID Knowledgebase Lead", "email": "lexicon.database@forgediagnostics.com", "workspace": "agents/lexicon_database" },
    { "id": "bot_06", "name": "MechMind-AI", "codename": "Cortex", "role": "Neural Repair & Analytics Specialist", "email": "cortex.ai@forgediagnostics.com", "workspace": "agents/cortex_ai" },
    { "id": "bot_07", "name": "ForgeCoder-Dev", "codename": "Vulcan", "role": "Autonomous Implementation Lead", "email": "vulcan.coder@forgediagnostics.com", "workspace": "agents/vulcan_coder" },
    { "id": "bot_08", "name": "Guardian-QA", "codename": "Guardian-v2", "role": "Verification & Auto-Heal Lead", "email": "guardian.qa@forgediagnostics.com", "workspace": "agents/guardian_qa" },
    { "id": "bot_09", "name": "Sentinel-Sec", "codename": "Sentinel", "role": "Security & Safety Compliance", "email": "sentinel.security@forgediagnostics.com", "workspace": "agents/sentinel_security" },
    { "id": "bot_10", "name": "Deployer-Ops", "codename": "Hermes", "role": "Release Manager & DevOps Automation", "email": "hermes.release@forgediagnostics.com", "workspace": "agents/hermes_ops" }
  ]
}
JSON

# 3. Add Key Agent System Instructions
cat << 'YAML' > agents/aegis_orchestrator/prompt.yaml
persona:
  name: "Orchestrator-Prime (Aegis-1)"
  role: "Chief Sprint Orchestrator"
system_instruction: |
  Coordinate sprint execution across the team.forge codebase.
  Decompose issues into DAGs and assign to Nexus-Arch, OBD-Sage, ForgeCoder-Dev, and Guardian-QA.
YAML

cat << 'YAML' > agents/sage_protocols/prompt.yaml
persona:
  name: "OBD-Sage (Sage)"
  role: "VP Automotive Protocols"
system_instruction: |
  Govern all OBD-II, CAN, and UDS decoders in src/protocols.
  Ensure formulas match SAE J1979 and ISO 14229 standards.
YAML

cat << 'YAML' > agents/guardian_qa/prompt.yaml
persona:
  name: "Guardian-QA (Guardian-v2)"
  role: "Verification & Auto-Heal QA Lead"
system_instruction: |
  Run tests against the codebase. Intercept CI failures and dispatch auto-heal tasks to ForgeCoder-Dev.
YAML

# 4. Add SAE J1979 Protocol Decoder directly into your \`src/protocols/\`
cat << 'TS' > src/protocols/j1979_decoder.ts
/**
 * ForgeDiagnostic - SAE J1979 Mode 01 & Mode 03 Decoder
 */

export interface DecodedPid {
  pid: string;
  name: string;
  value: number;
  unit: string;
}

export function decodeMode01Response(hexString: string): DecodedPid | null {
  const clean = hexString.replace(/\s+/g, '');
  const match = clean.match(/(?:41)([0-9A-F]{2})([0-9A-F]+)/i);
  if (!match) return null;

  const pid = match[1].toUpperCase();
  const rawBytes = match[2];
  const bytes: number[] = [];
  for (let i = 0; i < rawBytes.length; i += 2) {
    bytes.push(parseInt(rawBytes.substr(i, 2), 16));
  }

  const [A, B] = bytes;

  switch (pid) {
    case '0C': // Engine RPM
      return { pid: '0C', name: 'Engine RPM', value: ((A * 256) + B) / 4, unit: 'RPM' };
    case '0D': // Vehicle Speed
      return { pid: '0D', name: 'Vehicle Speed', value: A, unit: 'km/h' };
    case '05': // Coolant Temp
      return { pid: '05', name: 'Coolant Temperature', value: A - 40, unit: '°C' };
    case '0F': // Intake Air Temp
      return { pid: '0F', name: 'Intake Air Temp', value: A - 40, unit: '°C' };
    case '04': // Calculated Load
      return { pid: '04', name: 'Engine Load', value: (A * 100) / 255, unit: '%' };
    default:
      return { pid, name: \`PID_\${pid}\`, value: A || 0, unit: 'raw' };
  }
}
TS

# 5. Add Virtual ELM327 HIL Simulator
cat << 'PY' > test/hil_emulator/virtual_elm327.py
#!/usr/bin/env python3
"""
ForgeDiagnostic Virtual ELM327 & Vehicle ECU Test Harness
"""
import sys

RESPONSES = {
    "ATZ": "\r\rELM327 v1.5\r\n>",
    "ATE0": "OK\r\n>",
    "ATL0": "OK\r\n>",
    "ATS0": "OK\r\n>",
    "ATSP0": "OK\r\n>",
    "ATDP": "ISO 15765-4 (CAN 11/500)\r\n>",
    "ATRV": "12.6V\r\n>",
    "0100": "41 00 BE 3F B8 13\r\n>",
    "010C": "41 0C 0F A0\r\n>",       # 1000 RPM
    "010D": "41 0D 37\r\n>",          # 55 km/h
    "0105": "41 05 7B\r\n>",          # 83 deg C
    "03":   "43 02 04 20 03 00\r\n>", # P0420, P0300
    "04":   "44 00\r\n>"              # Clear DTCs OK
}

def main():
    sys.stderr.write("[HIL-Emulator] Virtual ECU online.\n")
    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                break
            cmd = line.strip().upper().replace(" ", "")
            if cmd in RESPONSES:
                sys.stdout.write(RESPONSES[cmd])
            else:
                sys.stdout.write("7F 01 12\r\n>")
            sys.stdout.flush()
        except KeyboardInterrupt:
            break

if __name__ == "__main__":
    main()
PY
chmod +x test/hil_emulator/virtual_elm327.py

# 6. Add GitHub Actions Auto-Heal Workflow
cat << 'YML' > .github/workflows/autonomous-guardian.yml
name: Forge Autonomous Guardian v2

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  auto-heal:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 18
          cache: 'npm'

      - name: Clean Dependency Installation
        run: npm ci --ignore-scripts --no-audit --no-fund

      - name: Run Linting
        run: npm run lint --if-present

      - name: Run Vitest Suite
        run: npm test --if-present

      - name: Run Virtual HIL Diagnostics Check
        run: python3 test/hil_emulator/virtual_elm327.py <<< "010C"
YML

echo "[+] Successfully integrated into team.forge!"
