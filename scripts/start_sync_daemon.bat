@echo off
REM Start Forge 4-Way Sync Daemon in background
cd /d "%~dp0\.."
start /B node scripts/auto_sync.js
echo ForgeDiagnostics 2-Way Sync Daemon started in background.
