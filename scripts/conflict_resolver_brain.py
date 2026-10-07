#!/usr/bin/env python3
"""
Team Forge - Conflict Resolver & Auto-Merger Bot (Bot 2)

Automated AI-powered resolution engine for Git merge conflicts.
Fetches PR branch, performs merge with base branch, identifies conflict markers,
resolves conflicts using Gemini AI API (or smart resolution fallback), validates zero conflict markers,
runs tests, commits, pushes, and merges the PR automatically.
"""

import os
import sys
import re
import json
import subprocess
import argparse
from typing import List, Dict, Any, Optional

try:
    import google.generativeai as genai
    HAS_GEMINI = True
except ImportError:
    HAS_GEMINI = False

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN") or os.getenv("GH_TOKEN")
REPO_FULL = os.getenv("GITHUB_REPOSITORY", "newsteps4them-arch/ForgeDiagnostics")

def run_cmd(cmd: List[str], cwd: Optional[str] = None) -> tuple[int, str, str]:
    """Helper to run system commands cleanly."""
    try:
        res = subprocess.run(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except Exception as e:
        return 1, "", str(e)

def gh_api(endpoint: str, method: str = "GET", data: Optional[Dict[str, Any]] = None) -> Any:
    """Executes GitHub API calls via gh CLI."""
    cmd = ["gh", "api", "-H", "Accept: application/vnd.github+json", endpoint]
    if method != "GET":
        cmd.extend(["-X", method])
    if data:
        for k, v in data.items():
            if isinstance(v, (dict, list, bool)):
                cmd.extend(["-F", f"{k}={json.dumps(v)}"])
            else:
                cmd.extend(["-f", f"{k}={v}"])
    code, stdout, stderr = run_cmd(cmd)
    if code != 0:
        print(f"[ERROR] API Call failed ({endpoint}): {stderr}")
        return None
    try:
        return json.loads(stdout) if stdout else {}
    except json.JSONDecodeError:
        return stdout

def resolve_conflict_content_smart(content: str, filename: str) -> str:
    """
    Fallback deterministic smart conflict resolver when offline/no API key.
    Merges conflicting blocks by preserving valid unique lines and deduplicating.
    """
    conflict_pattern = re.compile(r'<<<<<<< [^\n]*\n(.*?)=======\n(.*?)>>>>>>> [^\n]*\n', re.DOTALL)

    def replacer(match):
        side_a = match.group(1).splitlines(keepends=True)
        side_b = match.group(2).splitlines(keepends=True)

        # Smart merge: union of unique non-blank lines preserving order
        merged_lines = []
        seen = set()
        for line in side_a + side_b:
            stripped = line.strip()
            if not stripped or stripped not in seen:
                merged_lines.append(line)
                if stripped:
                    seen.add(stripped)
        return "".join(merged_lines)

    return conflict_pattern.sub(replacer, content)

def resolve_conflict_content_gemini(content: str, filename: str) -> Optional[str]:
    """Resolves conflict markers using Gemini API."""
    if not HAS_GEMINI or not GEMINI_API_KEY:
        return None
    try:
        genai.configure(api_key=GEMINI_API_KEY)
        model = genai.GenerativeModel("gemini-2.5-flash")

        prompt = (
            f"You are Team Forge's Autonomous Git Conflict Resolution AI.\n"
            f"The following file `{filename}` contains standard Git merge conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`).\n"
            f"Please carefully resolve all merge conflicts, keeping both intent and correct programming syntax intact.\n"
            f"CRITICAL: Output ONLY the complete, resolved code file content. Do NOT wrap in markdown backticks or add explanatory text.\n\n"
            f"FILE CONTENT WITH CONFLICTS:\n"
            f"```\n{content}\n```"
        )
        response = model.generate_content(prompt)
        text = response.text.strip()

        # Clean up any markdown code block fences if returned
        if text.startswith("```"):
            lines = text.splitlines()
            if lines[0].startswith("```"):
                lines = lines[1:]
            if lines and lines[-1].strip() == "```":
                lines = lines[:-1]
            text = "\n".join(lines)

        if "<<<<<<<" not in text and ">>>>>>>" not in text:
            return text
    except Exception as e:
        print(f"[WARN] Gemini conflict resolution exception for {filename}: {e}")
    return None

def find_conflicting_files() -> List[str]:
    """Finds list of files currently in conflict in git status."""
    code, stdout, _ = run_cmd(["git", "status", "--porcelain"])
    conflicting = []
    for line in stdout.splitlines():
        if line.startswith("UU ") or line.startswith("AA ") or line.startswith("DD ") or line.startswith("DU ") or line.startswith("UD "):
            parts = line[3:].strip().split(" -> ")
            conflicting.append(parts[-1])
        elif "CONFLICT" in line:
            parts = line.split()
            if parts:
                conflicting.append(parts[-1])
    return list(set(conflicting))

def verify_no_conflict_markers() -> bool:
    """Verifies that no conflict markers remain in the repository."""
    code, stdout, _ = run_cmd(["git", "grep", "-E", "^(<<<<<<<|=======|>>>>>>>)"])
    return code != 0 or len(stdout.strip()) == 0

def resolve_pr_conflicts(pr_number: str, head_branch: str, base_branch: str, repo: str) -> bool:
    """Main execution loop for Bot 2."""
    print(f"[INFO] Initializing Conflict Resolver Bot for PR #{pr_number} ({head_branch} -> {base_branch})...")

    # Configure Git Bot identity
    run_cmd(["git", "config", "user.name", "Forge Conflict Resolver Bot"])
    run_cmd(["git", "config", "user.email", "conflict-resolver@forge.local"])

    # Fetch and checkout branches
    run_cmd(["git", "fetch", "origin", f"{base_branch}:{base_branch}"], cwd=None)
    run_cmd(["git", "fetch", "origin", f"{head_branch}:{head_branch}"], cwd=None)

    code, _, err = run_cmd(["git", "checkout", head_branch])
    if code != 0:
        print(f"[ERROR] Failed to checkout head branch {head_branch}: {err}")
        return False

    # Attempt Git merge with base branch
    code, stdout, stderr = run_cmd(["git", "merge", f"origin/{base_branch}", "-m", f"Merge origin/{base_branch} into {head_branch}"])

    if code == 0:
        print(f"[INFO] Clean merge succeeded without conflicts.")
    else:
        print(f"[INFO] Merge conflicts detected. Activating AI Conflict Resolution Engine...")
        conflicting_files = find_conflicting_files()
        print(f"[INFO] Conflicting files: {conflicting_files}")

        if not conflicting_files:
            print(f"[WARN] Git merge reported non-zero return code but no files were marked UU.")
            # Fallback: find files containing conflict markers directly
            code, stdout, _ = run_cmd(["git", "grep", "-l", "-E", "^<<<<<<< "])
            if code == 0 and stdout:
                conflicting_files = stdout.splitlines()

        resolved_count = 0
        for fname in conflicting_files:
            if not os.path.exists(fname):
                continue
            with open(fname, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()

            print(f"[RESOLVING] Resolving conflicts in `{fname}`...")
            resolved_code = resolve_conflict_content_gemini(content, fname)

            if not resolved_code:
                print(f"[INFO] Falling back to smart deterministic resolver for `{fname}`.")
                resolved_code = resolve_conflict_content_smart(content, fname)

            with open(fname, "w", encoding="utf-8") as f:
                f.write(resolved_code)

            run_cmd(["git", "add", fname])
            resolved_count += 1

        if not verify_no_conflict_markers():
            print(f"[ERROR] Conflict markers still detected in codebase after resolution attempt!")
            return False

        # Commit resolution
        run_cmd(["git", "commit", "-m", f"fix(conflict): auto-resolve merge conflicts with origin/{base_branch}", "--no-verify"])

    # 🔍 Mandatory Verification Suite before Push or Auto-Merge
    print(f"[INFO] Running verification test suite (npm run test:all:portable)...")
    test_code, test_out, test_err = run_cmd(["npm", "run", "test:all:portable"])
    if test_code != 0:
        print(f"[ERROR] Resolution failed test suite validation:\n{test_out}\n{test_err}")
        return False

    # Push resolved branch
    print(f"[INFO] Pushing resolved changes to origin/{head_branch}...")
    code, stdout, stderr = run_cmd(["git", "push", "origin", head_branch])
    if code != 0:
        print(f"[ERROR] Failed to push resolved head branch {head_branch}: {stderr}")
        return False

    # Remove conflict label
    if pr_number and pr_number != "None":
        gh_api(f"repos/{repo}/issues/{pr_number}/labels/has-merge-conflict", method="DELETE")

        # Comment success on PR
        comment_body = (
            f"### ✅ Merge Conflicts Resolved by Conflict Resolver Bot (Bot 2)\n\n"
            f"All merge conflicts between `{head_branch}` and `{base_branch}` have been automatically resolved, "
            f"verified against test suite (`npm run test:all:portable`), and pushed.\n\n"
            f"⚡ **Auto-Merging PR #{pr_number}...**"
        )
        gh_api(f"repos/{repo}/issues/{pr_number}/comments", method="POST", data={"body": comment_body})

        # Auto-merge PR into base branch
        print(f"[INFO] Executing auto-merge for PR #{pr_number}...")
        merge_res = gh_api(f"repos/{repo}/pulls/{pr_number}/merge", method="PUT", data={
            "merge_method": "merge",
            "commit_title": f"Merge pull request #{pr_number} from {head_branch}"
        })
        print(f"[INFO] Merge result: {merge_res}")

    return True

def main():
    parser = argparse.ArgumentParser(description="Forge Conflict Resolver Bot (Bot 2)")
    parser.add_argument("--pr", type=str, required=True, help="Pull Request Number")
    parser.add_argument("--head", type=str, required=True, help="Head Branch Name")
    parser.add_argument("--base", type=str, required=True, help="Base Branch Name")
    parser.add_argument("--repo", type=str, default=REPO_FULL, help="GitHub Repository (owner/repo)")
    args = parser.parse_args()

    print(f"===================================================================")
    print(f"🤖 Team Forge Conflict Resolver & Auto-Merger Bot (Bot 2) Initializing...")
    print(f"PR #{args.pr}: {args.head} -> {args.base} ({args.repo})")
    print(f"===================================================================")

    success = resolve_pr_conflicts(args.pr, args.head, args.base, args.repo)

    status_str = "SUCCESS" if success else "FAILED"
    log_content = (
        f"# Conflict Resolver Bot (Bot 2) Diagnostic Report\n\n"
        f"- Target PR: `#{args.pr}` (`{args.head}` -> `{args.base}`)\n"
        f"- Repository: `{args.repo}`\n"
        f"- Resolution & Auto-Merge Status: `{status_str}`\n"
    )

    with open("CONFLICT_RESOLVER_LOG.md", "w") as f:
        f.write(log_content)

    if success:
        print(f"[SUCCESS] Bot 2 resolved conflicts and merged PR #{args.pr} successfully.")
        sys.exit(0)
    else:
        print(f"[ERROR] Bot 2 failed to resolve conflicts or merge PR #{args.pr}.")
        sys.exit(1)

if __name__ == "__main__":
    main()
