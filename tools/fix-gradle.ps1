[CmdletBinding()]
param(
  [string]$ProjectDir = (Get-Location).Path,
  [switch]$PurgeWrapper,
  [switch]$PurgeDaemonLogs,
  [switch]$Clean
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Find-GradleWrapperRoot([string]$dir) {
  $d = (Resolve-Path $dir).Path
  while ($true) {
    if (Test-Path (Join-Path $d "gradlew")) { return $d }
    $parent = Split-Path $d -Parent
    if ($parent -eq $d) { throw "gradlew non trovato risalendo da: $dir" }
    $d = $parent
  }
}

$root = Find-GradleWrapperRoot $ProjectDir
Set-Location $root
Write-Host "== Gradle fix in: $root =="

# Reset env JVM opts "fantasma" (solo per questa sessione)
foreach ($name in "GRADLE_OPTS","JAVA_TOOL_OPTIONS","JDK_JAVA_OPTIONS") {
  if (Test-Path "env:$name") { Remove-Item "env:$name" -ErrorAction SilentlyContinue }
}

# Stop daemon
& .\gradlew --stop | Out-Host

# Ricava versione Gradle dal wrapper (se presente)
$gradleVer = $null
$wrapperProps = Join-Path $root "gradle\wrapper\gradle-wrapper.properties"
if (Test-Path $wrapperProps) {
  $content = Get-Content $wrapperProps -Raw
  $m = [regex]::Match($content, 'distributionUrl=.*gradle-([0-9.]+)-bin\.zip')
  if ($m.Success) { $gradleVer = $m.Groups[1].Value }
}

# Pulizia mirata (opzionale)
if ($PurgeWrapper -and $gradleVer) {
  $distDir = Join-Path $env:USERPROFILE ".gradle\wrapper\dists"
  $target = Join-Path $distDir ("gradle-{0}-bin" -f $gradleVer)
  if (Test-Path $target) {
    Write-Host "Purging wrapper dist: $target"
    Remove-Item -Recurse -Force $target -ErrorAction SilentlyContinue
  }
}

if ($PurgeDaemonLogs -and $gradleVer) {
  $daemonDir = Join-Path $env:USERPROFILE (".gradle\daemon\{0}" -f $gradleVer)
  if (Test-Path $daemonDir) {
    Write-Host "Purging daemon logs: $daemonDir"
    Remove-Item -Recurse -Force $daemonDir -ErrorAction SilentlyContinue
  }
}

if ($Clean) {
  & .\gradlew --no-daemon --stacktrace clean | Out-Host
}

# Test import/build model
& .\gradlew --no-daemon --stacktrace --info tasks | Out-Host

Write-Host "== Done =="
