$ErrorActionPreference = 'Continue'
Set-Location "C:\Users\Thiago Brito\Documents\Claude_Code\GymTracker"
$log = "build\build-install.log"
if (Test-Path $log) { Remove-Item $log }
.\gradlew.bat :app:assembleDebug --console=plain *> $log
if ($LASTEXITCODE -eq 0) {
  $adb = "C:\Android\Sdk\platform-tools\adb.exe"
  & $adb install -r app\build\outputs\apk\debug\app-debug.apk *>> $log
  & $adb shell monkey -p com.vitoroliveira.gymtracker -c android.intent.category.LAUNCHER 1 *>> $log
}
"FIM exit=$LASTEXITCODE" | Add-Content $log
