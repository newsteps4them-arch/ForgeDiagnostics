<#
.SYNOPSIS
    ForgeDiagnostics 2-Way Git Sync Script (Single-Pass)
.DESCRIPTION
    Performs a bidirectional sync with the remote repository:
    1. Stashes/fetches remote updates and merges/rebases with autostash.
    2. Stages and commits local changes.
    3. Pushes local commits to origin main.
#>
param (
    [string]$CommitMessage = "",
    [string]$Branch = "main",
    [string]$Remote = "origin"
)

$ErrorActionPreference = "Continue"

function Write-Timestamped([string]$Message, [string]$Level = "INFO") {
    $time = Get-Date -Format "HH:mm:ss"
    switch ($Level) {
        "SUCCESS" { Write-Host "[$time] [SUCCESS] $Message" -ForegroundColor Green }
        "WARN"    { Write-Host "[$time] [WARN]    $Message" -ForegroundColor Yellow }
        "ERROR"   { Write-Host "[$time] [ERROR]   $Message" -ForegroundColor Red }
        Default   { Write-Host "[$time] [INFO]    $Message" -ForegroundColor Cyan }
    }
}

Write-Timestamped "Starting 2-way synchronization with GitHub ($Remote/$Branch)..."

# 1. Fetch remote tracking updates
Write-Timestamped "Fetching latest changes from remote ($Remote)..."
$fetchOutput = git fetch $Remote 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Timestamped "Failed to fetch from remote: $fetchOutput" "ERROR"
    exit $LASTEXITCODE
}

# 2. Check remote differences
$behindCount = [int](git rev-list --count HEAD.."$Remote/$Branch" 2>$null)
if ($behindCount -gt 0) {
    Write-Timestamped "Found $behindCount new commit(s) on GitHub. Pulling with autostash..." "WARN"
    $pullOutput = git pull --rebase --autostash $Remote $Branch 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Timestamped "Rebase encountered conflict, falling back to standard pull: $pullOutput" "WARN"
        git rebase --abort 2>$null
        git pull --no-edit $Remote $Branch 2>&1
    } else {
        Write-Timestamped "Pulled $behindCount remote commit(s) successfully." "SUCCESS"
    }
} else {
    Write-Timestamped "Workspace is up to date with remote commits." "SUCCESS"
}

# 3. Check local uncommitted changes
$status = git status --porcelain
if ($status) {
    Write-Timestamped "Detected local changes to sync:" "WARN"
    $status | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }

    if ([string]::IsNullOrWhiteSpace($CommitMessage)) {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        $CommitMessage = "Auto-sync: update workspace [$timestamp]"
    }

    Write-Timestamped "Staging and committing local changes..."
    git add -A
    git commit -m $CommitMessage 2>&1 | Out-Null
    Write-Timestamped "Local changes committed." "SUCCESS"
} else {
    Write-Timestamped "No uncommitted local changes." "INFO"
}

# 4. Check if local is ahead and needs push
$aheadCount = [int](git rev-list --count "$Remote/$Branch"..HEAD 2>$null)
if ($aheadCount -gt 0) {
    Write-Timestamped "Pushing $aheadCount local commit(s) to $Remote/$Branch..." "INFO"
    $pushOutput = git push $Remote $Branch 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Timestamped "Pushed successfully to GitHub!" "SUCCESS"
    } else {
        Write-Timestamped "Push failed: $pushOutput" "ERROR"
        exit $LASTEXITCODE
    }
} else {
    Write-Timestamped "Remote is fully in sync with local (0 pending pushes)." "SUCCESS"
}

Write-Timestamped "2-Way Sync Complete!" "SUCCESS"
