# Instala o GymTracker no celular (cabo USB) e no relógio (depuração por Wi-Fi), no PC com Windows.
# Uso: clique com o botão direito > "Executar com o PowerShell", ou no terminal:
#   powershell -ExecutionPolicy Bypass -File tools\instalar_apks.ps1
# Antes: baixe e descompacte os dois artefatos DA MESMA execução do GitHub Actions
# (gymtracker-debug-apk e gymtracker-wear-apk) em qualquer pasta dentro de Downloads.
# Passo a passo completo em docs/RELOGIO.md.

param([string]$Pasta = "$env:USERPROFILE\Downloads")

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }
$pacote = "com.vitoroliveira.gymtracker"

function Achar-Apk($nome) {
    Get-ChildItem -Path $Pasta -Recurse -Filter $nome -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
}

function Instalar($serial, $apk, $onde) {
    Write-Host ""
    Write-Host "Instalando no $onde ($($apk.FullName))..." -ForegroundColor Cyan
    $saida = & $adb -s $serial install -r "$($apk.FullName)" 2>&1 | Out-String
    Write-Host $saida
    if ($saida -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE|signatures do not match") {
        Write-Host "O app instalado no $onde foi assinado com outra chave (outra execução do CI)." -ForegroundColor Yellow
        Write-Host "Para instalar, é preciso desinstalar o antigo. No celular isso apaga o que só existe nele" -ForegroundColor Yellow
        Write-Host "(agenda e tema; treinos, grupos, peso e medidas estão na nuvem)." -ForegroundColor Yellow
        $r = Read-Host "Desinstalar o antigo do $onde e instalar o novo? (s/n)"
        if ($r -eq "s") {
            & $adb -s $serial uninstall $pacote
            & $adb -s $serial install "$($apk.FullName)"
        }
    }
}

$appApk = Achar-Apk "app-debug.apk"
$wearApk = Achar-Apk "wear-debug.apk"
if (-not $appApk) { Write-Host "Não achei app-debug.apk em $Pasta (descompacte o gymtracker-debug-apk)." -ForegroundColor Red }
if (-not $wearApk) { Write-Host "Não achei wear-debug.apk em $Pasta (descompacte o gymtracker-wear-apk)." -ForegroundColor Red }
if ($appApk -and $wearApk -and ([Math]::Abs(($appApk.LastWriteTime - $wearApk.LastWriteTime).TotalHours) -gt 2)) {
    Write-Host "Atenção: os dois APKs parecem de execuções diferentes. Celular e relógio só conversam se vierem da mesma." -ForegroundColor Yellow
}

# ── celular (USB) ──
& $adb start-server | Out-Null
$celular = (& $adb devices | Select-String "\tdevice$" | ForEach-Object { ($_ -split "\t")[0] } | Where-Object { $_ -notmatch ":" } | Select-Object -First 1)
if ($appApk) {
    if ($celular) { Instalar $celular $appApk "celular" }
    else { Write-Host "Nenhum celular no cabo USB (confira a depuração USB). Pulando o celular." -ForegroundColor Yellow }
}

# ── relógio (Wi-Fi) ──
if ($wearApk) {
    Write-Host ""
    Write-Host "Relógio: Configurações > Opções do desenvolvedor > Depuração por Wi-Fi (relógio e PC no mesmo Wi-Fi)." -ForegroundColor Cyan
    $par = Read-Host "Se ainda não pareou: toque em 'Parear novo dispositivo' e digite o IP:porta de PAREAMENTO (Enter para pular)"
    if ($par) {
        $codigo = Read-Host "Código de 6 dígitos que aparece no relógio"
        & $adb pair $par $codigo
    }
    $con = Read-Host "IP:porta de CONEXÃO (a que aparece na tela da Depuração por Wi-Fi)"
    if ($con) {
        & $adb connect $con
        Instalar $con $wearApk "relógio"
        Write-Host ""
        Write-Host "Pronto. Abra o GymTracker no celular (entre na conta) e depois no relógio; toque em Sincronizar." -ForegroundColor Green
        Write-Host "Depois pode desligar a Depuração por Wi-Fi no relógio (gasta bateria)." -ForegroundColor Green
    }
}
Read-Host "Enter para fechar"
