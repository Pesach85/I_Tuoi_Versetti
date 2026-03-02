[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$release = Invoke-RestMethod -Uri 'https://api.github.com/repos/cli/cli/releases/latest' -Headers @{ 'User-Agent' = 'PowerShell' }
$asset = $release.assets | Where-Object { $_.name -match 'windows_amd64\.msi$' } | Select-Object -First 1
if (-not $asset) {
    throw 'No windows_amd64.msi asset found in latest gh release.'
}

$toolsDir = Join-Path $env:USERPROFILE 'tools'
New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
$msiPath = Join-Path $toolsDir $asset.name

Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $msiPath -UseBasicParsing

$arguments = @('/i', '"' + $msiPath + '"', '/qn', '/norestart', 'MSIINSTALLPERUSER=1', 'ALLUSERS=2')
$proc = Start-Process -FilePath 'msiexec.exe' -ArgumentList $arguments -Wait -PassThru
if ($proc.ExitCode -ne 0) {
    throw "MSI install failed with exit code $($proc.ExitCode)"
}

$machineCandidates = @(
    'C:\Program Files\GitHub CLI\gh.exe',
    (Join-Path $env:LOCALAPPDATA 'Programs\GitHub CLI\gh.exe')
)
$found = $machineCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $found) {
    $found = (Get-ChildItem -Path 'C:\Program Files','C:\Users' -Filter gh.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
}
if (-not $found) {
    throw 'gh.exe not found after MSI install.'
}

Write-Output "GH_EXE=$found"
& $found --version
