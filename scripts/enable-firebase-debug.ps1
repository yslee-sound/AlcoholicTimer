Param(
    [string]$Serial = "",
    [switch]$Disable
)

$package = "kr.sweetapps.alcoholictimer.debug"

function Print-ErrorAndExit($msg){ Write-Error $msg; exit 1 }

# adb 확인
$adb = "adb"
try { & $adb version > $null 2>&1 } catch { Print-ErrorAndExit "adb가 PATH에 없습니다. Android SDK platform-tools 설치 후 adb가 PATH에 포함되었는지 확인하세요." }

# 연결된 디바이스 목록 조회
$raw = & adb devices 2>&1
$lines = $raw -split "\r?\n"
$devices = $lines | Select-Object -Skip 1 | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" } | ForEach-Object { ($_ -split "\t")[0] }

if ($Serial -eq "") {
    if ($devices.Count -eq 0) { Print-ErrorAndExit "연결된 디바이스가 없습니다. USB 연결 또는 에뮬레이터 실행 후 재시도하세요." }
    elseif ($devices.Count -gt 1) {
        Write-Host "여러 디바이스가 연결되어 있습니다. -Serial 파라미터로 지정하세요. 연결된 디바이스:"
        $devices | ForEach-Object { Write-Host " - $_" }
        exit 1
    } else {
        $Serial = $devices[0]
    }
}

$prop = if ($Disable) { ".none." } else { $package }
Write-Host "디바이스: $Serial -> debug.firebase.analytics.app = $prop"
Write-Host "실행: adb -s $Serial shell setprop debug.firebase.analytics.app $prop"
& adb -s $Serial shell setprop debug.firebase.analytics.app $prop
Write-Host "확인:"
& adb -s $Serial shell getprop debug.firebase.analytics.app

Write-Host "완료. Firebase DebugView에서 몇 초 내로 이벤트가 보이는지 확인하세요. 문제가 지속되면 Logcat에서 'AnalyticsManager' 로그를 확인하세요."
exit 0

