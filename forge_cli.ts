#!/usr/bin/env node
import * as fs from 'fs';
import * as path from 'path';
import { ForgeAgent, AgentConfig } from './shared/a2a_bus/agent_core';
import 'dotenv/config';

async function main() {
  const args = process.argv.slice(2);
  if (args.length < 1) {
    console.error("Usage: npx ts-node forge_cli.ts '<task_description>'");
    process.exit(1);
  }

  const task = args[0] || "";
  console.log(`[+] Initializing Forge Collective CLI...`);
  console.log(`[+] Task: "${task}"`);

  let registry: any;
  try {
      const registryData = fs.readFileSync(path.join(process.cwd(), 'agents', 'accounts', 'registry.json'), 'utf-8');
      registry = JSON.parse(registryData);
  } catch(e: any) {
      console.error("Failed to load bot registry.", e.message);
      process.exit(1);
  }

  const aegisConfig = registry.bots.find((b: AgentConfig) => b.codename === 'Aegis-1');
  if (!aegisConfig) {
      console.error("Aegis-1 not found in registry.");
      process.exit(1);
  }

  const aegis = new ForgeAgent(aegisConfig);

  console.log(`\n[Aegis-1] Processing task...`);
  try {
    const result = await aegis.executeTask(task);
    console.log(`\n=== Aegis-1 Response ===\n`);
    console.log(result);
    console.log(`\n========================\n`);
  } catch (error) {
    console.error("Failed to execute task.");
  }
}

main();
