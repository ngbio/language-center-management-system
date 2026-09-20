$ErrorActionPreference = "Stop"

$jdkCandidates = @(
  (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName),
  "C:\Program Files\Android\Android Studio\jbr"
) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) }

$jdkHome = $jdkCandidates | Select-Object -First 1
if (-not $jdkHome) {
  throw "JDK 21 not found. Install Eclipse Temurin JDK 21 before building Android."
}

$env:JAVA_HOME = $jdkHome
npm.cmd run android:sync
if ($LASTEXITCODE -ne 0) {
  throw "Capacitor sync failed with exit code $LASTEXITCODE."
}

Push-Location (Join-Path $PSScriptRoot "..\android")
try {
  .\gradlew.bat assembleDebug
  if ($LASTEXITCODE -ne 0) {
    throw "Android build failed with exit code $LASTEXITCODE."
  }
} finally {
  Pop-Location
}
