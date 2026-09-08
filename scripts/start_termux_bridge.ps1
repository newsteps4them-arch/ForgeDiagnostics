<#
.SYNOPSIS
  ForgeDiagnostics Termux Mobile Bridge Initializer
#>

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  📱 ForgeDiagnostics: Termux Android Mobile Dev Bridge" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan

$localIp = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notmatch 'Loopback|vEthernet' -and $_.IPAddress -notmatch '^169\.' } | Select-Object -First 1).IPAddress
$currentUser = $env:USERNAME

Write-Host "`n1. One-Line Setup for Termux on Android:" -ForegroundColor Yellow
Write-Host "curl -fsSL https://raw.githubusercontent.com/newsteps4them-arch/ForgeDiagnostics/main/scripts/setup_termux_antigravity.sh | bash" -ForegroundColor White

Write-Host "`n2. Direct SSH Connection to Desktop from Termux (if OpenSSH is enabled):" -ForegroundColor Yellow
Write-Host "ssh ${currentUser}@${localIp}" -ForegroundColor White

Write-Host "`n3. All edits made in Termux automatically sync back to main via GitHub." -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
