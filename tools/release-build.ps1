param(
    [switch]$SkipKeystoreGeneration
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

$root = Get-Location
$keystorePath = Join-Path $root 'release-key.jks'
$propsPath = Join-Path $root 'keystore.properties'
$logPath = Join-Path $root 'release_build.log'
$alias = 'ituoiversetti'

function New-RandomPassword([int]$length = 24) {
    $bytes = New-Object byte[] $length
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes).Replace('/','A').Replace('+','B').TrimEnd('=')
}

if (-not (Test-Path $propsPath) -and $SkipKeystoreGeneration) {
    throw 'keystore.properties non trovato e SkipKeystoreGeneration è attivo.'
}

if (-not (Test-Path $propsPath)) {
    $password = New-RandomPassword
    if (-not (Test-Path $keystorePath)) {
        & keytool -genkeypair -v -keystore $keystorePath -alias $alias -keyalg RSA -keysize 2048 -validity 10000 -storepass $password -keypass $password -dname "CN=I Tuoi Versetti, OU=Mobile, O=I Tuoi Versetti, L=Rome, S=RM, C=IT" | Out-Null
    }
    @(
        'storeFile=release-key.jks'
        "storePassword=$password"
        "keyAlias=$alias"
        "keyPassword=$password"
    ) | Set-Content -Path $propsPath -Encoding Ascii
}

if (-not (Test-Path $keystorePath)) {
    throw 'Keystore mancante: release-key.jks non trovato.'
}

Write-Output "[INFO] Avvio build signed bundle..."
cmd /c "gradlew.bat :app:bundleRelease --no-daemon --stacktrace > release_build.log 2>&1"
$exitCode = $LASTEXITCODE
Write-Output "[INFO] Exit code: $exitCode"
Write-Output "[INFO] Log: $logPath"

$bundlePath = Join-Path $root 'app\build\outputs\bundle\release\app-release.aab'
if (Test-Path $bundlePath) {
    Write-Output "[OK] AAB creato: $bundlePath"
    exit 0
}

Write-Output '[WARN] AAB non trovato. Controlla release_build.log per il dettaglio.'
exit $exitCode
