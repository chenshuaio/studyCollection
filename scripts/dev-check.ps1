$ErrorActionPreference = "Stop"

if (!(Test-Path "backend/pom.xml")) { throw "Missing backend/pom.xml" }
if (!(Test-Path "backend/common-lib/pom.xml")) { throw "Missing backend/common-lib/pom.xml" }
if (!(Test-Path "frontend/package.json")) { throw "Missing frontend/package.json" }
if (!(Test-Path "frontend/src/main.ts")) { throw "Missing frontend/src/main.ts" }

Write-Host "Repository scaffold is present."
