# AlcoholicTimer 릴리즈 빌드 스크립트
# 사용법: .\build_release.ps1

# 환경변수 설정
$env:KEYSTORE_PATH = "G:/secure/AlcoholicTimer_Secure/alcoholic-timer-upload.jks"  # 실제 경로로 변경
$env:KEYSTORE_STORE_PW = "your_keystore_password"  # 실제 비밀번호로 변경
$env:KEY_ALIAS = "alcoholictimeruploadkey"  # 실제 별칭으로 변경
$env:KEY_PASSWORD = "your_key_password"  # 실제 키 비밀번호로 변경

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "  AlcoholicTimer 릴리즈 빌드" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`n" -ForegroundColor Cyan

# 릴리즈 빌드 전 체크리스트
Write-Host "🔍 릴리즈 빌드 전 체크리스트" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

Write-Host "`n다음 항목들을 확인하셨습니까?" -ForegroundColor White

Write-Host "  1. VersionCode와 VersionName이 업데이트 되었습니까?" -ForegroundColor Cyan
Write-Host "     → app/build.gradle.kts 파일 확인" -ForegroundColor DarkGray

Write-Host "`n  2. CHANGELOG.md가 업데이트 되었습니까?" -ForegroundColor Cyan
Write-Host "     → 새로운 버전의 변경사항 기록 확인" -ForegroundColor DarkGray

Write-Host "`n  3. 디버그 기능이 릴리즈에서 비활성화되어 있습니까?" -ForegroundColor Cyan
Write-Host "     → DebugAdHelper 관련 코드에 BuildConfig.DEBUG 체크 확인" -ForegroundColor DarkGray
Write-Host "     → Gradle 태스크가 자동으로 검증합니다" -ForegroundColor DarkGray

Write-Host "`n  4. 광고 유닛 ID가 실제 ID로 설정되어 있습니까?" -ForegroundColor Cyan
Write-Host "     → 테스트 광고 ID가 아닌 실제 AdMob ID 확인" -ForegroundColor DarkGray

Write-Host "`n  5. 로그/디버그 메시지가 제거되었습니까?" -ForegroundColor Cyan
Write-Host "     → ProGuard가 자동으로 제거하지만 민감 정보 확인" -ForegroundColor DarkGray

Write-Host "`n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`n" -ForegroundColor Gray

$response = Read-Host "계속 진행하시겠습니까? (Y/N)"
if ($response -ne "Y" -and $response -ne "y") {
    Write-Host "`n❌ 빌드 취소됨" -ForegroundColor Red
    exit 0
}

Write-Host "`n환경변수 설정 확인..." -ForegroundColor Yellow
Write-Host "KEYSTORE_PATH: $env:KEYSTORE_PATH" -ForegroundColor Gray
Write-Host "KEY_ALIAS: $env:KEY_ALIAS" -ForegroundColor Gray

# 키스토어 파일 존재 확인
if (-not (Test-Path $env:KEYSTORE_PATH)) {
    Write-Host "`n❌ 오류: 키스토어 파일을 찾을 수 없습니다!" -ForegroundColor Red
    Write-Host "   경로: $env:KEYSTORE_PATH" -ForegroundColor Red
    Write-Host "`n💡 스크립트 상단의 KEYSTORE_PATH를 실제 경로로 수정하세요." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ 키스토어 파일 확인 완료`n" -ForegroundColor Green

# 릴리즈 빌드 실행
Write-Host "AAB 빌드 시작..." -ForegroundColor Yellow
.\gradlew.bat clean :app:bundleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "  빌드 성공! 🎉" -ForegroundColor Cyan
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`n" -ForegroundColor Cyan

    $aabPath = "app\build\outputs\bundle\release\app-release.aab"
    if (Test-Path $aabPath) {
        $size = (Get-Item $aabPath).Length / 1MB
        Write-Host "📦 AAB 파일 생성:" -ForegroundColor Green
        Write-Host "   위치: $aabPath" -ForegroundColor Gray
        Write-Host "   크기: $([math]::Round($size, 2)) MB`n" -ForegroundColor Gray
    }
} else {
    Write-Host "`n❌ 빌드 실패!" -ForegroundColor Red
    exit 1
}

