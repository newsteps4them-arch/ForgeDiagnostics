[CmdletBinding()]
param(
    [string]$Repository = "newsteps4them-arch/ForgeDiagnostics",
    [string]$ReleaseAsset = "Forge-debug-1.0.0.141.apk",
    [switch]$ProbeSerialAdapters,
    [switch]$ProbeGemini
)

$ErrorActionPreference = "SilentlyContinue"
$results = [System.Collections.Generic.List[object]]::new()

function Add-Result([string]$Name, [string]$Status, [string]$Detail) {
    $results.Add([pscustomobject]@{ Name = $Name; Status = $Status; Detail = $Detail })
}

function Test-HttpEndpoint([string]$Name, [string]$Uri) {
    try {
        $response = Invoke-WebRequest -Uri $Uri -Method Get -TimeoutSec 10 -MaximumRedirection 3
        Add-Result $Name "PASS" ("HTTP {0}, {1} ms" -f $response.StatusCode, $response.Headers."X-Response-Time")
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode) { Add-Result $Name "PASS" ("Reachable, HTTP $statusCode") }
        else { Add-Result $Name "FAIL" $_.Exception.Message }
    }
}

Write-Host "ForgeDiagnostics integration verification" -ForegroundColor Cyan
Write-Host "No credentials are printed or written by this script.`n"

Test-HttpEndpoint "NHTSA recalls API" "https://api.nhtsa.gov/recalls/recallsByVehicle?make=Ford&model=F-150&modelYear=2020"
Test-HttpEndpoint "NHTSA VIN decoder" "https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVin/1FTFW1ET1EFA00001?format=json"
Test-HttpEndpoint "GitHub repository" "https://github.com/$Repository"

$assetUri = "https://github.com/$Repository/releases/latest/download/$ReleaseAsset"
try {
    $assetResponse = Invoke-WebRequest -Uri $assetUri -Method Head -TimeoutSec 15 -MaximumRedirection 5
    Add-Result "Latest APK download" "PASS" ("HTTP {0}, {1} bytes" -f $assetResponse.StatusCode, $assetResponse.Headers.'Content-Length')
} catch {
    Add-Result "Latest APK download" "FAIL" "Asset is unavailable or the latest release has no matching APK"
}

$registryPath = Join-Path $PSScriptRoot "..\agents\accounts\registry.json"
if (Test-Path $registryPath) {
    try {
        $registry = Get-Content $registryPath -Raw | ConvertFrom-Json
        $botCount = @($registry.bots).Count
        $missingPrompts = @($registry.bots | Where-Object { -not (Test-Path (Join-Path $PSScriptRoot "..\$($_.workspace)\prompt.yaml")) }).Count
        if ($botCount -gt 0 -and $missingPrompts -eq 0) { Add-Result "Bot registry and prompts" "PASS" "$botCount bots configured" }
        else { Add-Result "Bot registry and prompts" "FAIL" "$missingPrompts bot prompts missing" }
    } catch { Add-Result "Bot registry and prompts" "FAIL" "Invalid registry JSON" }
} else { Add-Result "Bot registry and prompts" "FAIL" "Registry file missing" }

$geminiKey = $env:GEMINI_API_KEY
if ([string]::IsNullOrWhiteSpace($geminiKey) -or $geminiKey.EndsWith("_PLACEHOLDER")) {
    Add-Result "Gemini credentials" "BLOCKED" "GEMINI_API_KEY is not configured"
} elseif ($ProbeGemini) {
    try {
        $body = @{ contents = @(@{ parts = @(@{ text = "Reply with OK" }) }) } | ConvertTo-Json -Depth 5
        $uri = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiKey"
        $response = Invoke-RestMethod -Uri $uri -Method Post -ContentType "application/json" -Body $body -TimeoutSec 20
        Add-Result "Gemini API request" "PASS" "Model returned a response"
    } catch { Add-Result "Gemini API request" "FAIL" "Request failed; credentials are not printed" }
} else { Add-Result "Gemini API request" "SKIP" "Use -ProbeGemini to make a live request" }

if (Get-Command adb -ErrorAction SilentlyContinue) {
    $adbDevices = @(adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" })
    if ($adbDevices.Count -gt 0) {
        Add-Result "ADB Android device" "PASS" "$($adbDevices.Count) authorized device(s)"
        $deviceId = ($adbDevices[0] -split "\s+")[0]
        $androidVersion = adb -s $deviceId shell getprop ro.build.version.release
        Add-Result "ADB shell access" "PASS" "Android $androidVersion"
    } else { Add-Result "ADB Android device" "BLOCKED" "No authorized device; enable USB debugging and accept the RSA prompt" }
} else { Add-Result "ADB Android device" "BLOCKED" "adb is not installed or not on PATH" }

if ($ProbeSerialAdapters) {
    $ports = [System.IO.Ports.SerialPort]::GetPortNames()
    if ($ports.Count -eq 0) { Add-Result "Serial OBD adapter" "BLOCKED" "No Windows COM ports detected" }
    foreach ($portName in $ports) {
        $port = $null
        try {
            $port = [System.IO.Ports.SerialPort]::new($portName, 115200, [System.IO.Ports.Parity]::None, 8, [System.IO.Ports.StopBits]::One)
            $port.ReadTimeout = 1500
            $port.Write("ATZ`r")
            $port.Open()
            $reply = $port.ReadExisting()
            if ($reply -match "ELM|STN|OBD|OK") { Add-Result "Serial $portName" "PASS" "Adapter answered reset" }
            else { Add-Result "Serial $portName" "FAIL" "Port opened but did not identify as an OBD adapter" }
        } catch { Add-Result "Serial $portName" "FAIL" "Could not open or query port" }
        finally { if ($port -and $port.IsOpen) { $port.Close() } }
    }
} else { Add-Result "Serial OBD adapter" "SKIP" "Use -ProbeSerialAdapters only when the adapter is not claimed by Android" }

$results | Format-Table -AutoSize
$failures = @($results | Where-Object { $_.Status -eq "FAIL" })
if ($failures.Count -gt 0) { exit 1 }
