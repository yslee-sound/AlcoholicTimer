간단 사용 설명서 — UMP 테스트 기기 해시

목적
- GDPR(UMP) 테스트를 위해 디버그 기기에서 맞춤형 광고/비동의 흐름을 재현합니다.

요약(한 줄)
- 디버그 빌드에서만 local.properties의 UMP_TEST_DEVICE_HASH 값을 사용합니다.
- 릴리즈 빌드에는 자동으로 빈값이 되므로 유출 걱정 없음.

중요 원칙
- 절대 테스트 해시를 소스코드에 하드코딩하지 마세요.
- 테스트 해시는 반드시 local.properties에만 넣고 .gitignore에 포함되어야 합니다.

1) local.properties에 해시 넣기 (예)
- 파일: 프로젝트 루트의 local.properties

예: (PowerShell에서 한 줄씩 입력)
setx UMP_TEST_DEVICE_HASH "B3EEABB8EE11C2BE770B684D95219ECB"

설명: 위 예시는 로컬에만 적용되는 값입니다. 실제로는 텍스트 편집기로 local.properties에 다음 줄을 추가하면 됩니다:
UMP_TEST_DEVICE_HASH=B3EEABB8EE11C2BE770B684D95219ECB

2) 테스트 해시 찾는 방법 (간단)
- 1) 디버그 APK 설치 후 기기에서 앱을 실행합니다.

- 2) 터미널에서 다음 명령으로 로그를 검색합니다 (기기 ID는 항상 -s emulator-5554 사용):
adb -s emulator-5554 logcat -v time | findstr "addTestDeviceHashedId"

- 3) 로그에 다음과 같은 줄이 나오면 "<해시>" 부분을 복사합니다:
D/SomeTag: addTestDeviceHashedId("44A19A7AB27DC2FEEC73259C8D892E01")

- 4) 복사한 해시를 local.properties의 UMP_TEST_DEVICE_HASH에 넣습니다.

3) 코드에서 해시 사용 방식
- build.gradle.kts (app 모듈)에서 local.properties를 읽어 debug 빌드에만
  buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"$debugUmpTestDeviceHash\"") 로 주입합니다.
- 앱 코드에서는 BuildConfig.UMP_TEST_DEVICE_HASH를 읽어 testDeviceIds 목록에 추가합니다.

4) 릴리즈 전 체크
- 릴리즈 빌드에서는 위 변수가 빈값으로 강제됩니다(gradle 스크립트에 로직 포함).
- 릴리즈 전 local.properties에 테스트 해시를 남겨도 빌드 스크립트가 릴리즈 태스크 요청 시 빈값으로 교체합니다.
- 그러나 안전을 위해 릴리즈 전에 local.properties에서 UMP_TEST_DEVICE_HASH를 제거하거나 빈값으로 바꾸세요.

5) Tab05 / Debug 메뉴 관련(현재 적용된 상태)
- Tab05에는 "Personalized Ads" 스위치가 있어 UMP 동의 창을 다시 띄웁니다.
- Debug Menu에는 "맞춤형 광고 재설정" 버튼이 있어 UMP 동의 상태를 초기화합니다.
- 두 기능은 디버그 환경에서만 사용되며, 동작은 앱의 UmpConsentManager/AdsUmpConsentManager와 연동됩니다.

6) 빠른 디버그 검사 절차
- 1) local.properties에 UMP_TEST_DEVICE_HASH 값을 넣습니다.
- 2) PowerShell에서 한 줄씩 명령 실행:
./gradlew.bat clean

.
:app:assembleDebug

.
:app:installDebug

- 3) 앱 실행 후 로그에서 AppOpen 관련 로그 확인 (항상 -s emulator-5554 사용):
adb -s emulator-5554 logcat -d -v time | findstr AppOpenAdManager

성공 기준 (간단)
- 로그에 "preload: loading unit=..." 가 나오고
- "onAdLoaded app-open" 로그가 나오며
- "AppOpen onAdShowedFullScreenContent" 또는 앱에서 실제 광고가 보이면 성공입니다.

문제가 보이면
- App Open 단위 ID가 올바른지 확인하세요. (App Open 전용 단위 ID여야 함)
- UMP 동의가 수집되어 canRequestAds=true 상태인지 확인하세요.

보안 주의
- UMP 테스트 해시, 개인 키 등은 절대 원격 저장소에 올리지 마세요.
- local.properties는 로컬 전용이며 반드시 .gitignore에 포함되어야 합니다.

끝.

