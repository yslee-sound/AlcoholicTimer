# 다국어화 체크리스트

> 📋 각 단계별로 완료 여부를 체크하며 진행하세요.

최종 수정: 2025-10-27

---

## 🎯 Phase 1: 영어 지원 (6주)

### ✅ Week 1-2: 코드 리팩터링

#### 문자열 리소스 마이그레이션
- [ ] 하드코딩 문자열 검색 완료 (`findstr` 실행)
- [ ] `values/strings.xml`에 모든 문자열 추가 (주석 포함)
- [ ] Kotlin 코드를 `stringResource(R.string.*)` 또는 `getString(R.string.*)` 변경
- [ ] 컴파일 성공 (`./gradlew assembleDebug`)
- [ ] 앱 실행 정상 (한국어 모드)

#### 날짜/숫자 포맷 유틸리티
- [ ] `LocaleFormatUtils.kt` 파일 생성
- [ ] `formatCurrency()` 메서드 구현
- [ ] `getDateFormat()` 메서드 구현
- [ ] `getTimeFormat()` 메서드 구현
- [ ] 기존 하드코딩 포맷 코드 모두 변경
- [ ] 단위 테스트 작성 및 통과 (`LocaleFormatUtilsTest`)

#### 레이아웃 유연성 확보
- [ ] 고정 너비(`width = XXX.dp`) 제거 → `weight()` 또는 `fillMaxWidth()` 사용
- [ ] 긴 텍스트 대비 `maxLines` + `TextOverflow.Ellipsis` 설정
- [ ] 최소 터치 영역 48dp 유지 확인

---

### ✅ Week 3-4: 영어 번역

#### values-en 디렉토리 생성
- [ ] `app/src/main/res/values-en/` 디렉토리 생성
- [ ] `values-en/strings.xml` 파일 생성

#### 초벌 번역
- [ ] DeepL/Google Translate로 초벌 번역 완료
- [ ] 모든 `<string>` 태그 번역 (약 80개)
- [ ] 컨텍스트 주석 확인 및 반영
- [ ] 빌드 오류 없음

#### 네이티브 검수
- [ ] 번역사 플랫폼(Upwork/Fiverr) 계정 생성
- [ ] 번역 검수 의뢰 ($25-30)
- [ ] 스크린샷 5장 + 용어집 제공
- [ ] 피드백 받아 `values-en/strings.xml` 최종 수정

#### Plurals & Format Strings
- [ ] Plurals 리소스 추가 (`days_count`, `records_count` 등)
- [ ] 한국어/영어 각각 `quantity="one"`, `quantity="other"` 정의
- [ ] 코드에서 `getQuantityString()` 사용
- [ ] 변수 순서 유연화 (`%1$d`, `%2$s` 활용)

---

### ✅ Week 5-6: 테스트 & 출시

#### 레이아웃 테스트
- [ ] 에뮬레이터 언어를 English로 변경
- [ ] **Run 화면** 레이아웃 검증 (Indicator 타이틀, Stat Chips)
- [ ] **Quit 화면** 다이얼로그 버튼 간격 확인
- [ ] **Detail 화면** 날짜 형식 "MMM d, yyyy" 확인
- [ ] **Records 화면** 빈 상태 메시지 표시
- [ ] **About 화면** License 정보 정렬

#### Pseudolocalization 테스트
- [ ] Settings → Languages → **English (XA)** 선택
- [ ] 모든 화면에서 텍스트 잘림 없는지 확인
- [ ] 버튼 겹침 없는지 확인
- [ ] 여백 적절한지 확인

#### 접근성 테스트
- [ ] **TalkBack** 활성화 (한국어/영어)
- [ ] 모든 버튼 Content Description 읽기 확인
- [ ] **큰 글씨** 모드 (200%) 레이아웃 확인
- [ ] **다크 모드**에서 색상 대비 확인

#### Lint 검사
- [ ] `./gradlew lintDebug` 실행
- [ ] **MissingTranslation** 경고 0건
- [ ] **UnusedResources** 확인 (사용 안 하는 문자열 삭제)
- [ ] 보고서 확인 (`app/build/reports/lint-results-debug.html`)

#### Play Store 메타데이터
- [ ] **짧은 설명** 작성 (한국어/영어, 80자 이내)
- [ ] **전체 설명** 작성 (한국어/영어, 500자+)
- [ ] **스크린샷** 5장 준비 (1080x1920, 영어 UI)
  - [ ] Run 화면
  - [ ] Records 화면 (주간 통계)
  - [ ] Detail 화면
  - [ ] All Records 화면
  - [ ] About 화면
- [ ] **Release Notes** 작성 (한국어/영어)

#### 버전 업데이트
- [ ] `app/build.gradle.kts`에서 `versionCode` 증가
- [ ] `versionName` = "1.2.0"
- [ ] `CHANGELOG.md` 업데이트

#### QA
- [ ] 한국어 모드 회귀 테스트 (기존 기능 정상 동작)
- [ ] 영어 모드 전체 기능 테스트
- [ ] 언어 전환 반복 테스트 (크래시 없음)
- [ ] 메모리 누수 체크

#### Release 빌드 및 업로드
- [ ] Keystore 환경변수 설정
  ```cmd
  set KEYSTORE_PATH=G:\keystore\alcoholictimer-release.jks
  set KEYSTORE_STORE_PW=your_password
  set KEY_ALIAS=alcoholictimer
  set KEY_PASSWORD=your_key_password
  ```
- [ ] `./gradlew clean bundleRelease` 실행
- [ ] AAB 파일 생성 확인 (`app/build/outputs/bundle/release/app-release.aab`)
- [ ] Play Console 업로드
  - [ ] 릴리스 노트 입력 (한국어/영어)
  - [ ] 메타데이터 확인
  - [ ] 단계적 출시 10% 설정
- [ ] 출시 승인 완료

#### 출시 후 모니터링 (48시간)
- [ ] Firebase Crashlytics 크래시 보고 확인
- [ ] Play Console Analytics 국가별 다운로드 확인
- [ ] 리뷰 모니터링 ("translation", "english" 키워드)
- [ ] 문제 없으면 단계적 출시 100% 확대

---

## 🎯 Phase 2: 일본어 지원 (4주)

### ✅ Week 7-8: 일본어 번역

#### 번역 준비
- [ ] `app/src/main/res/values-ja/` 디렉토리 생성
- [ ] `values-ja/strings.xml` 파일 생성
- [ ] 초벌 번역 (DeepL)

#### 전문 번역사 의뢰
- [ ] 일본어 네이티브 번역사 섭외 (Gengo/Upwork)
- [ ] 번역 검수 ($40-50 예상)
- [ ] 용어집 제공 (금주→禁酒, 레벨→レベル 등)
- [ ] 피드백 반영

#### 폰트 최적화 (옵션)
- [ ] Noto Sans JP 폰트 검토
- [ ] 필요 시 `res/font/` 추가
- [ ] Typography 설정 업데이트

### ✅ Week 9-10: 테스트 & 출시

#### 레이아웃 테스트
- [ ] 에뮬레이터 언어를 Japanese로 변경
- [ ] 모든 주요 화면 검증
- [ ] 날짜 형식 "yyyy年M月d日" 확인
- [ ] 통화 "¥1,000" 형식 확인

#### Play Store 메타데이터
- [ ] 일본어 짧은 설명
- [ ] 일본어 전체 설명
- [ ] 일본어 스크린샷 5장

#### 출시
- [ ] 버전 1.3.0 빌드
- [ ] Play Console 업로드 (일본어 메타데이터)
- [ ] 단계적 출시

---

## 🎯 Phase 3: 확장 언어 (중국어, 스페인어 등) (4주)

### ✅ Week 11-12: 중국어 간체

- [ ] `values-zh-rCN/strings.xml` 생성
- [ ] 번역 완료 (전문 또는 크라우드소싱)
- [ ] Noto Sans SC 폰트 검토
- [ ] 날짜 형식 "yyyy年M月d日" 확인
- [ ] 통화 "¥" 확인
- [ ] Play Store 메타데이터 (중국어)
- [ ] 스크린샷 4장 이상

### ✅ Week 13-14: 스페인어

- [ ] `values-es/strings.xml` 생성
- [ ] 번역 완료
- [ ] 날짜 형식 "d 'de' MMM 'de' yyyy" 확인
- [ ] 통화 "€" 확인
- [ ] Play Store 메타데이터 (스페인어)
- [ ] 스크린샷 4장 이상
- [ ] 버전 1.4.0 출시

---

## 🎯 선택: 자동화 및 커뮤니티 (Phase 4+)

### Crowdin 플랫폼 연동
- [ ] Crowdin 계정 생성 (Pro 플랜 $40/월)
- [ ] GitHub 저장소 연동
- [ ] `strings.xml` 자동 동기화 설정
- [ ] 번역 메모리 구축

### 커뮤니티 번역
- [ ] 번역 기여자 모집 공지 (Reddit, Facebook)
- [ ] 용어집 및 스타일 가이드 공유
- [ ] 기여자 크레딧 표시 (About 화면)

### 추가 언어
- [ ] 포르투갈어 (`values-pt-rBR`)
- [ ] 독일어 (`values-de`)
- [ ] 프랑스어 (`values-fr`)
- [ ] 러시아어 (`values-ru`)
- [ ] 힌디어 (`values-hi`)

---

## 📊 성공 지표 체크 (출시 후 1개월)

### 정량적 지표
- [ ] 전체 다운로드 수 +50% 이상
- [ ] 영어권 다운로드 비율 20% 이상
- [ ] 일본 다운로드 비율 10% 이상
- [ ] 평균 평점 4.0+ 유지 (모든 언어)
- [ ] 크래시율 1% 미만 (언어별)

### 정성적 지표
- [ ] 번역 오류 제보 5건 이하 (각 언어)
- [ ] "잘 번역되었다" 긍정 리뷰 존재
- [ ] 커뮤니티 번역 기여자 10명 이상 (Phase 4 이후)

### 모니터링
- [ ] Play Console Analytics 주간 확인
- [ ] Firebase Crashlytics 일일 확인
- [ ] 리뷰 주간 모니터링 (언어별 키워드 검색)

---

## 🆘 문제 해결 체크리스트

### 번역 누락 경고
- [ ] `./gradlew lintDebug` 실행
- [ ] MissingTranslation 항목 확인
- [ ] 누락된 키를 `values-XX/strings.xml`에 추가

### 레이아웃 깨짐
- [ ] `modifier = Modifier.weight(1f)` 또는 `fillMaxWidth()` 사용
- [ ] `maxLines = 2` + `TextOverflow.Ellipsis` 추가
- [ ] Pseudolocalization (English XA)로 테스트

### 날짜/숫자 형식 오류
- [ ] `LocaleFormatUtils` 메서드 사용 확인
- [ ] `Locale.getDefault()` 올바르게 전달되는지 확인
- [ ] 단위 테스트 실행

### 크래시 발생
- [ ] Firebase Crashlytics에서 스택 트레이스 확인
- [ ] 특정 언어에서만 발생하는지 확인
- [ ] 문자열 리소스 누락 여부 체크

### 긴급 롤백 필요 시
- [ ] Play Console → 릴리스 → 이전 버전 선택
- [ ] "이 릴리스로 롤백" 클릭
- [ ] 핫픽스 브랜치 생성 (`hotfix/i18n-fix`)
- [ ] 수정 후 패치 버전(1.2.1) 출시

---

## 💰 예산 체크리스트

### Phase 1 (영어)
- [ ] 영어 번역 검수: $25-30
- [ ] 스크린샷 제작: $0 (자체)
- [ ] 도구 (DeepL): $0 (무료)
- **총: $25-30**

### Phase 2 (일본어)
- [ ] 일본어 전문 번역: $40-50
- [ ] 스크린샷 제작: $0
- **총: $40-50**

### Phase 3 (중국어, 스페인어)
- [ ] 중국어 번역: $35
- [ ] 스페인어 번역: $25
- [ ] 스크린샷: $0
- **총: $60**

### Phase 4 (자동화)
- [ ] Crowdin Pro: $40/월 × 3개월 = $120
- [ ] DeepL API: $5/월 × 3개월 = $15
- **총: $135**

### **전체 예산: $260-280** (Phase 1-3)  
### **확장 시: $395-415** (Phase 4 포함)

---

## 📚 참고 문서

### 필수 읽기 순서
1. **I18N_QUICK_START.md** - 전체 흐름 파악 (이 문서 먼저!)
2. **INTERNATIONALIZATION_PLAN.md** - 전략 및 시장 분석
3. **I18N_IMPLEMENTATION_GUIDE.md** - 실제 코드 구현
4. **I18N_CHECKLIST.md** (이 문서) - 진행 상황 체크

### 추가 자료
- `APP_SPEC.md` - 앱 전체 기획
- `PLAY_STORE_METADATA_TEMPLATE.md` - 스토어 메타데이터 템플릿
- `CHANGELOG.md` - 버전 히스토리

---

**작성일**: 2025-10-27  
**버전**: 1.0  
**다음 액션**: 각 단계별 체크박스를 완료하며 진행하세요!

