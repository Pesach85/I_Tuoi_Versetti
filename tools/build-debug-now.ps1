[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Set-Location (Split-Path -Parent $PSScriptRoot)
$root = Get-Location

$candidates = @(
    'C:\Program Files\Android\Android Studio\jbr',
    'C:\Program Files\Android\Android Studio\jre',
    'D:\Android\Android Studio\jbr',
    'D:\Android\android-studio\jbr',
    'D:\JDK_17'
)

$jdkHome = $null
foreach ($candidate in $candidates) {
    if (Test-Path (Join-Path $candidate 'bin\java.exe')) {
        $jdkHome = $candidate
        break
    }
}

if (-not $jdkHome) {
    throw 'Nessuna JDK valida trovata.'
}

$env:JAVA_HOME = $jdkHome
$env:Path = (Join-Path $jdkHome 'bin') + ';' + $env:Path

Write-Output "JAVA_HOME=$jdkHome"
& (Join-Path $jdkHome 'bin\java.exe') -version

$logPath = Join-Path $root 'debug_build_now.log'
if (Test-Path $logPath) {
    Remove-Item $logPath -Force
}

cmd /c "gradlew.bat :app:assembleDebug --no-daemon --stacktrace > debug_build_now.log 2>&1"
$code = $LASTEXITCODE

Write-Output ("BUILD_LOG=" + $logPath)

$apkPath = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if (Test-Path $apkPath) {
    $apk = Get-Item $apkPath
    Write-Output ("APK_PATH=" + $apk.FullName)
    Write-Output ("APK_LAST_WRITE=" + $apk.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))
}

exit $code
