[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Set-Location (Split-Path -Parent $PSScriptRoot)
$root = Get-Location

$keystorePath = Join-Path $root 'release-key.jks'
$propsPath = Join-Path $root 'keystore.properties'
$outPath = Join-Path $root 'github-secrets.local.txt'

if (-not (Test-Path $keystorePath)) {
    throw "Keystore non trovato: $keystorePath"
}
if (-not (Test-Path $propsPath)) {
    throw "File non trovato: $propsPath"
}

$props = @{}
Get-Content $propsPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line -split '=', 2
        if ($parts.Count -eq 2) {
            $props[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
}

foreach ($required in @('storePassword','keyAlias','keyPassword')) {
    if (-not $props.ContainsKey($required) -or [string]::IsNullOrWhiteSpace($props[$required])) {
        throw "Chiave mancante in keystore.properties: $required"
    }
}

$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))

@(
    'Use these values in GitHub: Settings > Secrets and variables > Actions > New repository secret'
    ''
    "ANDROID_KEYSTORE_BASE64=$keystoreBase64"
    "ANDROID_STORE_PASSWORD=$($props['storePassword'])"
    "ANDROID_KEY_ALIAS=$($props['keyAlias'])"
    "ANDROID_KEY_PASSWORD=$($props['keyPassword'])"
) | Set-Content -Path $outPath -Encoding UTF8

try {
    Set-Clipboard -Value $keystoreBase64
    $clipboardMsg = ' (ANDROID_KEYSTORE_BASE64 copied to clipboard)'
} catch {
    $clipboardMsg = ''
}

Write-Output "OK: secrets exported to $outPath$clipboardMsg"
