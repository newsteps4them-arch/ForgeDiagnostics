#!/usr/bin/env pwsh
# Forge Hardware Diagnostics & OBD Serial Probe Utility

Write-Host "Forge Agentic Diagnostics: OBD Hardware Diagnostic Probe" -ForegroundColor Cyan
$availablePorts = [System.IO.Ports.SerialPort]::GetPortNames()
Write-Host "`nDiscovered Serial Ports:" -ForegroundColor Yellow
if ($availablePorts.Count -eq 0) { Write-Host "  No COM ports detected in system." -ForegroundColor Red }
foreach ($p in $availablePorts) { Write-Host "  -> Port: $p" -ForegroundColor Green }

Write-Host "`nHardware Device Inventory:" -ForegroundColor Yellow
$pnpPorts = Get-PnpDevice -Class 'Ports' -ErrorAction SilentlyContinue | Select-Object InstanceId, FriendlyName, Status
foreach ($dev in $pnpPorts) { Write-Host "  -> $($dev.FriendlyName) [Status: $($dev.Status)]" -ForegroundColor White }

Write-Host "`nProbing OBD Communication on Ports..." -ForegroundColor Yellow
$baudRates = @(38400, 9600, 115200)
foreach ($portName in $availablePorts) {
    Write-Host "`nTesting Port: $portName" -ForegroundColor Cyan
    $portWorking = $false
    foreach ($baud in $baudRates) {
        Write-Host "  -> Probing Baud Rate: $baud..." -ForegroundColor Gray
        $port = $null
        try {
            $port = New-Object System.IO.Ports.SerialPort($portName, $baud, [System.IO.Ports.Parity]::None, 8, [System.IO.Ports.StopBits]::One)
            $port.ReadTimeout = 1200
            $port.WriteTimeout = 1200
            $port.DtrEnable = $true
            $port.RtsEnable = $true
            $port.Open()
            $port.DiscardInBuffer()
            $port.DiscardOutBuffer()
            $port.Write("ATZ`r")
            Start-Sleep -Milliseconds 700
            $response = ""
            $readBytes = $port.BytesToRead
            if ($readBytes -gt 0) {
                $buf = New-Object byte[] $readBytes
                $port.Read($buf, 0, $readBytes) | Out-Null
                $response = [System.Text.Encoding]::ASCII.GetString($buf)
            }
            if ($response -notmatch "ELM|STN|OBD") {
                Write-Host "    [NO ID] Reset did not identify an ELM/STN-compatible adapter." -ForegroundColor DarkGray
                continue
            }
            $port.Write("ATE0`r")
            Start-Sleep -Milliseconds 200
            $port.DiscardInBuffer()
            $port.Write("0100`r")
            Start-Sleep -Milliseconds 500
            $pidResponse = $port.ReadExisting()
            if ($pidResponse -match "41\s*00\s+[0-9A-F]{2}(\s+[0-9A-F]{2}){3}") {
                Write-Host "    [SUCCESS] $portName answered ELM/STN handshake and PID 0100 at $baud baud." -ForegroundColor Green
                $portWorking = $true
                break
            }
            Write-Host "    [NO PID] Adapter identified, but PID 0100 was not valid at $baud baud." -ForegroundColor DarkGray
        } catch { Write-Host "    [PORT ERROR] $($_.Exception.Message)" -ForegroundColor DarkGray }
        finally { if ($port -and $port.IsOpen) { $port.Close() } }
    }
    if (-not $portWorking) { Write-Host "  -> Port $portName - No active ELM/OBD response or device in sleep/pairing mode." -ForegroundColor Yellow }
}
Write-Host "`nHardware Diagnostic Scan Complete." -ForegroundColor Cyan
