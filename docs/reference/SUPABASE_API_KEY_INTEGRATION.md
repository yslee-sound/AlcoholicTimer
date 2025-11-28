목적

이 문서는 Supabase 프로젝트의 API 키를 앱에 안전하게 통합하는 방법을 단계별로 안내합니다. Android(앱)와 서버/CI 환경에서 키를 어떻게 보관·주입·회전할지, 위험 완화 방법과 검증 절차를 포함합니다.

배경 지식(간단)

- Supabase URL: 프로젝트 엔드포인트(공개해도 무방)
- anon key: 클라이언트용 공개 키(그러나 남용·요금 문제 우려 있음)
- service_role key: 전체 권한을 가진 비밀 키(절대 클라이언트에 포함하면 안 됨)
- RLS(행 수준 보안): 데이터 접근을 제한하는 권한 제어, 중요하지만 완전한 방어 수단은 아님

원칙 요약

1) service_role 키는 절대 클라이언트에 포함하지 말고 서버(또는 안전한 서버리스 함수)에서만 사용한다.
2) 키는 안전한 비밀 저장소에 보관한다(환경변수, GitHub/GitLab Secrets, Vault 등).
3) 로컬 개발자용 파일은 절대 버전관리/Git에 커밋하지 않도록 .gitignore에 추가한다.
4) 빌드 시점에만 CI가 비밀을 주입하고, 빌드 아티팩트에 시크릿이 포함되지 않게 검증한다.

권장 아키텍처(간단)

- 권장: 앱 -> 우리 서버(프록시) -> Supabase
  - 서버가 service_role로 민감 작업을 수행
  - 앱은 서버에 사용자 토큰(또는 익명 요청)을 보내고 서버가 Supabase에 중계
- 대체(간단 앱): 앱에서 직접 Supabase를 호출할 경우 anon 키만 사용하고 RLS로 엄격히 제한

구체적 절차

1) 사용처 파악
- 코드, docs, 빌드 스크립트, .github/workflows, 백업 번들 등 전체에서 Supabase URL/키 사용 위치를 검색한다.
- 예시(로컬에서 실행):
  - git grep -n 'supabase.key' || echo 'no hits'
  - git rev-list --objects --all | Select-String -Pattern 'supabase' || echo 'no hits'

2) 로컬 개발자 지침
- local.properties 또는 .env.local 같은 파일을 사용하되, 해당 파일은 .gitignore에 추가한다.
- 예시 .gitignore 항목: local.properties
- 로컬 파일 예시(local.properties):
  supabase.url=https://your-project.supabase.co
  supabase.key=your-anon-key-here

3) CI/빌드 파이프라인 설정
- GitHub Actions(GHA) 예시 요지:
  - 리포지터리 Secrets에 SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY(서버 전용) 등록
  - 워크플로우에서 secrets를 읽어 빌드 시 local.properties를 생성한다.
  - 절대: 시크릿을 레포지터리에 쓰거나 아티팩트로 업로드하지 않는다.

  간단한 GHA 작업 흐름 예시(개념):
  - name: Create local.properties
    run: |
      echo "supabase.url=${{ secrets.SUPABASE_URL }}" >> local.properties
      echo "supabase.key=${{ secrets.SUPABASE_ANON_KEY }}" >> local.properties
  - name: Build
    run: ./gradlew assembleRelease

4) 서버 프록시(권장)
- 목적: 민감 키(과금/데이터 수정 권한)를 서버에만 보관하고 클라이언트에 영향을 주지 않음
- 간단한 Node/Express 예시(개념):
  - POST /api/ads-config -> 서버는 supabase service_role로 필요한 쿼리 실행 후 클라이언트에 결과 반환

5) RLS 및 인증
- 모든 민감 테이블에 RLS 적용
- 서버는 사용자 권한을 확인하고 필요한 경우 service_role로 작업을 수행
- 클라이언트는 인증된 JWT(유저 토큰)로 RLS 정책을 만족시켜야 함

6) 키 회전(권장 순서)
- 준비 단계: 사용처 목록화(코드/CI/서버/문서/백업)
- 테스트: 새 키를 staging 환경(또는 CI 변수)에 적용하고 기능 검증
- 롤아웃: 서버/CI 먼저 새 키로 배포
- 검증: 로그와 에러 체크(401/403, 리퀘스트 실패 등)
- 폐기: 모든 시스템이 새 키로 정상 동작하면 기존 키 Revoke

긴급 회전 절차(키가 유출된 경우)
- 즉시: service_role 발견 시 즉시 회전(하지만 배포 전에 새 키를 서버에 먼저 적용)
- anon만 노출된 경우: 영향 범위를 검토(앱에 포함되어 있으면 재배포 필요성 고려)
- 모든 협업자에게 공지(SECURITY POSTCLEAN NOTICE 등)

검증 및 모니터링

- 코드/리포지터리 검증
  - git grep -n 'supabase.key' .
  - git grep -n 'supabase.url' .
- 빌드 아티팩트 검증
  - APK/AAB에서 키 문자열 검색(압축 해제 후 strings 검색)
- 운영 로그 모니터링
  - Supabase 콘솔의 요청 로그, 비정상 트래픽/요청 패턴 확인

안전 체크리스트(간단)

- [ ] service_role 키가 레포나 클라이언트에 없음
- [ ] local.properties와 supabase.key가 .gitignore에 추가됨
- [ ] GitHub/GitLab Secrets에 키 등록됨
- [ ] CI가 빌드 시점에만 키를 주입함
- [ ] 스테이징에서 새 키로 테스트함
- [ ] 키 회전 및 폐기 계획이 문서화됨

개발자용 빠른 명령(Windows PowerShell 한 줄씩, 각 줄 뒤에 빈 줄 포함)

Set-Location 'G:\Workspace\AlcoholicTimer'

git grep -n 'supabase.key' || Write-Output 'no hits'

git grep -n 'supabase.url' || Write-Output 'no hits'

Get-Content .\local.properties -Raw

주의사항

- 문서나 예시 파일에 실제 키(특히 service_role)를 포함하지 마세요. 항상 플레이스홀더를 사용하세요.
- anon 키는 ‘공개’라고 해도 남용·요금 관련 문제가 생길 수 있으니 로그와 사용량을 주시하세요.

참고 템플릿

- .gitignore 추가 예시:
  local.properties
  supabase.key
  local.properties.bak

- GitHub Actions secrets 사용 예시(요약):
  secrets.SUPABASE_URL
  secrets.SUPABASE_ANON_KEY
  secrets.SUPABASE_SERVICE_ROLE_KEY (서버 전용)

문의 및 다음 단계

- 이 문서를 기반으로 CI 워크플로우 교체 또는 .gitignore 추가, 민감 파일 제거 작업을 제가 대신 적용해 드릴 수 있습니다. 원하시면 한 줄로 알려 주세요: "CI 수정" 또는 "레포 정리" 또는 "문서만".
