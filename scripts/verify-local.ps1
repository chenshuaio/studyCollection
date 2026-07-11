$ErrorActionPreference = "Stop"

Push-Location backend
mvn test
Pop-Location

Push-Location frontend
npm run typecheck
npm test
npm run build
Pop-Location

Write-Host "Local verification passed."
