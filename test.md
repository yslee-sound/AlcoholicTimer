# AlcoholicTimer 테스트 가이드 (Windows/cmd.exe)

이 문서는 AlcoholicTimer 프로젝트의 로컬 단위 테스트를 실행하고, 결과 리포트를 확인하며, 자주 발생하는 이슈를 해결하는 방법을 정리합니다. 기본 쉘은 Windows의 cmd.exe를 기준으로 안내합니다.

---

## 1) 사전 준비
- Android Studio(Chipmunk+ 권장), JDK 11 이상
- 이 저장소의 루트 디렉터리: `G:\Workspace\AlcoholicTimer`
- Gradle Wrapper(동봉): `gradlew.bat` 사용

---

## 2) 빠른 시작: 테스트 한 번에 실행
프로젝트 루트로 이동한 뒤, 앱 모듈의 디버그 로컬 단위 테스트를 실행합니다.

```bat
cd /d G:\Workspace\AlcoholicTimer
gradlew.bat :app:testDebugUnitTest
```

- 모든 변형(variant)을 자동으로 실행하고 싶다면:
```bat
cd /d G:\Workspace\AlcoholicTimer
gradlew.bat test
```

강제 재실행(캐시/Up-to-date 무시)하려면 `--rerun-tasks` 옵션을 추가하세요.
```bat
cd /d G:\Workspace\AlcoholicTimer
gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

빌드를 깨끗하게 한 뒤 테스트하려면:
```bat
cd /d G:\Workspace\AlcoholicTimer
gradlew.bat clean :app:testDebugUnitTest
```

특정 테스트 클래스만 실행:
```bat
cd /d G:\Workspace\AlcoholicTimer
gradlew.bat :app:testDebugUnitTest --tests com.example.alcoholictimer.utils.DateOverlapUtilsTest
```

특정 테스트 메서드만 실행:
```bat
cd /d G:\Workspace\AlcoholicTimer
gradlew.bat :app:testDebugUnitTest --tests com.example.alcoholictimer.utils.DateOverlapUtilsTest.sumOfThreeRecords_inSeptember2025_equals_2_4_days
```

Release 변형에서 테스트 실행:
```bat
cd /d G:\Workspace\AlcoholicTimer
gradlew.bat :app:testReleaseUnitTest --rerun-tasks
```

---

## 3) 테스트 결과 리포트 위치
테스트가 성공적으로 실행되면 HTML 및 XML 리포트가 생성됩니다.

- HTML 리포트(브라우저로 열면 요약/상세 확인 가능)
  - Debug: `app/build/reports/tests/testDebugUnitTest/index.html`
  - Release: `app/build/reports/tests/testReleaseUnitTest/index.html`

- JUnit XML 결과(도구/CI가 읽는 원시 결과)
  - Debug: `app/build/test-results/testDebugUnitTest/`
  - Release: `app/build/test-results/testReleaseUnitTest/`

참고: 루트(`build/reports/...`) 아래의 `problems-report.html`은 Gradle 자체 리포트이며, “테스트 리포트”는 반드시 `app/build/reports/tests/...` 경로를 확인하세요.

---

## 4) Android Studio에서 실행/확인하는 법
- Project 뷰를 “Android”에서 “Project(또는 Project Files)”로 바꾸면 `app/src/test/java` 트리가 보입니다.
- “Android” 뷰에서도 `app > kotlin+java` 아래에 `test` 섹션이 나타날 수 있습니다. 안 보이면 상단 기어(⚙)에서 “Show Tests”를 활성화하고, Sync/Invalidate Caches 후 다시 확인하세요.
- 테스트 실행:
  - `app/src/test/java/com/example/alcoholictimer/utils/DateOverlapUtilsTest.kt` 파일을 열고, 클래스/메서드 옆 ▶️ 아이콘으로 Run.
  - 또는 Gradle Tool Window에서 `:app > verification > testDebugUnitTest` 실행.
- 실행 결과는 하단 Run/Test 탭에서 바로 확인 가능하며, HTML 리포트는 위 경로에서 브라우저로 볼 수 있습니다.

---

## 5) 현재 포함된 주요 테스트
`app/src/test/java/com/example/alcoholictimer/utils/DateOverlapUtilsTest.kt`

다음 시나리오를 검증합니다(기간 겹침 기반 계산이 정확한지 확인):
- `sumOfThreeRecords_inSeptember2025_equals_2_4_days`:
  - 9월 기록 0.5일 + 0.5일 + 1.4일 → 합계 2.4일인지 검증
- `monthStartBoundary_overlap_isTwoHours`:
  - 8/31 23:00 ~ 9/1 02:00 → 9월 내 겹침 2시간(= 2/24일)
- `monthEndBoundary_overlap_isHalfDay`:
  - 9/30 12:00 ~ 10/1 12:00 → 9월 내 겹침 12시간(= 0.5일)
- `noOverlap_returnsZero`:
  - 전혀 겹치지 않는 기간이면 0일 처리

해당 테스트들은 `DateOverlapUtils` 유틸의 `overlapDays`/`monthRange`/`ms`를 사용하여 경계 케이스까지 일관성 있게 검증합니다.

---

## 6) 자주 겪는 이슈와 해결
- 오류: `Directory 'G:\' does not contain a Gradle build.`
  - 원인: 프로젝트 루트가 아닌 디렉터리에서 명령 실행
  - 해결: 루트로 이동 후 실행
    ```bat
    cd /d G:\Workspace\AlcoholicTimer
    gradlew.bat :app:testDebugUnitTest
    ```
  - 또는 `-p` 옵션으로 프로젝트 경로 지정
    ```bat
    G:\Workspace\AlcoholicTimer\gradlew.bat -p G:\Workspace\AlcoholicTimer :app:testDebugUnitTest
    ```

- 테스트가 ‘up-to-date’로 스킵되어 리포트가 안 생김
  - 해결: `--rerun-tasks` 또는 `clean` 후 다시 실행
    ```bat
    gradlew.bat :app:testDebugUnitTest --rerun-tasks
    :: 또는
    gradlew.bat clean :app:testDebugUnitTest
    ```

- `test` 폴더가 Android 뷰에서 안 보임
  - Project 뷰를 “Project/Project Files”로 변경하거나, Android 뷰의 기어(⚙)에서 “Show Tests” 켜기
  - Gradle Sync, Invalidate Caches / Restart 수행

- 테스트는 통과했는데 리포트가 안 보임
  - 올바른 경로(`app/build/reports/tests/testDebugUnitTest/index.html`)를 확인
  - 다른 변형에서 실행했다면 해당 변형 경로를 확인(`testReleaseUnitTest` 등)

- Gradle Daemon 충돌/호환 이슈
  - `gradlew.bat --stop` 후 다시 실행

---

## 7) CI 연동(선택)
간단 예시(GitHub Actions 등):
```yaml
- name: Test (Debug)
  run: |
    cd G:/Workspace/AlcoholicTimer
    ./gradlew.bat :app:testDebugUnitTest --stacktrace --info
```
성공 시 아티팩트로 `app/build/reports/tests/testDebugUnitTest/` 폴더를 업로드하면, 웹에서 테스트 리포트를 바로 열어볼 수 있습니다.

---

## 8) 참고
- 테스트는 로컬 단위 테스트(에뮬레이터 불필요)입니다. UI 테스트(계측 테스트)는 `app/src/androidTest/java`에 위치하며, 별도 태스크(`connectedDebugAndroidTest` 등)를 사용합니다.
- 본 프로젝트의 월/주/년 통계는 “기간과 겹치는 시간만 일수로 환산”하는 규칙을 따릅니다. 해당 규칙이 변경되면 테스트도 함께 업데이트하세요.

---

문의/개선 제안은 이 문서나 테스트 클래스에 TODO 코멘트로 남겨 주세요.

