$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$stateDir = Join-Path $root ".local"
$pidFile = Join-Path $stateDir "local-pids.txt"

if (!(Test-Path $pidFile)) {
  Write-Host "No local service PID record found."
  exit 0
}

Get-Content $pidFile | ForEach-Object {
  $parts = $_ -split "=", 2
  if ($parts.Length -ne 2) {
    return
  }

  $name = $parts[0]
  $processId = [int]$parts[1]
  $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
  if ($process) {
    Stop-Process -Id $processId -Force
    Write-Host "Stopped $name service: $processId"
  }
}

Remove-Item -LiteralPath $pidFile -Force
Write-Host "Local services stopped."
