param(
  [switch]$UseMysql
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

function Wait-LocalPort {
  param(
    [string]$Name,
    [int]$Port,
    [string]$LogPath,
    [int]$TimeoutSeconds = 120
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-PortOpen -HostName "127.0.0.1" -Port $Port) {
      Write-Host "$Name is ready on port $Port."
      return
    }
    Start-Sleep -Seconds 1
  }

  throw "$Name did not become ready on port $Port within $TimeoutSeconds seconds. Check log: $LogPath"
}

$root = Split-Path -Parent $PSScriptRoot
$stateDir = Join-Path $root ".local"
$logDir = Join-Path $stateDir "logs"
$pidFile = Join-Path $stateDir "local-pids.txt"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (Test-Path $pidFile) {
  Write-Host "Local services already have a PID record. Run scripts\stop-local.ps1 first."
  exit 1
}

$backendPath = Join-Path $root "backend"
$frontendPath = Join-Path $root "frontend"
$backendLog = Join-Path $logDir "backend.log"
$backendErrorLog = Join-Path $logDir "backend-error.log"
$frontendLog = Join-Path $logDir "frontend.log"
$frontendErrorLog = Join-Path $logDir "frontend-error.log"

$mysqlEnv = ""
if ($UseMysql) {
  $mysqlEnv = "`$env:SPRING_PROFILES_ACTIVE='local-mysql'; `$env:STUDY_COLLECTION_DB_USER='root'; `$env:STUDY_COLLECTION_DB_PASSWORD='root'; `$env:STUDY_COLLECTION_DB_URL='jdbc:mysql://127.0.0.1:3306/study_collection?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'; "
}

$backendCommand = "Set-Location -LiteralPath '$backendPath'; $mysqlEnv mvn -pl local-app -am -DskipTests install; if (`$LASTEXITCODE -eq 0) { mvn -f local-app/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.3.2:run }"
$frontendCommand = "Set-Location -LiteralPath '$frontendPath'; `$env:VITE_API_TARGET='http://127.0.0.1:18080'; npm.cmd run dev -- --host 127.0.0.1 --port 5173"

$backend = Start-Process powershell `
  -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $backendCommand `
  -WindowStyle Hidden `
  -RedirectStandardOutput $backendLog `
  -RedirectStandardError $backendErrorLog `
  -PassThru

$frontend = Start-Process powershell `
  -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $frontendCommand `
  -WindowStyle Hidden `
  -RedirectStandardOutput $frontendLog `
  -RedirectStandardError $frontendErrorLog `
  -PassThru

@(
  "backend=$($backend.Id)",
  "frontend=$($frontend.Id)"
) | Set-Content -Path $pidFile

Write-Host "Local services are starting. Waiting for ports to become ready..."
Wait-LocalPort -Name "Backend API" -Port 18080 -LogPath $backendLog
Wait-LocalPort -Name "Frontend" -Port 5173 -LogPath $frontendLog

Write-Host "Local services are ready:"
Write-Host "- Backend API: http://127.0.0.1:18080"
Write-Host "- Frontend: http://127.0.0.1:5173"
Write-Host "- Logs: $logDir"
if ($UseMysql) {
  Write-Host "- MySQL env: root/root @ 127.0.0.1:3306/study_collection"
}
Write-Host "Stop services: .\scripts\stop-local.ps1"
