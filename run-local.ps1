# ── run-local.ps1 ────────────────────────────────────────────────────────────
# Starts the Spring Boot backend using the local dev profile.
# Credentials are read from:  src/main/resources/application-local.properties
#
# Usage (from project root or BackEnd folder):
#   .\run-local.ps1
# ─────────────────────────────────────────────────────────────────────────────

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $scriptDir

Write-Host ""
Write-Host "  Starting Web POS Backend (profile: local) ..." -ForegroundColor Cyan
Write-Host ""

mvn spring-boot:run "-Dspring-boot.run.profiles=local" --no-transfer-progress
