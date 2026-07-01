$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$publishScript = Join-Path $PSScriptRoot "publish-local.ps1"
$stopPublishScript = Join-Path $PSScriptRoot "stop-publish.ps1"
$viteConfig = Join-Path $root "frontend\vite.config.ts"

function Assert-True {
  param(
    [bool]$Condition,
    [string]$Message
  )

  if (-not $Condition) {
    throw $Message
  }
}

function Assert-FileContains {
  param(
    [string]$Path,
    [string]$Pattern,
    [string]$Message
  )

  $content = Get-Content -Raw -Path $Path
  Assert-True -Condition ($content -match $Pattern) -Message $Message
}

function Assert-PowerShellParses {
  param([string]$Path)

  $tokens = $null
  $errors = $null
  [System.Management.Automation.Language.Parser]::ParseFile($Path, [ref]$tokens, [ref]$errors) | Out-Null
  Assert-True -Condition ($errors.Count -eq 0) -Message "PowerShell syntax errors in ${Path}: $($errors -join '; ')"
}

Assert-True -Condition (Test-Path $publishScript) -Message "Missing scripts\publish-local.ps1"
Assert-True -Condition (Test-Path $stopPublishScript) -Message "Missing scripts\stop-publish.ps1"

Assert-PowerShellParses -Path $publishScript
Assert-PowerShellParses -Path $stopPublishScript

Assert-FileContains -Path $publishScript -Pattern "cloudflared" -Message "publish-local.ps1 should use cloudflared"
Assert-FileContains -Path $publishScript -Pattern "https://github\.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64\.exe" -Message "publish-local.ps1 should use the official Cloudflare latest Windows amd64 download URL"
Assert-FileContains -Path $publishScript -Pattern "tunnel\s+--url\s+http://127\.0\.0\.1:5173" -Message "publish-local.ps1 should publish the frontend dev server"
Assert-FileContains -Path $publishScript -Pattern "public-url\.txt" -Message "publish-local.ps1 should write .local/public-url.txt"
Assert-FileContains -Path $publishScript -Pattern "\[string\]::IsNullOrWhiteSpace" -Message "publish-local.ps1 should tolerate empty tunnel log files while waiting for the URL"
Assert-FileContains -Path $stopPublishScript -Pattern "public-pids\.txt" -Message "stop-publish.ps1 should stop the public tunnel PID"
Assert-FileContains -Path $stopPublishScript -Pattern "stop-local\.ps1" -Message "stop-publish.ps1 should stop local services"
Assert-FileContains -Path $viteConfig -Pattern "allowedHosts\s*:\s*true" -Message "Vite dev server should allow tunnel host headers"

Write-Host "Publish script contract passed."
