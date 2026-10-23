import { describe, it, expect } from "vitest";
import {
  containsShellMetacharacters,
  validateRepoUrl,
  validateGithubToken,
  validateCommitMessage,
} from "../../src/utils/security";

describe("Security Validation Utility", () => {
  describe("containsShellMetacharacters", () => {
    it("should detect dangerous shell metacharacters", () => {
      expect(containsShellMetacharacters("https://github.com/repo; rm -rf /")).toBe(true);
      expect(containsShellMetacharacters("https://github.com/repo && whoami")).toBe(true);
      expect(containsShellMetacharacters("https://github.com/repo | cat /etc/passwd")).toBe(true);
      expect(containsShellMetacharacters("https://github.com/repo`id`")).toBe(true);
      expect(containsShellMetacharacters("https://github.com/repo$(id)")).toBe(true);
      expect(containsShellMetacharacters("https://github.com/repo > output.txt")).toBe(true);
      expect(containsShellMetacharacters("https://github.com/repo < input.txt")).toBe(true);
      expect(containsShellMetacharacters("https://github.com\\repo")).toBe(true);
    });

    it("should allow safe strings without metacharacters", () => {
      expect(containsShellMetacharacters("https://github.com/org/repo.git")).toBe(false);
      expect(containsShellMetacharacters("git@github.com:org/repo.git")).toBe(false);
      expect(containsShellMetacharacters("feat: add new feature #123")).toBe(false);
      expect(containsShellMetacharacters("ghp_1234567890abcdefghijklmnopqrstuvwxyz")).toBe(false);
    });
  });

  describe("validateRepoUrl", () => {
    it("should validate legitimate repo URLs", () => {
      const res1 = validateRepoUrl("https://github.com/user/repo.git");
      expect(res1.isValid).toBe(true);
      expect(res1.cleanUrl).toBe("https://github.com/user/repo.git");

      const res2 = validateRepoUrl("git@github.com:user/repo.git");
      expect(res2.isValid).toBe(true);
      expect(res2.cleanUrl).toBe("git@github.com:user/repo.git");
    });

    it("should reject non-string inputs", () => {
      expect(validateRepoUrl(12345).isValid).toBe(false);
      expect(validateRepoUrl(null).isValid).toBe(false);
      expect(validateRepoUrl({ url: "https://github.com" }).isValid).toBe(false);
    });

    it("should reject empty repo URLs", () => {
      const res = validateRepoUrl("   ");
      expect(res.isValid).toBe(false);
      expect(res.error).toBe("Repository URL is required.");
    });

    it("should reject URLs containing shell metacharacters", () => {
      const res = validateRepoUrl("https://github.com/user/repo.git; echo hacked");
      expect(res.isValid).toBe(false);
      expect(res.error).toBe("Invalid characters in input.");
    });

    it("should reject invalid URL protocols", () => {
      const res = validateRepoUrl("ftp://github.com/user/repo.git");
      expect(res.isValid).toBe(false);
      expect(res.error).toBe("Invalid repository URL format.");
    });
  });

  describe("validateGithubToken", () => {
    it("should allow valid tokens", () => {
      const res = validateGithubToken("ghp_1234567890abcdef");
      expect(res.isValid).toBe(true);
      expect(res.cleanToken).toBe("ghp_1234567890abcdef");
    });

    it("should handle undefined, null, and empty string tokens gracefully", () => {
      expect(validateGithubToken(undefined).isValid).toBe(true);
      expect(validateGithubToken(null).isValid).toBe(true);
      expect(validateGithubToken("").isValid).toBe(true);
    });

    it("should reject non-string tokens", () => {
      expect(validateGithubToken(123456).isValid).toBe(false);
    });

    it("should reject tokens with shell metacharacters", () => {
      const res = validateGithubToken("ghp_token; reboot");
      expect(res.isValid).toBe(false);
      expect(res.error).toBe("Invalid characters in input.");
    });
  });

  describe("validateCommitMessage", () => {
    it("should allow valid commit messages", () => {
      const res = validateCommitMessage("fix: resolve issue with sync");
      expect(res.isValid).toBe(true);
      expect(res.cleanMessage).toBe("fix: resolve issue with sync");
    });

    it("should handle empty or missing commit messages", () => {
      expect(validateCommitMessage(undefined).isValid).toBe(true);
      expect(validateCommitMessage("").isValid).toBe(true);
    });

    it("should reject non-string commit messages", () => {
      expect(validateCommitMessage({ message: "test" }).isValid).toBe(false);
    });

    it("should reject commit messages containing shell metacharacters", () => {
      const res = validateCommitMessage("fix: bug | rm -rf /");
      expect(res.isValid).toBe(false);
      expect(res.error).toBe("Invalid characters in commit message.");
    });
  });
});
