[CmdletBinding()]
param(
    [string]$Repo = 'Pesach85/I_Tuoi_Versetti'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ghExe = Get-Command gh -ErrorAction SilentlyContinue
if (-not $ghExe) {
    $fallback = Get-ChildItem -Path (Join-Path $env:USERPROFILE 'tools\gh-cli') -Recurse -Filter gh.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $fallback) { throw 'gh non trovato' }
    $ghPath = $fallback.FullName
} else {
    $ghPath = $ghExe.Source
}

& $ghPath secret list --repo $Repo
