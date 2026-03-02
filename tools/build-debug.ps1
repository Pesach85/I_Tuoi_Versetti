[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

Set-Location (Split-Path -Parent $PSScriptRoot)
$logPath = Join-Path (Get-Location) 'debug_build_full.log'
if (Test-Path $logPath) {
    Remove-Item $logPath -Force
}

& .\gradlew.bat :app:assembleDebug --no-daemon --stacktrace --info *>&1 | Tee-Object -FilePath $logPath
$code = $LASTEXITCODE
Write-Host "EXIT_CODE=$code"
exit $code
