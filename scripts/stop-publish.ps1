$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$stateDir = Join-Path $root ".local"
$publicPidFile = Join-Path $stateDir "public-pids.txt"
$publicUrlFile = Join-Path $stateDir "public-url.txt"

function Stop-ProcessTree {
  param([int]$RootProcessId)

  $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $RootProcessId" -ErrorAction SilentlyContinue
  foreach ($child in $children) {
    Stop-ProcessTree -RootProcessId ([int]$child.ProcessId)
  }

  $process = Get-Process -Id $RootProcessId -ErrorAction SilentlyContinue
  if ($process) {
    Stop-Process -Id $RootProcessId -Force -ErrorAction SilentlyContinue
  }
}

if (Test-Path $publicPidFile) {
  Get-Content $publicPidFile | ForEach-Object {
    $parts = $_ -split "=", 2
    if ($parts.Length -ne 2 -or $parts[1] -notmatch "^\d+$") {
      return
    }

    $processId = [int]$parts[1]
    if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
      Stop-ProcessTree -RootProcessId $processId
      Write-Host "Stopped public tunnel: $processId"
    }
  }
  Remove-Item -LiteralPath $publicPidFile -Force
} else {
  Write-Host "No public tunnel PID record found."
}

if (Test-Path $publicUrlFile) {
  Remove-Item -LiteralPath $publicUrlFile -Force
}

& (Join-Path $PSScriptRoot "stop-local.ps1")
