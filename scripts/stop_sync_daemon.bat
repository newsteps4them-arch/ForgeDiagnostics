@echo off
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*auto_sync.js*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Host ('Stopped auto_sync daemon (PID: ' + $_.ProcessId + ')') }"
