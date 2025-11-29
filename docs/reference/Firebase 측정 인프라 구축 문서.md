# Firebase 연동 및 LTV 측정 인프라 구축 가이드 (비밀키 노출 방지 우선)

이 문서는 Android 앱에 Firebase(GA4 포함)와 AdMob 수익 이벤트를 안전하게 연동하는 단계별 가이드입니다. 특히 google-services.json, 서비스 계정 키 등 민감 키의 노출을 방지하고 Debug/Release 빌드 검증 워크플로우를 명확히 하는 데 초점을 둡니다.

핵심 원칙
- 민감 파일(google-services-*.json, 서비스 계정 키 등)은 절대 소스 저장소에 커밋하지 않습니다.
- Debug(개발)와 Release(프로덕션) Firebase 프로젝트를 분리합니다.
- CI/CD에 암호화된 시크릿을 등록하고, 빌드 시 필요한 파일을 주입합니다.
- 릴리즈 바이너리는 먼저 Debug Firebase(테스트 프로젝트)로 이벤트 전송을 검증한 뒤 Prod로 연결해 배포합니다.

1) 프로젝트 설계
- 큰 그림: 앱이 10개 이상일 경우 "앱별 프로젝트"를 모두 생성하면 관리·비용·권한 이슈가 커집니다. 대신 환경(env) 기준(Dev/Prod)으로 프로젝트를 분리하고 각 프로젝트에 여러 앱을 등록하는 방식을 권장합니다.

- 선택지 비교
  옵션 A) 앱별 프로젝트 (앱×환경 → 프로젝트 수 증가)
    - 장점: 앱 간 완전한 데이터/권한 격리, 각 앱별 IAM·결제 분리 가능
    - 단점: 프로젝트 수(및 관리 부담) 급증, 키·서비스 계정 관리 복잡, AdMob/Analytics 연결 관리 증가
    - 권장 케이스: 규제·법적 요구로 앱별 완전 분리가 필요하거나 각 앱마다 다른 조직 단위가 책임질 때

  옵션 B) 환경별 단일 프로젝트(권장: 다수 앱)
    - 구조: sweetapps-prod, sweetapps-dev 같은 환경별 프로젝트를 만들고, 각 프로젝트에 앱(안드로이드 앱 등록)을 다수 등록
    - 장점: 프로젝트 수 감소(관리 용이), 시크릿/CI 파이프라인 단순화, 공통 정책·권한 적용 쉬움
    - 단점: 앱 간 데이터 격리 수준은 낮아짐(필요 시 앱별 필터링 및 BigQuery 분리 권장)
    - 권장 케이스: 앱이 많고 조직 내에서 공통 운영 정책·접근 제어가 가능한 경우

- sweetApps 권장 네이밍(환경별 단일 프로젝트 방식 권장)
  - 프로젝트 ID 예: sweetapps-prod, sweetapps-dev
  - 콘솔 표시명 예: "sweetApps (Prod)", "sweetApps (Dev)"
  - 각 앱은 동일 프로젝트 내에서 앱 등록: com.sweetapps.app1, com.sweetapps.app2, ...

- GA4 / Firebase 연동 관점
  - 동일 프로젝트 내 여러 앱은 각각의 Firebase 앱 인스턴스(google-services.json)를 가집니다. 즉 앱 당 google-services.json은 필요하지만 프로젝트는 공유됩니다.
  - GA4: 하나의 Firebase 프로젝트에 연결된 단일 GA4 Property를 사용하거나(프로젝트-Property 1:1), 앱별로 별도 Property를 만들고 프로젝트에 연결하는 구성도 가능(복잡성 증가). 대부분은 환경별 Property(프로젝트당 1개)를 사용하고 앱별로 stream을 구분하는 방식으로 충분합니다.

- AdMob 연동 주의
  - AdMob 앱 등록과 Play Console 매핑은 앱 단위이므로 App-level 설정은 그대로 유지됩니다.
  - ad_revenue 이벤트 전송 시 이벤트 내에 앱 식별자(또는 Firebase가 제공하는 app_instance_id 등)를 포함해 어떤 앱에서 발생했는지 명확히 하십시오.

- 보안·CI 영향
  - 환경별 프로젝트 방식이면 CI 시크릿(google-services-<앱>-dev.json 등) 관리가 쉬워집니다. 각 앱의 google-services.json은 app/src/<variant>-<app>/ 또는 앱별 디렉터리 구조로 CI에서 주입하면 됩니다.
  - 앱 간 접근 제어가 필요하면 Firebase 콘솔/BigQuery Export 시 앱별 데이터셋 분리 또는 BigQuery에 수집 후 앱별 뷰를 만들어 권한을 제어하십시오.

- 권장 요약
  - 앱이 10개 이상이면 기본적으로 옵션 B(환경별 단일 프로젝트에 다수 앱 등록)를 권장합니다.
  - 단, 법적 요구나 고객 요구로 완전 분리가 필요한 특정 앱은 별도 프로젝트로 분리하는 하이브리드 방식도 고려하세요.

2) 파일 배치 권장 및 .gitignore
- 권장 파일 위치:
  - app/src/debug/google-services.json
  - app/src/release/google-services.json
- .gitignore 예시(반드시 적용):
  app/src/*/google-services.json
  **/google-services-*.json
  **/service-account-*.json
- 실수로 커밋한 경우 바로 키 무효화 및 커밋 히스토리에서 제거(도구 사용 권장).

3) 안전한 비밀 관리(권장 방법)
- CI(예: GitHub Actions, Azure DevOps, GitLab CI)에 google-services.json 파일을 시크릿(암호화)으로 저장합니다.
- 빌드 파이프라인에서 시크릿을 복원해 app/src/<variant>/ 위치에 파일을 생성합니다.
- 선택사항: Firebase CLI 또는 Firebase Management API로 서비스 계정(최소 권한)으로 빌드 시 동적 다운로드. 서비스 계정 키도 CI 시크릿으로 관리.

4) 빌드·배포 흐름(권장)
- 단계 1: CI가 debug json을 app/src/debug/google-services.json으로 풀어서 릴리즈 서명으로 릴리즈 APK/AAB 생성(단, Firebase는 Debug 프로젝트로 설정)
- 단계 2: 생성한 릴리즈 바이너리를 내부 테스트 트랙이나 테스트 디바이스에 배포
- 단계 3: Firebase DebugView/Analytics로 핵심 이벤트(ad_revenue, start_timer 등) 전송 여부 확인
- 단계 4: 검증 통과 시 CI가 release json을 app/src/release/google-services.json으로 풀어 최종 배포 빌드 생성 및 Play Console 업로드

5) 릴리즈 빌드 검증 워크플로우 (필수)
- 이유: 릴리즈 빌드는 난독화/빌드 옵션으로 디버그와 동작이 달라질 수 있음.
- 권장 절차:
  1. 릴리즈 서명으로 빌드 생성(google-services.json은 Debug 프로젝트 것 사용)
  2. 내부 테스트 디바이스/트랙에 배포
  3. DebugView 또는 로그로 핵심 이벤트 정상 전송 확인
  4. 이상 없을 때만 Release json을 주입하여 실제 배포

6) 이벤트 구현 규칙
- ad_revenue
  - AdMob의 PaidEventListener로부터 value(실제 예상 수익)와 currency를 수집해 Firebase로 전송
  - 운영 데이터와 테스트 데이터를 분리: 릴리즈 바이너리에서만 실제 수익을 수집하도록 코드 플래그로 제어하되, 검증 과정에서 릴리즈 바이너리를 Debug 프로젝트로 확인 가능해야 함
- ad_impression
  - ad_type 파라미터 포함(예: banner, interstitial, rewarded)
- start_timer
  - target_days 파라미터 포함
- 개인정보(PII)는 이벤트 파라미터로 전송 금지(이메일, 전화번호, 기기 고유 식별자 등)

7) 난독화(R8/ProGuard) 주의
- Firebase 및 AdMob SDK 관련 클래스가 난독화로 제거되지 않도록 권장 keep 규칙을 적용
- 릴리즈에서 로그레벨을 낮춰 민감 정보 로그가 유출되지 않도록 함

8) CI 예시 흐름(간단)
- CI 시크릿 등록: google_services_debug, google_services_release, signing_key 등
- 빌드 스크립트 예시 흐름:
  1. 시크릿에서 debug json 추출 -> app/src/debug/google-services.json 생성
  2. ./gradlew assembleRelease (릴리즈 서명 키는 시크릿으로 관리)
  3. 내부 테스트 트랙 업로드 및 DebugView로 확인
  4. 검증 완료 시 시크릿에서 release json 추출 -> app/src/release/google-services.json 생성
  5. 최종 빌드 생성 및 Play Console 업로드

9) 권한 관리 및 키 롤링
- Firebase 콘솔과 GCP IAM에서 최소 권한 원칙 적용(개발자는 Debug 접근만, 마케팅/광고 담당자는 Release 접근권한 부여)
- 서비스 계정 키는 주기적으로 교체(롤링)하고 불필요한 권한은 제거

10) 배포 전 체크리스트
- [ ] google-services.json 파일이 저장소에 커밋되어 있지 않음
- [ ] CI 시크릿에 Prod/Debug json과 서명키가 안전하게 저장됨
- [ ] 릴리즈 바이너리를 Debug Firebase로 검증(핵심 이벤트 정상 수신)
- [ ] R8/ProGuard 규칙 적용 및 릴리즈 동작 검증
- [ ] 이벤트에 PII 미포함

부록: 문제 발생 시 대처
- 이벤트 미전송 시
  1. DebugView 활성화 및 Logcat 확인
  2. gmp_app_id 등 google-services 설정 확인
  3. 광고 SDK 초기화 시점 확인
- google-services.json 실수 커밋 시
  1. 즉시 해당 프로젝트의 키/설정 무효화
  2. 커밋 히스토리에서 파일 제거(필요 시 bfg/git-filter-repo 사용)

끝. 이 문서는 조직의 보안·운영 정책에 맞게 조정해 적용하십시오.
