[CmdletBinding()]
param(
    [string]$Repo = 'Pesach85/I_Tuoi_Versetti',
    [string]$Workflow = 'Android Signed AAB',
    [string]$Ref = 'master'
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

& $ghPath workflow run $Workflow --repo $Repo --ref $Ref
if ($LASTEXITCODE -ne 0) {
    throw 'Impossibile avviare il workflow: autenticazione mancante o errore gh.'
}
Write-Host "Workflow avviato: $Workflow su $Repo ($Ref)"
& $ghPath run list --repo $Repo --workflow $Workflow --limit 1
if ($LASTEXITCODE -ne 0) {
    throw 'Workflow avviato ma impossibile leggere run list da gh.'
}
