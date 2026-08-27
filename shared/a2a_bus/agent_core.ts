import { GoogleGenAI } from '@google/genai';
import * as fs from 'fs';
import * as path from 'path';

export interface AgentConfig {
  id: string;
  name: string;
  codename: string;
  role: string;
  workspace: string;
}

export class ForgeAgent {
  private ai: GoogleGenAI;
  private config: AgentConfig;
  private systemInstruction: string;

  constructor(config: AgentConfig) {
    this.config = config;
    this.ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY || "" });
    this.systemInstruction = this.loadPrompt();
  }

  private loadPrompt(): string {
    try {
      const promptPath = path.join(process.cwd(), this.config.workspace, 'prompt.yaml');
      return fs.readFileSync(promptPath, 'utf-8');
    } catch (error) {
      console.warn(`[!] Warning: Could not load prompt for ${this.config.codename}. Using default.`);
      return `You are ${this.config.name} (${this.config.codename}), role: ${this.config.role}.`;
    }
  }

  async executeTask(taskDescription: string): Promise<string> {
    if (!process.env.GEMINI_API_KEY) {
      return `[SIMULATED] ${this.config.codename} completed task: ${taskDescription} (No API Key found)`;
    }

    try {
      const response = await this.ai.models.generateContent({
          model: 'gemini-2.5-flash',
          contents: taskDescription,
          config: {
              systemInstruction: this.systemInstruction
          }
      });
      return response.text || "No response generated.";
    } catch (error: any) {
      console.error(`[X] Error during task execution for ${this.config.codename}:`, error.message);
      throw error;
    }
  }
}
