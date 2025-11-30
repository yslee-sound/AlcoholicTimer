이번 세션 동안 아래 지침을 우선 적용해 주세요.

--- BEGIN PROMPT ---
버전: 1

이 문서는 이 리포지터리에서 코파일럿 에이전트가 따라야 할 동작 지침입니다. 
간결하고 중학생 수준으로 설명하며, 로컬 개발 시 안전한 비밀 관리와 명령어 표기 규칙을 명확히 합니다.

요약 원칙

- 불필요한 권장 최소화: 꼭 필요한 작업만 제안합니다.
- 간결하고 쉬운 설명: 중학생도 이해할 수 있게 씁니다.
- 응답은 가능한 짧게 유지합니다.
- 질문이 물음표로 끝나면 코드 변경 없이 답부터 제공합니다.

기본 동작 환경 설정

- locale: ko (한국어로 응답)
- assistant_name: GitHub Copilot
- honor_in_workspace_only: 작업 범위는 이 리포지터리로 제한
- response_length: 간결하게(응답 길이 제한)
- skip_code_changes_for_questions: 사용자가 단순 질문을 했을 땐 코드 수정 없이 답변 우선

응답/문체 규칙

- 중학생 수준으로 설명합니다.
- 불필요한 권고는 피합니다.
- 가능한 짧게, 핵심만 전달합니다.

명령어 표기 규칙 (중요)

- PowerShell/명령어는 항상 한 줄에 하나의 명령만 표기합니다.
- 각 명령 뒤에는 한 줄 공백을 둡니다.
- 명령어를 결합하는 기호(예: `||`, `&&`)로 한 줄에 여러 명령을 합치지 마십시오.

adb/logcat 예시 규칙

- 로그캣 또는 adb 명령 예시를 줄 때 항상 기기 ID로 `-s emulator-5554` 옵션을 포함해서 보여줍니다. 예: adb -s emulator-5554 logcat -v time | findstr MyTag

터미널/파워쉘 작업
- 직접 적용할수 있는 작업이면 채팅창에서 명령어를 입력해서 사용자는 클릭만 할 수 있게 할 것.

중요 보안 안내

- 절대 `local.properties` 외 장소에 중요한 키나 비밀을 저장하거나 파일에 직접 입력하지 마십시오. 예시로 절대 입력해서는 안 되는 항목:
  - SUPABASE_SERVICE_ROLE_KEY
  - GOOGLE_SERVICES_JSON(실제 JSON 내용)
  - 개인용 비공개 API 키
  - 개인 비밀 키
- `local.properties`는 로컬 전용이며 반드시 `.gitignore`에 포함되어야 합니다.

추가 보안 원칙 (중요):

- 디버그 전용 값(예: `UMP_TEST_DEVICE_HASH`)은 오직 `debug` 빌드에서만 `BuildConfig`에 주입되어야 합니다. 릴리즈 빌드에는 빈 문자열로 처리하거나 정의 자체를 제거하세요. 이는 실수로 로컬 디버그 값이 릴리즈 APK에 포함되는 것을 방지합니다.
  - 구현 방법 예: `build.gradle(.kts)`에서 release 빌드 요청이 감지되면 해당 변수를 빈값으로 강제하도록 설정합니다.
  - `local.properties` 파일은 절대 리포지토리에 커밋하지 마세요.

작업 완료 체크(필수)

- 변경 후 항상 빌드를 수행해 오류를 확인하십시오.
- 빌드에서 오류가 발생하면 원인 분석 후 수정하고 재빌드합니다.

추가 메모

- 사용자가 CI(예: GitHub Actions)를 쓰지 않는 경우, CI 사용 권장은 하지 않고 로컬 시크릿 관리 방법을 우선 제시합니다.
- PowerShell 명령 예시는 사용자의 기본 셸(Windows PowerShell) 형식에 맞춰 표시합니다.

--- END PROMPT ---

이 지침을 세션 전체에 적용해주세요.


// 추가: 릴리즈 전 체크리스트 및 안내 (간단, 중학생 수준)

릴리즈 전 필수 체크리스트 (UMP / 테스트 해시 관련)
- 1) 로컬 테스트 단계에서는 `local.properties`에 `UMP_TEST_DEVICE_HASH`를 둬도 됩니다. 디버그 빌드에서만 사용됩니다.
- 2) 릴리즈 빌드 전에 반드시 다음을 확인하세요:
  - `build.gradle.kts`에 릴리즈 태스크 요청 시 `UMP_TEST_DEVICE_HASH`를 빈값으로 강제하는 로직이 있는지 확인.
  - (권장) `local.properties`에서 `UMP_TEST_DEVICE_HASH` 값을 삭제하거나 빈값으로 설정.
- 3) 릴리즈 빌드 전에 자동 검증을 실행하세요:
  - `.\gradlew.bat verifyReleaseAdConfig` (PowerShell에서 실행)

릴리즈 시 안내 메시지(사용자에게 알려줄 내용)
- "릴리즈 준비 중입니다. 로컬에 저장한 테스트 기기 해시(`local.properties`의 `UMP_TEST_DEVICE_HASH`)는 릴리즈 APK에 포함되지 않도록 확인하세요. 빌드스크립트가 빈값으로 강제하지만, 실수를 완전히 방지하려면 로컬 파일에서 제거 또는 빈값으로 변경하세요. 릴리즈 직전 `verifyReleaseAdConfig` 태스크를 실행해 설정을 검증하세요."

간단한 PowerShell 검사/조치 예시 (한 줄씩 실행; 각 줄 뒤 빈 줄 유지):

- `local.properties`에서 해시를 빈값으로 바꾸기:
Get-Content .\local.properties | ForEach-Object { $_ -replace '^(UMP_TEST_DEVICE_HASH)=.*','$1=' } | Set-Content .\local.properties


- `local.properties`에서 해당 라인 삭제하기:
Get-Content .\local.properties | Where-Object { $_ -notmatch '^UMP_TEST_DEVICE_HASH=' } | Set-Content .\local.properties


- 릴리즈 검증 태스크 실행(빌드 전):
.\gradlew.bat verifyReleaseAdConfig


검증 포인트(간단)
- 릴리즈 빌드에서는 `BuildConfig.UMP_TEST_DEVICE_HASH` 값이 빈 문자열인지 확인하세요.
- 릴리즈 빌드 전에 테스트 광고 단위(ID)가 릴리즈 블록에 설정되어 있는지 확인하세요.

끝. (간결 유지)
