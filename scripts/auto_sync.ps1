<#
.SYNOPSIS
    Continuous 2-Way Git Sync Daemon for ForgeDiagnostics (PowerShell)
.DESCRIPTION
    Keeps the local workspace and GitHub remote repository continuously synchronized in real-time.
    - Automatically pulls incoming remote commits.
    - Automatically stages, commits, and pushes local edits.
    - Gracefully handles temporary network glitches and reconnection.
.EXAMPLE
    .\scripts\auto_sync.ps1 -IntervalSeconds 15
#>
param (
    [int]$IntervalSeconds = 15,
    [string]$Branch = "main",
    [string]$Remote = "origin",
    [switch]$Once
)

$ErrorActionPreference = "Continue"

function Log-Message([string]$Msg, [string]$Level = "INFO") {
    $time = Get-Date -Format "HH:mm:ss"
    switch ($Level) {
        "SUCCESS" { Write-Host "[$time] [SUCCESS] $Msg" -ForegroundColor Green }
        "SYNC"    { Write-Host "[$time] [SYNC]    $Msg" -ForegroundColor Cyan }
        "WARN"    { Write-Host "[$time] [WARN]    $Msg" -ForegroundColor Yellow }
        "ERROR"   { Write-Host "[$time] [ERROR]   $Msg" -ForegroundColor Red }
        Default   { Write-Host "[$time] [INFO]    $Msg" -ForegroundColor Gray }
    }
}

function Perform-SyncCycle {
    # 1. Fetch remote tracking updates silently
    git fetch $Remote --quiet 2>$null
    if ($LASTEXITCODE -ne 0) {
        Log-Message "Remote unreachable or offline, will retry next cycle..." "WARN"
        return
    }

    # 2. Check remote differences & pull if behind
    $behindCount = [int](git rev-list --count HEAD.."$Remote/$Branch" 2>$null)
    if ($behindCount -gt 0) {
        Log-Message "GitHub has $behindCount new commit(s). Pulling into workspace..." "SYNC"
        $pullResult = git pull --rebase --autostash $Remote $Branch 2>&1
        if ($LASTEXITCODE -ne 0) {
            Log-Message "Rebase pull failed, trying merge pull: $pullResult" "WARN"
            git rebase --abort 2>$null
            git pull --no-edit $Remote $Branch 2>&1 | Out-Null
        } else {
            Log-Message "Successfully pulled and updated local workspace with remote changes." "SUCCESS"
        }
    }

    # 3. Check for local modifications/untracked files
    $status = git status --porcelain
    if ($status) {
        Log-Message "Local changes detected. Auto-committing..." "SYNC"
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        git add -A
        git commit -m "Auto-sync: local updates [$timestamp]" --quiet 2>$null
        Log-Message "Local changes committed." "SUCCESS"
    }

    # 4. Check if local is ahead and push
    $aheadCount = [int](git rev-list --count "$Remote/$Branch"..HEAD 2>$null)
    if ($aheadCount -gt 0) {
        Log-Message "Pushing $aheadCount commit(s) to GitHub ($Remote/$Branch)..." "SYNC"
        $pushResult = git push $Remote $Branch 2>&1
        if ($LASTEXITCODE -eq 0) {
            Log-Message "Pushed changes to GitHub successfully!" "SUCCESS"
        } else {
            Log-Message "Push failed: $pushResult" "ERROR"
        }
    }
}

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "   Team Forge 2-Way Git Continuous Sync Daemon Active    " -ForegroundColor Green
Write-Host "   Branch: $Branch | Remote: $Remote | Interval: ${IntervalSeconds}s " -ForegroundColor Gray
Write-Host "=========================================================" -ForegroundColor Cyan

if ($Once) {
    Perform-SyncCycle
    exit 0
}

Log-Message "Starting continuous synchronization loop (Press Ctrl+C to stop)..." "INFO"

while ($true) {
    try {
        Perform-SyncCycle
    } catch {
        Log-Message "Exception during sync: $_" "ERROR"
    }
    Start-Sleep -Seconds $IntervalSeconds
}
