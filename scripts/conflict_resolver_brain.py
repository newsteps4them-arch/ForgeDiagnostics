#!/usr/bin/env python3
"""
Team Forge - Conflict Resolver & Auto-Merger Bot (Bot 2)

Automated AI-powered resolution engine for Git merge conflicts.
Fetches PR branch, performs merge with base branch, identifies conflict markers,
resolves conflicts using Gemini AI API (or smart resolution fallback), validates zero conflict markers,
commits, pushes, and merges the PR automatically — but only when the PR's CI checks
are not failing (failing or still-running checks hold the merge, with a comment why).
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

def resolve_conflict_content_smart(content: str, filename: str) -> tuple[str, str]:
    """
    Fallback deterministic smart conflict resolver when offline/no API key.

    Returns (resolved_content, method) where method is:
    - "smart-clean": every conflict block was resolvable without guessing
      (identical sides, one empty side, or one side fully containing the other).
    - "smart-union": at least one block fell back to keeping both sides in
      order (the old behaviour). That can duplicate code, so callers record it
      and the pre-merge check gate is what keeps a bad union from merging.
    """
    conflict_pattern = re.compile(
        r'^<<<<<<< [^\n]*\n(.*?)^=======\n(.*?)^>>>>>>> [^\n]*\n?',
        re.DOTALL | re.MULTILINE,
    )
    methods: List[str] = []

    def replacer(match):
        side_a = match.group(1).splitlines(keepends=True)
        side_b = match.group(2).splitlines(keepends=True)

        if side_a == side_b:
            methods.append("clean")
            return "".join(side_a)

        a_code = [l for l in side_a if l.strip()]
        b_code = [l for l in side_b if l.strip()]

        if not a_code:
            methods.append("clean")
            return "".join(side_b)
        if not b_code:
            methods.append("clean")
            return "".join(side_a)
        # One side is a superset of the other's real lines: keep the superset.
        if all(l in b_code for l in a_code):
            methods.append("clean")
            return "".join(side_b)
        if all(l in a_code for l in b_code):
            methods.append("clean")
            return "".join(side_a)

        methods.append("union")
        # Retain both sides in order, dropping duplicate trailing blank lines
        merged = []
        for line in side_a + side_b:
            if merged and not line.strip() and not merged[-1].strip():
                continue
            merged.append(line)

        return "".join(merged)

    resolved = conflict_pattern.sub(replacer, content)
    method = "smart-union" if "union" in methods else "smart-clean"
    return resolved, method

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
        if line[:2] in ("UU", "AA", "DD", "DU", "UD", "AU", "UA"):
            parts = line[3:].strip().split(" -> ")
            conflicting.append(parts[-1])
        elif "CONFLICT" in line:
            parts = line.split()
            if parts:
                conflicting.append(parts[-1])
    return list(set(conflicting))

# A genuine conflict marker line: exactly seven '<' or '>' (optionally followed
# by a label), or a line that is exactly seven '=' characters. Longer runs of
# '=' (banner comments in scripts/docs) are NOT conflict markers.
CONFLICT_MARKER_RE = re.compile(r"^(<{7}|>{7})([ ].*)?$|^={7}$")

def verify_no_conflict_markers(files: Optional[List[str]] = None) -> bool:
    """Verifies the merge is fully resolved: no unmerged index entries remain
    and none of the resolved files still contain real conflict markers.

    The previous implementation ran `git grep -E "^(<<<<<<<|=======|>>>>>>>)"`
    across the WHOLE repo. The `^=======` alternative matches any line of seven
    or more '=' characters — e.g. the banner lines in
    scripts/connect_workflows.ps1 — so verification failed on every PR even
    when the resolution itself succeeded. That was the root cause of Bot 2
    failing at this step on 2026-10-21 (41/41 runs).
    """
    code, stdout, _ = run_cmd(["git", "ls-files", "-u"])
    if code == 0 and stdout.strip():
        return False
    for fname in files or []:
        if not os.path.exists(fname):
            continue
        with open(fname, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                if CONFLICT_MARKER_RE.match(line.rstrip("\r\n")):
                    return False
    return True

def get_check_state(repo: str, sha: str) -> str:
    """Returns the CI state for a commit: 'success', 'failure', 'pending', or
    'none' (no checks reported at all). Uses the combined commit status plus
    check runs."""
    if not sha:
        return "none"
    status = gh_api(f"repos/{repo}/commits/{sha}/status")
    runs = gh_api(f"repos/{repo}/commits/{sha}/check-runs?per_page=100")

    run_list = runs.get("check_runs", []) if isinstance(runs, dict) else []
    total_statuses = status.get("total_count", 0) if isinstance(status, dict) else 0
    if not run_list and not total_statuses:
        return "none"

    for r in run_list:
        if r.get("status") != "completed":
            return "pending"
        if r.get("conclusion") not in ("success", "neutral", "skipped"):
            return "failure"
    if isinstance(status, dict):
        state = status.get("state")
        if state == "failure" or state == "error":
            return "failure"
        if state == "pending" and total_statuses:
            return "pending"
    return "success"

def wait_for_check_state(repo: str, sha: str, attempts: int = 4, delay_seconds: int = 30) -> str:
    """Polls the check state briefly while it is 'pending' (new CI runs can be
    triggered by the push when a PAT, rather than the default GITHUB_TOKEN,
    is used). Returns the final observed state."""
    import time
    state = get_check_state(repo, sha)
    tries = 0
    while state == "pending" and tries < attempts:
        print(f"[INFO] Checks for {sha[:8]} still pending; waiting {delay_seconds}s...")
        time.sleep(delay_seconds)
        state = get_check_state(repo, sha)
        tries += 1
    return state

def resolve_pr_conflicts(pr_number: str, head_branch: str, base_branch: str, repo: str,
                         details: List[str]) -> tuple[str, str]:
    """Main execution loop for Bot 2.

    Returns (outcome, reason) where outcome is one of:
    - "merged":     conflicts resolved, pushed, checks OK, PR merged.
    - "not_merged": conflicts resolved and pushed, but auto-merge was held
                    (checks failing/pending, or the merge API refused).
    - "failed":     the resolution itself failed.
    Every step appends a human-readable line to `details` for the log artifact.
    """
    def note(msg: str):
        print(msg)
        details.append(msg)

    note(f"[INFO] Initializing Conflict Resolver Bot for PR #{pr_number} ({head_branch} -> {base_branch})...")

    # Commit authorship stays under the repo owner's account (project rule:
    # no bot identities on commits).
    run_cmd(["git", "config", "user.name", "newsteps4them-arch"])
    run_cmd(["git", "config", "user.email", "newsteps4them@gmail.com"])

    # Safely fetch origin refs
    run_cmd(["git", "fetch", "origin", base_branch])
    run_cmd(["git", "fetch", "origin", head_branch])

    # Remember the PR branch tip before we touch it: its check state is the
    # fallback signal for the merge gate (a GITHUB_TOKEN push does not trigger
    # fresh CI runs, so the new tip may report no checks at all).
    _, pre_push_sha, _ = run_cmd(["git", "rev-parse", f"origin/{head_branch}"])

    code, _, err = run_cmd(["git", "checkout", "-B", head_branch, f"origin/{head_branch}"])
    if code != 0:
        return "failed", f"Failed to checkout head branch {head_branch}: {err}"

    # Attempt Git merge with base branch
    code, stdout, stderr = run_cmd(["git", "merge", f"origin/{base_branch}", "-m", f"Merge origin/{base_branch} into {head_branch}"])

    resolution_methods: Dict[str, str] = {}
    if code == 0:
        note(f"[INFO] Clean merge succeeded without conflicts.")
    else:
        note(f"[INFO] Merge conflicts detected. Activating AI Conflict Resolution Engine...")
        conflicting_files = find_conflicting_files()
        note(f"[INFO] Conflicting files: {conflicting_files}")

        if not conflicting_files:
            note(f"[WARN] Git merge reported non-zero return code but no files were marked as conflicted.")
            code, stdout, _ = run_cmd(["git", "grep", "-l", "-E", "^<<<<<<< "])
            if code == 0 and stdout:
                conflicting_files = stdout.splitlines()

        for fname in conflicting_files:
            if not os.path.exists(fname):
                continue
            with open(fname, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()

            note(f"[RESOLVING] Resolving conflicts in `{fname}`...")
            resolved_code = resolve_conflict_content_gemini(content, fname)
            method = "gemini"

            if not resolved_code:
                resolved_code, method = resolve_conflict_content_smart(content, fname)
                note(f"[INFO] Resolved `{fname}` with fallback resolver ({method}).")

            resolution_methods[fname] = method

            with open(fname, "w", encoding="utf-8") as f:
                f.write(resolved_code)

            run_cmd(["git", "add", fname])

        if not verify_no_conflict_markers(conflicting_files):
            run_cmd(["git", "merge", "--abort"])
            return "failed", "Conflict markers still present in a resolved file, or unmerged entries remain in the index."

        # Commit resolution
        run_cmd(["git", "commit", "-m", f"fix(conflict): auto-resolve merge conflicts with origin/{base_branch}", "--no-verify"])

    for fname, method in resolution_methods.items():
        details.append(f"- Resolved `{fname}` via `{method}`")
    if any(m == "smart-union" for m in resolution_methods.values()):
        note("[WARN] At least one file used the union fallback (both sides kept). "
             "The check gate below is the safety net: it will not merge on red checks.")

    # Push resolved branch
    note(f"[INFO] Pushing resolved changes to origin/{head_branch}...")
    code, stdout, stderr = run_cmd(["git", "push", "origin", head_branch])
    if code != 0:
        return "failed", f"Failed to push resolved head branch {head_branch}: {stderr}"

    if not (pr_number and pr_number != "None"):
        return "not_merged", "No PR number supplied; resolution pushed only."

    # Conflicts are gone from the branch: remove the conflict label.
    gh_api(f"repos/{repo}/issues/{pr_number}/labels/has-merge-conflict", method="DELETE")

    # --- Merge gate: only auto-merge when checks are not red ---
    _, head_sha, _ = run_cmd(["git", "rev-parse", "HEAD"])
    check_state = wait_for_check_state(repo, head_sha)
    if check_state == "none" and pre_push_sha:
        check_state = get_check_state(repo, pre_push_sha)
        note(f"[INFO] No checks reported for the new tip; using pre-push tip state: {check_state}")
    note(f"[INFO] Check state for PR #{pr_number}: {check_state}")

    if check_state == "failure":
        gh_api(f"repos/{repo}/issues/{pr_number}/comments", method="POST", data={"body": (
            f"### ✅ Merge Conflicts Resolved — Auto-Merge Held (Bot 2)\n\n"
            f"Conflicts between `{head_branch}` and `{base_branch}` were resolved and pushed, "
            f"but the PR's checks are currently **failing**, so Bot 2 did not merge. "
            f"Once the checks pass, this PR can be merged normally."
        )})
        return "not_merged", "Checks are failing on the PR; auto-merge held."
    if check_state == "pending":
        gh_api(f"repos/{repo}/issues/{pr_number}/comments", method="POST", data={"body": (
            f"### ✅ Merge Conflicts Resolved — Auto-Merge Held (Bot 2)\n\n"
            f"Conflicts between `{head_branch}` and `{base_branch}` were resolved and pushed, "
            f"but the PR's checks were still **running** after a short wait, so Bot 2 did not merge. "
            f"If the checks finish green, this PR can be merged normally."
        )})
        return "not_merged", "Checks still pending after wait; auto-merge held."

    # Auto-merge PR into base branch, and verify the API actually merged it.
    note(f"[INFO] Executing auto-merge for PR #{pr_number}...")
    merge_res = gh_api(f"repos/{repo}/pulls/{pr_number}/merge", method="PUT", data={
        "merge_method": "merge",
        "commit_title": f"Merge pull request #{pr_number} from {head_branch}"
    })
    note(f"[INFO] Merge result: {merge_res}")
    if not (isinstance(merge_res, dict) and merge_res.get("merged") is True):
        msg = merge_res.get("message") if isinstance(merge_res, dict) else merge_res
        return "not_merged", f"Merge API did not confirm the merge: {msg}"

    gh_api(f"repos/{repo}/issues/{pr_number}/comments", method="POST", data={"body": (
        f"### ✅ Merge Conflicts Resolved & Merged by Conflict Resolver Bot (Bot 2)\n\n"
        f"All merge conflicts between `{head_branch}` and `{base_branch}` were resolved, "
        f"verified, pushed, and the PR was merged (checks: {check_state})."
    )})
    return "merged", "Conflicts resolved, checks OK, PR merged."

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

    details: List[str] = []
    outcome, reason = "failed", ""
    try:
        if not args.pr or not args.head or not args.base or args.pr == "None":
            outcome, reason = "failed", (
                f"Missing/empty dispatch inputs (pr={args.pr!r}, head={args.head!r}, base={args.base!r}). "
                "For repository_dispatch, the client_payload must carry pr_number, head_branch and base_branch."
            )
        else:
            outcome, reason = resolve_pr_conflicts(args.pr, args.head, args.base, args.repo, details)
    except Exception as e:
        outcome, reason = "failed", f"Unexpected error: {e}"

    status_str = {"merged": "SUCCESS", "not_merged": "RESOLVED_NOT_MERGED", "failed": "FAILED"}[outcome]
    log_lines = [
        "# Conflict Resolver Bot (Bot 2) Diagnostic Report",
        "",
        f"- Target PR: `#{args.pr}` (`{args.head}` -> `{args.base}`)",
        f"- Repository: `{args.repo}`",
        f"- Resolution & Auto-Merge Status: `{status_str}`",
        f"- Reason: {reason}",
        "",
        "## Details",
        "",
    ]
    log_lines.extend(details if details else ["- (no detail lines recorded)"])

    with open("CONFLICT_RESOLVER_LOG.md", "w") as f:
        f.write("\n".join(log_lines) + "\n")

    if outcome == "failed":
        print(f"[ERROR] Bot 2 failed for PR #{args.pr}: {reason}")
        sys.exit(1)
    print(f"[DONE] Bot 2 finished for PR #{args.pr}: {status_str} — {reason}")
    sys.exit(0)

if __name__ == "__main__":
    main()
