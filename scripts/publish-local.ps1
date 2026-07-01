param(
  [switch]$UseMysql,
  [switch]$RestartLocal,
  [int]$TimeoutSeconds = 90
)

$ErrorActionPreference = "Stop"

function Test-PortOpen {
  param(
    [string]$HostName,
    [int]$Port
  )

  $client = [System.Net.Sockets.TcpClient]::new()
  try {
    $connect = $client.BeginConnect($HostName, $Port, $null, $null)
    if (-not $connect.AsyncWaitHandle.WaitOne(500)) {
      return $false
    }
    $client.EndConnect($connect)
    return $true
  } catch {
    return $false
  } finally {
    $client.Close()
  }
}

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

function Stop-ExistingTunnel {
  param([string]$PidFile)

  if (!(Test-Path $PidFile)) {
    return
  }

  Get-Content $PidFile | ForEach-Object {
    $parts = $_ -split "=", 2
    if ($parts.Length -ne 2 -or $parts[1] -notmatch "^\d+$") {
      return
    }

    Stop-ProcessTree -RootProcessId ([int]$parts[1])
  }
  Remove-Item -LiteralPath $PidFile -Force
}

function Resolve-Cloudflared {
  param(
    [string]$ToolsDir
  )

  $existing = Get-Command cloudflared -ErrorAction SilentlyContinue
  if ($existing) {
    return $existing.Source
  }

  New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
  $localExe = Join-Path $ToolsDir "cloudflared.exe"
  if (Test-Path $localExe) {
    return $localExe
  }

  $downloadUrl = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
  Write-Host "Downloading cloudflared from $downloadUrl"
  Invoke-WebRequest -Uri $downloadUrl -OutFile $localExe
  return $localExe
}

function Wait-PublicUrl {
  param(
    [string[]]$LogPaths,
    [int]$TimeoutSeconds
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    foreach ($logPath in $LogPaths) {
      if (!(Test-Path $logPath)) {
        continue
      }

      $content = Get-Content -Raw -Path $logPath
      if ([string]::IsNullOrWhiteSpace($content)) {
        continue
      }

      $match = [regex]::Match($content, "https://[-a-zA-Z0-9.]+\.trycloudflare\.com")
      if ($match.Success) {
        return $match.Value
      }
    }
    Start-Sleep -Seconds 1
  }

  throw "Public tunnel URL was not found within $TimeoutSeconds seconds. Check logs: $($LogPaths -join ', ')"
}

$root = Split-Path -Parent $PSScriptRoot
$stateDir = Join-Path $root ".local"
$logDir = Join-Path $stateDir "logs"
$toolsDir = Join-Path $stateDir "tools"
$localPidFile = Join-Path $stateDir "local-pids.txt"
$publicPidFile = Join-Path $stateDir "public-pids.txt"
$publicUrlFile = Join-Path $stateDir "public-url.txt"
$cloudflaredLog = Join-Path $logDir "cloudflared.log"
$cloudflaredErrorLog = Join-Path $logDir "cloudflared-error.log"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Stop-ExistingTunnel -PidFile $publicPidFile
if (Test-Path $publicUrlFile) {
  Remove-Item -LiteralPath $publicUrlFile -Force
}

if ($RestartLocal -and (Test-Path $localPidFile)) {
  & (Join-Path $PSScriptRoot "stop-local.ps1")
}

if (!(Test-Path $localPidFile)) {
  if ($UseMysql) {
    & (Join-Path $PSScriptRoot "start-local.ps1") -UseMysql
  } else {
    & (Join-Path $PSScriptRoot "start-local.ps1")
  }
} else {
  Write-Host "Local services already have a PID record. Reusing current services."
}

if (!(Test-PortOpen -HostName "127.0.0.1" -Port 5173)) {
  throw "Frontend is not reachable on http://127.0.0.1:5173"
}

$cloudflaredPath = Resolve-Cloudflared -ToolsDir $toolsDir
$tunnelCommand = "& '$cloudflaredPath' tunnel --url http://127.0.0.1:5173"

$tunnel = Start-Process powershell `
  -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $tunnelCommand `
  -WindowStyle Hidden `
  -RedirectStandardOutput $cloudflaredLog `
  -RedirectStandardError $cloudflaredErrorLog `
  -PassThru

"tunnel=$($tunnel.Id)" | Set-Content -Path $publicPidFile

Write-Host "Public tunnel is starting. Waiting for public URL..."
$publicUrl = Wait-PublicUrl -LogPaths @($cloudflaredErrorLog, $cloudflaredLog) -TimeoutSeconds $TimeoutSeconds
$publicUrl | Set-Content -Path $publicUrlFile

Write-Host "Public URL: $publicUrl"
Write-Host "URL file: $publicUrlFile"
Write-Host "Logs: $logDir"
Write-Host "Stop public service: .\scripts\stop-publish.ps1"
