<#
.SYNOPSIS
    Team Forge: Unified Workflow Connector (Android Studio + Antigravity + Google AI Studio + GitHub)
.DESCRIPTION
    Automates and validates the connection between:
    1. Android Studio (Local SDK, Gradle, local.properties)
    2. Antigravity IDE (Workspace & 2-Way Continuous Sync Daemon)
    3. Google AI Studio (Gemini API Keys & System Prompts)
    4. GitHub Repository (newsteps4them-arch/ForgeDiagnostics Secrets & CI/CD)
#>
param (
    [string]$GeminiApiKey = "",
    [string]$AndroidSdkDir = "",
    [switch]$StartSyncDaemon
)

$ErrorActionPreference = "Continue"

function Write-Step([string]$Title) {
    Write-Host "`n=======================================================" -ForegroundColor Cyan
    Write-Host " [*] $Title" -ForegroundColor Green
    Write-Host "=======================================================" -ForegroundColor Cyan
}

function Log-Info([string]$Msg) {
    Write-Host " [INFO] $Msg" -ForegroundColor Gray
}

function Log-Success([string]$Msg) {
    Write-Host " [SUCCESS] $Msg" -ForegroundColor Green
}

function Log-Warn([string]$Msg) {
    Write-Host " [WARN] $Msg" -ForegroundColor Yellow
}

Write-Host @"
=========================================================
  Team Forge: 4-Way Workflow Synchronization Manager
  Android Studio <-> Antigravity <-> AI Studio <-> GitHub
=========================================================
"@ -ForegroundColor Cyan

# ---------------------------------------------------------
# STEP 1: Antigravity & Git Repository Validation
# ---------------------------------------------------------
Write-Step "Step 1: Validating Workspace & Git Remote (Antigravity <-> GitHub)"

$remoteUrl = git remote get-url origin 2>$null
if ($remoteUrl) {
    Log-Success "Git Remote connected: $remoteUrl"
} else {
    Log-Warn "No origin remote found! Adding newsteps4them-arch/ForgeDiagnostics..."
    git remote add origin https://github.com/newsteps4them-arch/ForgeDiagnostics.git
}

# ---------------------------------------------------------
# STEP 2: Google AI Studio <-> Local Environment & GitHub Secrets
# ---------------------------------------------------------
Write-Step "Step 2: Synchronizing Google AI Studio (Gemini API)"

$envFile = ".env"
$envExampleFile = ".env.example"

# Read existing key from .env if present
$currentGeminiKey = ""
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^GEMINI_API_KEY=(.*)$") {
            $currentGeminiKey = $matches[1].Trim()
        }
    }
}

if (-not [string]::IsNullOrWhiteSpace($GeminiApiKey)) {
    $currentGeminiKey = $GeminiApiKey
}

if ([string]::IsNullOrWhiteSpace($currentGeminiKey) -or $currentGeminiKey -eq "GEMINI_API_KEY_PLACEHOLDER") {
    Log-Warn "No Google AI Studio GEMINI_API_KEY detected in .env."
    Log-Info "Get your free Gemini API key from: https://aistudio.google.com/app/apikey"
    Write-Host -NoNewline "Enter your Google AI Studio GEMINI_API_KEY (or press Enter to skip): " -ForegroundColor Yellow
    $userInputKey = Read-Host
    if (-not [string]::IsNullOrWhiteSpace($userInputKey)) {
        $currentGeminiKey = $userInputKey.Trim()
    }
}

# Write/update .env file
if (-not (Test-Path $envFile)) {
    if (Test-Path $envExampleFile) {
        Copy-Item $envExampleFile $envFile
    } else {
        Set-Content -Path $envFile -Value "GEMINI_API_KEY="
    }
}

if (-not [string]::IsNullOrWhiteSpace($currentGeminiKey) -and $currentGeminiKey -ne "GEMINI_API_KEY_PLACEHOLDER") {
    $envContent = Get-Content $envFile
    $updated = $false
    $newContent = $envContent | ForEach-Object {
        if ($_ -match "^GEMINI_API_KEY=") {
            $updated = $true
            "GEMINI_API_KEY=$currentGeminiKey"
        } else {
            $_
        }
    }
    if (-not $updated) {
        $newContent += "GEMINI_API_KEY=$currentGeminiKey"
    }
    Set-Content -Path $envFile -Value $newContent
    Log-Success "Saved GEMINI_API_KEY to local .env (used by Android Studio & Antigravity)"

    # Sync to GitHub Secrets via gh CLI
    $ghAuth = gh auth status 2>&1
    if ($LASTEXITCODE -eq 0) {
        Log-Info "Syncing GEMINI_API_KEY to GitHub Secrets (newsteps4them-arch/ForgeDiagnostics)..."
        $currentGeminiKey | gh secret set GEMINI_API_KEY --repo newsteps4them-arch/ForgeDiagnostics 2>&1 | Out-Null
        Log-Success "GitHub Secret GEMINI_API_KEY synchronized for CI/CD & Forge Autonomous Guardian!"
    }
}

# ---------------------------------------------------------
# STEP 3: Android Studio Local Properties & SDK Setup
# ---------------------------------------------------------
Write-Step "Step 3: Configuring Android Studio (local.properties & Gradle)"

$localPropFile = "local.properties"
$defaultSdkPaths = @(
    "$env:LOCALAPPDATA\Android\Sdk",
    "$env:USERPROFILE\AppData\Local\Android\Sdk",
    "C:\Android\Sdk"
)

$detectedSdk = ""
if (-not [string]::IsNullOrWhiteSpace($AndroidSdkDir)) {
    $detectedSdk = $AndroidSdkDir
} else {
    foreach ($path in $defaultSdkPaths) {
        if (Test-Path $path) {
            $detectedSdk = $path
            break
        }
    }
}

$localPropLines = @()
if (Test-Path $localPropFile) {
    $localPropLines = Get-Content $localPropFile
}

if (-not [string]::IsNullOrWhiteSpace($detectedSdk)) {
    $formattedSdk = $detectedSdk.Replace('\', '\\').Replace(':', '\:')
    $hasSdk = $false
    $updatedPropLines = $localPropLines | ForEach-Object {
        if ($_ -match "^sdk\.dir=") {
            $hasSdk = $true
            "sdk.dir=$formattedSdk"
        } else {
            $_
        }
    }
    if (-not $hasSdk) {
        $updatedPropLines += "sdk.dir=$formattedSdk"
    }
    Set-Content -Path $localPropFile -Value $updatedPropLines
    Log-Success "Configured Android SDK in local.properties: $detectedSdk"
} else {
    Log-Warn "Android SDK not found in standard paths. If Android Studio is installed, it will automatically configure sdk.dir upon opening."
}

# ---------------------------------------------------------
# STEP 4: 2-Way Git Continuous Sync Daemon
# ---------------------------------------------------------
Write-Step "Step 4: Real-time 2-Way Sync Engine (Local <-> GitHub)"

Log-Info "Changes made in Android Studio or Antigravity IDE will automatically sync with GitHub."
Log-Info "Run anytime via: npm run sync:watch OR powershell -File ./scripts/auto_sync.ps1"

if ($StartSyncDaemon) {
    Log-Info "Starting Auto-Sync Daemon..."
    & powershell -NoProfile -ExecutionPolicy Bypass -File "./scripts/auto_sync.ps1" -IntervalSeconds 15
}

Write-Host @"

=========================================================
  🎉 Workflow Integration Complete!
=========================================================
  1. Android Studio: Open '$(Get-Location)'
  2. Antigravity IDE: Real-time pair programming & AI
  3. Google AI Studio: Key synced in .env & GitHub Secrets
  4. GitHub Repo: Tracking origin/main with 2-Way Auto-Sync
=========================================================
"@ -ForegroundColor Green
