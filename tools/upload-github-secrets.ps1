[CmdletBinding()]
param(
    [string]$Repo = 'Pesach85/I_Tuoi_Versetti',
    [string]$SecretsFile = 'github-secrets.local.txt'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Set-Location (Split-Path -Parent $PSScriptRoot)
$root = Get-Location
$secretsPath = Join-Path $root $SecretsFile

$ghExe = Get-Command gh -ErrorAction SilentlyContinue
if (-not $ghExe) {
    $fallback = Get-ChildItem -Path (Join-Path $env:USERPROFILE 'tools\gh-cli') -Recurse -Filter gh.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($fallback) {
        $ghPath = $fallback.FullName
    } else {
        throw 'GitHub CLI non trovato. Esegui prima tools/install-gh-cli.ps1'
    }
} else {
    $ghPath = $ghExe.Source
}

if (-not (Test-Path $secretsPath)) {
    throw "File segreti non trovato: $secretsPath"
}

function Invoke-Gh {
    param([string[]]$Args)
    & $ghPath @Args
    if ($LASTEXITCODE -ne 0) {
        throw "gh command failed: gh $($Args -join ' ')"
    }
}

$authOk = $true
try {
    Invoke-Gh @('auth','status','--hostname','github.com') | Out-Null
} catch {
    $authOk = $false
}

if (-not $authOk) {
    Write-Host 'Login GitHub richiesto. Si aprirà il browser...'
    Invoke-Gh @('auth','login','--hostname','github.com','--git-protocol','https','--web','--scopes','repo,read:org')
}

$pairs = @{}
Get-Content $secretsPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and ($line -match '^[A-Z0-9_]+=.*$')) {
        $parts = $line -split '=', 2
        if ($parts.Count -eq 2) {
            $pairs[$parts[0].Trim()] = $parts[1]
        }
    }
}

$required = @('ANDROID_KEYSTORE_BASE64','ANDROID_STORE_PASSWORD','ANDROID_KEY_ALIAS','ANDROID_KEY_PASSWORD')
foreach ($name in $required) {
    if (-not $pairs.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($pairs[$name])) {
        throw "Secret mancante in file: $name"
    }
}

foreach ($name in $required) {
    $value = $pairs[$name]
    $temp = [System.IO.Path]::GetTempFileName()
    try {
        [System.IO.File]::WriteAllText($temp, $value)
        Invoke-Gh @('secret','set',$name,'--repo',$Repo,'--body-file',$temp) | Out-Null
        Write-Host "OK secret caricato: $name"
    } finally {
        Remove-Item $temp -ErrorAction SilentlyContinue
    }
}

Write-Host "Completato: secrets caricati su $Repo"
