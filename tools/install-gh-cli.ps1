[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$release = Invoke-RestMethod -Uri 'https://api.github.com/repos/cli/cli/releases/latest' -Headers @{ 'User-Agent' = 'PowerShell' }
$asset = $release.assets | Where-Object { $_.name -match 'windows_amd64\.zip$' } | Select-Object -First 1
if (-not $asset) {
    throw 'No windows_amd64.zip asset found in latest gh release.'
}

$toolsDir = Join-Path $env:USERPROFILE 'tools'
$installRoot = Join-Path $toolsDir 'gh-cli'
New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
if (Test-Path $installRoot) {
    Remove-Item -Recurse -Force $installRoot
}
New-Item -ItemType Directory -Force -Path $installRoot | Out-Null

$zipPath = Join-Path $toolsDir $asset.name
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zipPath -UseBasicParsing
Expand-Archive -Path $zipPath -DestinationPath $installRoot -Force

$ghExe = Get-ChildItem -Path $installRoot -Recurse -Filter gh.exe | Select-Object -First 1
if (-not $ghExe) {
    throw 'gh.exe not found after extraction.'
}

$ghBin = Split-Path $ghExe.FullName -Parent
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
if ([string]::IsNullOrWhiteSpace($userPath)) {
    $userPath = ''
}
$parts = $userPath.Split(';', [System.StringSplitOptions]::RemoveEmptyEntries)
if ($parts -notcontains $ghBin) {
    $newPath = (($parts + $ghBin) -join ';').Trim(';')
    [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
}

Write-Output "GH_BIN=$ghBin"
& $ghExe.FullName --version
