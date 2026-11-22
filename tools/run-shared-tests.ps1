$ErrorActionPreference = 'Continue'
Set-Location "C:\Users\Thiago Brito\Documents\Claude_Code\GymTracker"
$log = "C:\Users\Thiago Brito\Documents\Claude_Code\GymTracker\build\shared-tests.log"
if (Test-Path $log) { Remove-Item $log }
.\gradlew.bat :shared:testDebugUnitTest --console=plain *> $log
"FIM exit=$LASTEXITCODE" | Add-Content $log
