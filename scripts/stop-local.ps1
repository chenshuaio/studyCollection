$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$stateDir = Join-Path $root ".local"
$pidFile = Join-Path $stateDir "local-pids.txt"

function Stop-ProcessTree {
  param([int]$RootProcessId)

  $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $RootProcessId" -ErrorAction SilentlyContinue
  foreach ($child in $children) {
    Stop-ProcessTree -RootProcessId ([int]$child.ProcessId)
  }

  $process = Get-Process -Id $RootProcessId -ErrorAction SilentlyContinue
  if ($process) {
    Stop-Process -Id $RootProcessId -Force
  }
}

function Stop-WorkspaceListener {
  param([int]$Port)

  $listeners = netstat -ano |
    Select-String "LISTENING" |
    Select-String ":$Port\s" |
    ForEach-Object { ($_.Line -split "\s+")[-1] } |
    Sort-Object -Unique

  foreach ($listenerPid in $listeners) {
    if ($listenerPid -notmatch "^\d+$" -or [int]$listenerPid -eq 0) {
      continue
    }

    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $listenerPid" -ErrorAction SilentlyContinue
    if ($processInfo -and $processInfo.CommandLine -like "*$root*") {
      Stop-ProcessTree -RootProcessId ([int]$listenerPid)
      Write-Host "Stopped workspace listener on port $Port: $listenerPid"
    }
  }
}

if (!(Test-Path $pidFile)) {
  Stop-WorkspaceListener -Port 18080
  Stop-WorkspaceListener -Port 5173
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
    Stop-ProcessTree -RootProcessId $processId
    Write-Host "Stopped $name service: $processId"
  }
}

Stop-WorkspaceListener -Port 18080
Stop-WorkspaceListener -Port 5173
Remove-Item -LiteralPath $pidFile -Force
Write-Host "Local services stopped."
