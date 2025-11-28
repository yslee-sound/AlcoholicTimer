# GitHub Actions Secrets 등록 및 사용 가이드

목적
- 프로덕션 Supabase / Firebase 키를 저장소에 하드코딩하지 않고 GitHub Actions Secrets로 안전하게 관리하고 빌드 시 주입하는 방법을 정리합니다.

중요 키(권장 등록 목록)
- Supabase
  - SUPABASE_URL            (예: https://<project>.supabase.co)
  - SUPABASE_KEY            (anon public key)
  - SUPABASE_SERVICE_ROLE_KEY  (service_role 키; 매우 민감)

- Firebase (Android)
  - FIREBASE_API_KEY
  - FIREBASE_APP_ID
  - FIREBASE_PROJECT_ID
  - FIREBASE_STORAGE_BUCKET
  - FIREBASE_MESSAGING_SENDER_ID
  - GOOGLE_SERVICES_JSON    (google-services.json 전체 내용을 하나의 시크릿으로 보관 가능)

UI에서 Secrets 등록하는 방법
1. 리포지토리 페이지 → Settings → Secrets and variables → Actions
2. 'New repository secret' 클릭
3. Name: 대문자 + 밑줄 규칙 사용 (예: SUPABASE_KEY)
4. Secret: 실제 값(토큰, JSON 전체 등)을 그대로 붙여넣기
5. Add secret 클릭

권장 네이밍 규칙
- 대문자, 밑줄 사용. 예: SUPABASE_URL, FIREBASE_API_KEY
- 파일 전체를 저장할 때는 접미사로 _JSON 사용: GOOGLE_SERVICES_JSON

워크플로에서 시크릿 사용하는 패턴
1) local.properties에 주입 (간단하고 기존 Gradle 설정 재사용 가능)
- 장점: 기존 build.gradle.kts가 local.properties에서 읽도록 구성되어 있다면 수정 최소화
- 예시 단계(워크플로의 job 내 steps에 추가):

Run step (local.properties에 쓰기):
  - name: Inject secrets
    run: |
      echo "supabase.url=${{ secrets.SUPABASE_URL }}" >> local.properties
      echo "supabase.key=${{ secrets.SUPABASE_KEY }}" >> local.properties
      echo "sdk.dir=${{ runner.tool_cache }}/android-sdk" >> local.properties  # 필요시

주의: echo로 민감값을 출력하지 마십시오. 위는 값을 파일에 쓰는 예시이며, 빌드 로그에 값이 남지 않도록 워크플로에서 출력을 금지하세요.

2) google-services.json 파일을 시크릿으로 보관하고 파일로 복원하기
- 직접 멀티라인 JSON을 Secrets에 넣을 수 있으나, 공백/개행 문제를 피하려면 Base64로 인코딩하여 저장한 뒤 워크플로에서 디코드하는 방법 권장

- 저장(로컬에서 예)
  cat app/src/release/google-services.json | base64 > encoded.txt
  # encoded.txt 내용을 Secrets(GOOGLE_SERVICES_JSON_B64)로 저장

- 워크플로에서 디코드하여 파일 생성
  - name: Restore google-services.json
    run: |
      echo "${{ secrets.GOOGLE_SERVICES_JSON_B64 }}" | base64 --decode > app/src/release/google-services.json

3) Gradle에 -P로 직접 전달
- 권장: 짧은 값(토큰) 전달 시 사용
  - name: Build
    run: ./gradlew assembleRelease -PsupabaseKey=${{ secrets.SUPABASE_KEY }}

Gradle(build.gradle.kts)에서 읽는 권장 패턴
- 우선 환경변수 → local.properties 순으로 읽도록 구현
- 예시(간단):
  // build.gradle.kts 내
  val localProperties = java.util.Properties()
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
      localProperties.load(java.io.FileInputStream(localPropertiesFile))
  }
  val supabaseKey = System.getenv("SUPABASE_KEY") ?: localProperties.getProperty("supabase.key") ?: ""
  // buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")

보안 권장사항 (짧고 실무적)
- Secrets는 Organization 또는 Repository 수준에서 관리. 민감한 키(예: service_role)는 환경(Environments)으로 제한하여 승인 프로세스 적용 권장.
- 워크플로 로그에 시크릿 값 출력 금지(절대 echo, printf 등으로 출력하지 말 것). GitHub는 자동 마스킹하지만 안전을 위해 로그 출력하지 마세요.
- 멀티라인(예: google-services.json)은 base64로 인코딩하여 저장 및 디코드 권장.
- 시크릿 유출 의심 시 즉시 회전(rotate) 및 히스토리(커밋) 검사.
- 로컬 개발자는 local.properties에만 보관(.gitignore에 등록)하고 리포지토리에는 local.properties.template만 커밋.

추가 체크리스트
- [ ] repo Settings → Secrets에 위 키들 등록 완료
- [ ] workflow에서 secrets를 local.properties에 주입 또는 파일로 복원하는 단계 추가
- [ ] 빌드 로그에서 시크릿이 노출되지 않는지 확인
- [ ] 과거 커밋 히스토리에서 민감 키 노출 여부 스캔(필요시 회전 및 git-history 정리)

참고
- GitHub Docs: "Encrypted secrets for GitHub Actions"
- Supabase: 프로젝트 대시보드에서 anon key / service_role key 위치 확인
- Firebase: google-services.json 생성 방법(콘솔)

끝.
