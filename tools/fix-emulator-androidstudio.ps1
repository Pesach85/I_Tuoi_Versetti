[CmdletBinding()]
param(
  [string]$SdkPath = 'D:\Android\android-sdk',
  [switch]$SetUserEnv = $true
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Add-UserPathSegment([string]$segment) {
  $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
  if ([string]::IsNullOrWhiteSpace($userPath)) { $userPath = '' }

  $parts = $userPath.Split(';', [System.StringSplitOptions]::RemoveEmptyEntries)
  if ($parts -notcontains $segment) {
    $newPath = ($parts + $segment) -join ';'
    [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
    Write-Host "Added to USER Path: $segment"
  } else {
    Write-Host "Already in USER Path: $segment"
  }
}

Write-Host "== Android Emulator Fix =="
Write-Host "SDK path: $SdkPath"

$emulatorExe = Join-Path $SdkPath 'emulator\emulator.exe'
$adbExe = Join-Path $SdkPath 'platform-tools\adb.exe'

if (-not (Test-Path $emulatorExe)) {
  throw "emulator.exe non trovato in: $emulatorExe"
}

if ($SetUserEnv) {
  [Environment]::SetEnvironmentVariable('ANDROID_HOME', $SdkPath, 'User')
  [Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', $SdkPath, 'User')
  Write-Host 'Set USER env: ANDROID_HOME / ANDROID_SDK_ROOT'

  Add-UserPathSegment (Join-Path $SdkPath 'platform-tools')
  Add-UserPathSegment (Join-Path $SdkPath 'emulator')
}

Write-Host ''
Write-Host '--- Checks ---'
Write-Host "emulator exists: $((Test-Path $emulatorExe))"
Write-Host "adb exists:      $((Test-Path $adbExe))"

Write-Host ''
Write-Host 'Emulator version:'
& $emulatorExe -version | Select-Object -First 3

Write-Host ''
Write-Host 'Acceleration check:'
& $emulatorExe -accel-check

Write-Host ''
Write-Host 'Available AVDs:'
& $emulatorExe -list-avds

Write-Host ''
Write-Host '== Done =='
Write-Host 'Riavvia Android Studio dopo questo script.'
