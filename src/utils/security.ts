/**
 * Security utility functions for validating inputs and preventing shell command injection.
 */

const SHELL_METACHARACTERS_REGEX = /[;&|$<>\`\\]/;

export function containsShellMetacharacters(input: string): boolean {
  return SHELL_METACHARACTERS_REGEX.test(input);
}

export function validateRepoUrl(repoUrl: unknown): { isValid: boolean; error?: string; cleanUrl?: string } {
  if (typeof repoUrl !== "string") {
    return { isValid: false, error: "Repository URL must be a string." };
  }

  const trimmedUrl = repoUrl.trim();
  if (!trimmedUrl) {
    return { isValid: false, error: "Repository URL is required." };
  }

  if (containsShellMetacharacters(trimmedUrl)) {
    return { isValid: false, error: "Invalid characters in input." };
  }

  if (
    !trimmedUrl.startsWith("https://") &&
    !trimmedUrl.startsWith("http://") &&
    !trimmedUrl.startsWith("git@")
  ) {
    return { isValid: false, error: "Invalid repository URL format." };
  }

  return { isValid: true, cleanUrl: trimmedUrl };
}

export function validateGithubToken(githubToken: unknown): { isValid: boolean; error?: string; cleanToken?: string } {
  if (githubToken === undefined || githubToken === null || githubToken === "") {
    return { isValid: true, cleanToken: "" };
  }

  if (typeof githubToken !== "string") {
    return { isValid: false, error: "GitHub token must be a string." };
  }

  const trimmedToken = githubToken.trim();
  if (containsShellMetacharacters(trimmedToken)) {
    return { isValid: false, error: "Invalid characters in input." };
  }

  return { isValid: true, cleanToken: trimmedToken };
}

export function validateCommitMessage(commitMessage: unknown): { isValid: boolean; error?: string; cleanMessage?: string } {
  if (commitMessage === undefined || commitMessage === null || commitMessage === "") {
    return { isValid: true, cleanMessage: "" };
  }

  if (typeof commitMessage !== "string") {
    return { isValid: false, error: "Commit message must be a string." };
  }

  const trimmedMessage = commitMessage.trim();
  if (containsShellMetacharacters(trimmedMessage)) {
    return { isValid: false, error: "Invalid characters in commit message." };
  }

  return { isValid: true, cleanMessage: trimmedMessage };
}
