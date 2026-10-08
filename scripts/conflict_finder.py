#!/usr/bin/env python3
"""
Team Forge - Conflict Finder Bot (Bot 1)

Searches open GitHub Pull Requests, checks for merge conflicts against their target
base branch, labels conflicting PRs with `has-merge-conflict`, posts detailed diagnostic
comments on the PRs, and dispatches the Conflict Resolver Bot (Bot 2) to resolve them automatically.
"""

import os
import sys
import json
import subprocess
import argparse
from typing import List, Dict, Any, Optional

GITHUB_TOKEN = os.getenv("GITHUB_TOKEN") or os.getenv("GH_TOKEN")
REPO_FULL = os.getenv("GITHUB_REPOSITORY", "newsteps4them-arch/ForgeDiagnostics")

def run_command(cmd: List[str], cwd: Optional[str] = None, check: bool = False) -> tuple[int, str, str]:
    """Runs a subprocess command and returns (returncode, stdout, stderr)."""
    try:
        res = subprocess.run(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=check)
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except subprocess.CalledProcessError as e:
        return e.returncode, e.stdout.strip() if e.stdout else "", e.stderr.strip() if e.stderr else str(e)
    except Exception as ex:
        return 1, "", str(ex)

def gh_api_request(endpoint: str, method: str = "GET", data: Optional[Dict[str, Any]] = None) -> Any:
    """Executes a GitHub API request using the gh CLI."""
    cmd = ["gh", "api", "-H", "Accept: application/vnd.github+json", endpoint]
    if method != "GET":
        cmd.extend(["-X", method])
    if data:
        for k, v in data.items():
            if isinstance(v, (dict, list, bool)):
                cmd.extend(["-F", f"{k}={json.dumps(v)}"])
            else:
                cmd.extend(["-f", f"{k}={v}"])

    code, stdout, stderr = run_command(cmd)
    if code != 0:
        print(f"[ERROR] API request failed ({endpoint}): {stderr}")
        return None
    try:
        return json.loads(stdout) if stdout else {}
    except json.JSONDecodeError:
        return stdout

def get_open_pull_requests(repo: str) -> List[Dict[str, Any]]:
    """Retrieves all open Pull Requests for the repository."""
    endpoint = f"repos/{repo}/pulls?state=open&per_page=100"
    prs = gh_api_request(endpoint)
    if isinstance(prs, list):
        return prs
    print(f"[WARN] Failed to list PRs or no PRs found: {prs}")
    return []

def get_pr_details(repo: str, pr_number: int) -> Optional[Dict[str, Any]]:
    """Fetches detailed PR information including mergeable state."""
    endpoint = f"repos/{repo}/pulls/{pr_number}"
    return gh_api_request(endpoint)

def check_local_merge_conflict(base_branch: str, head_branch: str) -> tuple[bool, List[str]]:
    """
    Dry-runs a git merge locally to verify if conflicts exist between base and head.
    Returns (has_conflict, list_of_conflicting_files).
    """
    run_command(["git", "fetch", "origin", f"{base_branch}:{base_branch}"], check=False)
    run_command(["git", "fetch", "origin", f"{head_branch}:{head_branch}"], check=False)

    code, stdout, stderr = run_command(["git", "merge-tree", f"origin/{base_branch}", f"origin/{head_branch}"])

    conflicting_files = []
    has_conflict = False

    if code != 0 or ">>>" in stdout or "CONFLICT" in stdout:
        has_conflict = True
        for line in stdout.splitlines():
            if "CONFLICT" in line and "in" in line:
                parts = line.split("in")
                if len(parts) > 1:
                    conflicting_files.append(parts[-1].strip())
            elif line.startswith("changed in both"):
                parts = line.split()
                if parts:
                    conflicting_files.append(parts[-1].strip())

    return has_conflict, list(set(conflicting_files))

def ensure_label_exists(repo: str, label_name: str = "has-merge-conflict", color: str = "d93f0b"):
    """Ensures that the conflict label exists in the repository."""
    endpoint = f"repos/{repo}/labels/{label_name}"
    label = gh_api_request(endpoint)
    if not label or "name" not in label:
        gh_api_request(f"repos/{repo}/labels", method="POST", data={
            "name": label_name,
            "color": color,
            "description": "Indicates the PR has merge conflicts that need automated resolution"
        })

def label_pr_with_conflict(repo: str, pr_number: int, label_name: str = "has-merge-conflict"):
    """Applies the conflict label to the specified PR."""
    endpoint = f"repos/{repo}/issues/{pr_number}/labels"
    gh_api_request(endpoint, method="POST", data={"labels": [label_name]})

def post_pr_conflict_comment(repo: str, pr_number: int, conflicting_files: List[str]):
    """Posts or updates a diagnostic comment on the PR detailing the merge conflict."""
    file_list_md = "\n".join([f"- `{f}`" for f in conflicting_files]) if conflicting_files else "- *Detected during dry-run merge check*"

    comment_body = (
        f"### ⚠️ Merge Conflict Detected by Conflict Finder Bot (Bot 1)\n\n"
        f"This Pull Request has merge conflicts with its base branch that prevent automatic merging.\n\n"
        f"**Conflicting Files:**\n{file_list_md}\n\n"
        f"🤖 **Automated Resolution:**\n"
        f"The **Conflict Resolver Bot (Bot 2)** has been dispatched to resolve these conflicts automatically, "
        f"verify builds & unit tests, push the clean resolution, and complete the merge."
    )

    endpoint = f"repos/{repo}/issues/{pr_number}/comments"
    gh_api_request(endpoint, method="POST", data={"body": comment_body})

def dispatch_conflict_resolver_bot(repo: str, pr_number: int, head_branch: str, base_branch: str):
    """Triggers the Conflict Resolver Bot (Bot 2) workflow via workflow_dispatch API."""
    endpoint = f"repos/{repo}/actions/workflows/conflict_resolver_bot.yml/dispatches"
    result = gh_api_request(endpoint, method="POST", data={
        "ref": base_branch if base_branch in ["main", "master"] else "main",
        "inputs": {
            "pr_number": str(pr_number),
            "head_branch": head_branch,
            "base_branch": base_branch
        }
    })
    print(f"[INFO] Dispatched Conflict Resolver Bot for PR #{pr_number} ({head_branch} -> {base_branch}): {result}")

def inspect_and_process_prs(repo: str) -> List[Dict[str, Any]]:
    """Inspects all open PRs, detects merge conflicts, labels, comments, and dispatches Bot 2."""
    open_prs = get_open_pull_requests(repo)
    print(f"[INFO] Found {len(open_prs)} open Pull Request(s) in {repo}.")

    ensure_label_exists(repo)
    conflicting_prs = []

    for pr in open_prs:
        pr_number = pr.get("number")
        head_branch = pr.get("head", {}).get("ref")
        base_branch = pr.get("base", {}).get("ref")

        if not pr_number or not head_branch or not base_branch:
            continue

        print(f"[INFO] Checking PR #{pr_number}: '{pr.get('title')}' ({head_branch} -> {base_branch})...")

        pr_detail = get_pr_details(repo, pr_number) or {}
        mergeable = pr_detail.get("mergeable")
        mergeable_state = pr_detail.get("mergeable_state")

        has_conflict = False
        conflicting_files = []

        if mergeable is False or mergeable_state == "dirty":
            has_conflict = True
            print(f"[CONFLICT] GitHub API reports conflict for PR #{pr_number} (mergeable={mergeable}, state={mergeable_state}).")
        else:
            local_conflict, files = check_local_merge_conflict(base_branch, head_branch)
            if local_conflict:
                has_conflict = True
                conflicting_files = files
                print(f"[CONFLICT] Local dry-run merge detected conflicts for PR #{pr_number} in files: {files}")

        if has_conflict:
            conflicting_prs.append({
                "pr_number": pr_number,
                "head_branch": head_branch,
                "base_branch": base_branch,
                "conflicting_files": conflicting_files
            })

            label_pr_with_conflict(repo, pr_number)
            post_pr_conflict_comment(repo, pr_number, conflicting_files)
            dispatch_conflict_resolver_bot(repo, pr_number, head_branch, base_branch)
        else:
            print(f"[OK] PR #{pr_number} is clean and mergeable without conflicts.")

    return conflicting_prs

def main():
    parser = argparse.ArgumentParser(description="Forge Conflict Finder Bot (Bot 1)")
    parser.add_argument("--repo", type=str, default=REPO_FULL, help="GitHub Repository (owner/repo)")
    args = parser.parse_args()

    print(f"===================================================================")
    print(f"🤖 Team Forge Conflict Finder Bot (Bot 1) Initializing...")
    print(f"Target Repository: {args.repo}")
    print(f"===================================================================")

    conflicting_prs = inspect_and_process_prs(args.repo)

    log_content = (
        f"# Conflict Finder Bot (Bot 1) Diagnostic Report\n\n"
        f"- Target Repository: `{args.repo}`\n"
        f"- Total Conflicting PRs Detected: `{len(conflicting_prs)}`\n\n"
    )
    if conflicting_prs:
        log_content += "## Conflicting Pull Requests Identified:\n"
        for c in conflicting_prs:
            log_content += f"- **PR #{c['pr_number']}** (`{c['head_branch']}` -> `{c['base_branch']}`)\n"
    else:
        log_content += "All open pull requests are currently conflict-free.\n"

    with open("CONFLICT_FINDER_LOG.md", "w") as f:
        f.write(log_content)

    print(f"[SUCCESS] Conflict Finder Bot completed scan. Summary written to CONFLICT_FINDER_LOG.md.")

if __name__ == "__main__":
    main()
