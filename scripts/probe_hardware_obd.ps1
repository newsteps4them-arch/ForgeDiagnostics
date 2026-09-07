# Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
# Proprietary and Confidential.
# Forge Hardware Diagnostics & OBD Serial Probe Utility

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Forge Agentic Diagnostics: OBD Hardware Diagnostic Probe" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Discover Active Serial Ports
$availablePorts = [System.IO.Ports.SerialPort]::GetPortNames()
Write-Host "`n[1/3] Discovered Serial Ports:" -ForegroundColor Yellow
if ($availablePorts.Count -eq 0) {
    Write-Host "  No COM ports detected in system." -ForegroundColor Red
} else {
    foreach ($p in $availablePorts) {
        Write-Host "  -> Port: $p" -ForegroundColor Green
    }
}

# 2. Inspect PnP USB and Bluetooth Hardware
Write-Host "`n[2/3] Hardware Device Inventory:" -ForegroundColor Yellow
$pnpPorts = Get-PnpDevice -Class 'Ports' -ErrorAction SilentlyContinue | Select-Object InstanceId, FriendlyName, Status
foreach ($dev in $pnpPorts) {
    Write-Host "  -> $($dev.FriendlyName) [Status: $($dev.Status)]" -ForegroundColor White
}

# 3. Test OBD Interface on available ports
Write-Host "`n[3/3] Probing OBD Communication on Ports..." -ForegroundColor Yellow
$baudRates = @(38400, 9600, 115200)
$commands = @("ATZ`r", "ATE0`r", "ATL0`r", "0100`r")

foreach ($portName in $availablePorts) {
    Write-Host "`nTesting Port: $portName" -ForegroundColor Cyan
    $portWorking = $false

    foreach ($baud in $baudRates) {
        Write-Host "  -> Probing Baud Rate: $baud..." -ForegroundColor Gray
        $port = $null
        try {
            $port = New-Object System.IO.Ports.SerialPort $portName, $baud, [System.IO.Ports.Parity]::None, 8, [System.IO.Ports.StopBits]::One
            $port.ReadTimeout = 1500
            $port.WriteTimeout = 1500
            $port.DtrEnable = $true
            $port.RtsEnable = $true
            $port.Open()

            # Send reset command
            $port.DiscardInBuffer()
            $port.DiscardOutBuffer()
            $port.Write("ATZ`r")
            Start-Sleep -Milliseconds 500

            $response = ""
            $readBytes = $port.BytesToRead
            if ($readBytes -gt 0) {
                $buf = New-Object byte[] $readBytes
                $port.Read($buf, 0, $readBytes) | Out-Null
                $response = [System.Text.Encoding]::ASCII.GetString($buf)
            }

            if ($response.Length -gt 0) {
                Write-Host "    [SUCCESS] Response at $baud baud: $($response.Trim())" -ForegroundColor Green
                $portWorking = $true
                break
            } else {
                Write-Host "    [NO RESPONSE] at $baud baud." -ForegroundColor DarkGray
            }
        } catch {
            Write-Host "    [PORT ERROR] $($_.Exception.Message)" -ForegroundColor DarkGray
        } finally {
            if ($port -and $port.IsOpen) {
                $port.Close()
            }
        }
    }

    if (-not $portWorking) {
        Write-Host "  -> $portName: No active ELM/OBD response or device in sleep/pairing mode." -ForegroundColor Yellow
    }
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " Hardware Diagnostic Scan Complete." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
