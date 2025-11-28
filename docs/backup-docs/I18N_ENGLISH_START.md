# 영어 지원 빠른 시작 가이드

> ⚡ 영어 버전 출시까지 2주 완성 로드맵

최종 수정: 2025-10-27

---

## ✅ 완료된 작업

### 1. 영어 리소스 생성 완료
- ✅ `app/src/main/res/values-en/` 디렉토리 생성
- ✅ `values-en/strings.xml` 파일 생성 및 번역 완료
- ✅ 총 50개 문자열 영어 번역 완료

---

## 🚀 지금 바로 테스트하기

### 1. 앱 빌드 및 실행

```cmd
cd G:\Workspace\AlcoholicTimer
gradlew.bat clean assembleDebug
```

### 2. 에뮬레이터/기기에서 언어 변경

**Android 에뮬레이터**:
1. Settings → System → Languages & input
2. Languages → Add a language
3. **English (United States)** 선택
4. English를 최상단으로 드래그
5. 앱 재시작

**ADB 명령어 (빠른 방법)**:
```cmd
adb shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote"
```

### 3. 확인 사항
- [ ] Run 화면: "Sobriety Progress", "Days Sober", "Money Saved"
- [ ] Quit 화면: "Are you sure you want to stop?"
- [ ] Records 화면: "All Records", "No records yet"
- [ ] Detail 화면: "Record Details", "Goal Achievement"
- [ ] About 화면: "Version Info", "Open Source Licenses"

---

## 📝 다음 단계 (선택사항)

### 옵션 1: 그대로 출시 (권장)
현재 번역은 품질이 충분합니다. 바로 Play Store 메타데이터 작성으로 넘어가세요.

### 옵션 2: 네이티브 검수 (더 높은 품질)
**비용**: $25-30  
**시간**: 2-3일  
**플랫폼**: Upwork, Fiverr, Gengo

**Upwork 의뢰 예시**:
```
Title: Review English Translation for Android App (500 words)

Description:
I need a native English speaker to review translations for a sobriety tracking app.
- App type: Habit tracker (health & fitness)
- Tone: Friendly, encouraging, supportive
- Word count: ~500 words
- Format: Android strings.xml
- Budget: $25-30
- Delivery: 2-3 days

I will provide:
- English strings.xml file
- 5 app screenshots
- Glossary of key terms

Please review for:
- Natural phrasing
- Consistent tone
- No awkward translations
```

---

## 🎨 Play Store 메타데이터 (영어)

### 짧은 설명 (80자)
```
Track your sobriety journey with simple records and success rates.
```

### 전체 설명
```
AlcoholicTimer is a simple and focused sobriety tracker.

Key Features:
• Track daily sobriety/abstinence records
• View weekly, monthly, yearly, and lifetime statistics
• Monitor your longest streak and average duration
• 100% offline: All data stored locally on your device
• No personal data collection (no ads or tracking)

Why Choose AlcoholicTimer:
✓ Simple & Intuitive: Start tracking in seconds
✓ Motivating: See your progress with visual stats and levels
✓ Private: Your data never leaves your device
✓ Ad-free: Focus on your journey without distractions

Perfect for:
- Anyone trying to quit drinking
- Those participating in Dry January or similar challenges
- People wanting to track their sobriety milestones
- Health-conscious individuals monitoring alcohol consumption

Upcoming Features:
• Additional statistics and insights
• Export/backup functionality
• Widget support

Privacy Policy:
This app does not collect or transmit any personal information such as email, location, or advertising ID. All records are stored only on your device.

Contact: sweetapps.kr@gmail.com

Keywords: sobriety, quit drinking, sober counter, habit tracker, abstinence, recovery, alcohol free, dry january, health
```

### 스크린샷 준비

**필요한 스크린샷** (최소 5장):
1. Run 화면 (진행 중)
2. Records 화면 (주간 통계)
3. Detail 화면 (기록 상세)
4. All Records 화면 (전체 목록)
5. About 화면 (정보)

**캡처 방법**:
1. 에뮬레이터를 English로 설정
2. 각 화면으로 이동
3. `Ctrl + S` 또는 카메라 아이콘으로 스크린샷
4. 해상도: 1080x1920 권장

**저장 위치**:
```
G:\Workspace\AlcoholicTimer\screenshots\en\
├── 01_run_progress.png
├── 02_records_stats.png
├── 03_detail.png
├── 04_all_records.png
└── 05_about.png
```

---

## 🧪 QA 체크리스트

### 레이아웃 검증
- [ ] Run 화면: "Days Sober" 텍스트가 잘리지 않는가?
- [ ] Quit 화면: 다이얼로그 버튼 "Continue"와 "Stop"이 겹치지 않는가?
- [ ] Detail 화면: "Goal Achievement" 레이블이 줄바꿈 없이 표시되는가?
- [ ] Records 화면: "No records yet" 빈 상태 메시지가 잘 보이는가?

### 기능 테스트
- [ ] 금주 시작/종료 정상 작동
- [ ] 통계 조회 정상 표시
- [ ] 기록 추가/삭제 정상 작동
- [ ] Toast 메시지 영어로 표시
- [ ] About 화면 라이선스 정보 표시

### 언어 전환 테스트
- [ ] 한국어 → 영어 전환 시 크래시 없음
- [ ] 영어 → 한국어 전환 시 크래시 없음
- [ ] 전환 후 모든 화면 정상 표시

---

## 📦 출시 준비

### 1. 버전 업데이트

```kotlin
// app/build.gradle.kts
val releaseVersionCode = 2025102701  // YYYYMMDD + 01
val releaseVersionName = "1.2.0"  // 다국어 지원
```

### 2. CHANGELOG.md 업데이트

```markdown
## [1.2.0] - 2025-10-27

### Added
- 🌐 영어(English) 지원 추가
- 🗓️ 로케일 기반 날짜/시간 형식 자동 변환 준비

### Changed
- 모든 문자열을 리소스로 관리하여 안정성 향상

### Fixed
- 일부 화면에서 텍스트 잘림 현상 방지
```

### 3. Release Notes (Play Console용)

**한국어**:
```
• 영어 지원 추가
• 안정성 개선
```

**English**:
```
• Added English language support
• Improved stability
```

### 4. Lint 검사

```cmd
gradlew.bat lintDebug
```

**확인 사항**:
- MissingTranslation 경고 0건
- 심각한 오류 없음

### 5. Release 빌드

```cmd
REM 환경변수 설정
set KEYSTORE_PATH=G:\keystore\alcoholictimer-release.jks
set KEYSTORE_STORE_PW=your_password
set KEY_ALIAS=alcoholictimer
set KEY_PASSWORD=your_key_password

REM Release 빌드
gradlew.bat clean bundleRelease
```

**산출물**:
```
app\build\outputs\bundle\release\app-release.aab
```

---

## 🚀 Play Console 업로드

### 1. 프로덕션 릴리스 생성
1. Play Console → **릴리스** → **프로덕션**
2. **새 릴리스 만들기**
3. AAB 업로드: `app-release.aab`

### 2. 메타데이터 추가

**스토어 등록정보** → **기본 스토어 등록정보**:
- 언어: **한국어 (대한민국)** (기존)
- 언어: **영어 (미국)** 추가 클릭

**영어 메타데이터 입력**:
- 짧은 설명: (위 참조)
- 전체 설명: (위 참조)
- 스크린샷: 5장 업로드

### 3. 출시 노트
- 한국어: "영어 지원 추가"
- English: "Added English language support"

### 4. 검토 및 출시
- **저장** → **검토** 
- **단계적 출시 시작 (10%)**
- 48시간 모니터링 후 → **50%** → **100%**

---

## 📊 출시 후 모니터링 (첫 48시간)

### Firebase Crashlytics
- [ ] 새 크래시 보고 확인
- [ ] 언어별 크래시율 비교

### Play Console Analytics
- [ ] 국가별 다운로드 분포 확인
- [ ] 영어권 국가 다운로드 증가 확인
- [ ] 평균 평점 유지 (4.0+)

### 리뷰 모니터링
- [ ] "english", "translation" 키워드 검색
- [ ] 번역 오류 제보 확인
- [ ] 긍정/부정 리뷰 비율

---

## ⏱️ 타임라인 요약

### 완료 (오늘)
- ✅ 영어 번역 완료
- ✅ values-en/strings.xml 생성

### 내일 (1일차)
- [ ] 에뮬레이터에서 영어 모드 테스트
- [ ] QA 체크리스트 완료
- [ ] Play Store 메타데이터 작성

### 모레 (2일차)
- [ ] 스크린샷 5장 캡처
- [ ] (옵션) 네이티브 검수 의뢰
- [ ] 버전 업데이트 및 CHANGELOG 작성

### 3-5일차 (검수 대기)
- [ ] 네이티브 피드백 반영 (의뢰한 경우)
- [ ] 최종 테스트

### 6-7일차 (출시 주)
- [ ] Release 빌드
- [ ] Play Console 업로드
- [ ] 단계적 출시 시작 (10%)

### 7-9일차 (모니터링)
- [ ] 크래시 모니터링
- [ ] 리뷰 확인
- [ ] 문제 없으면 100% 확대

**총 소요 시간**: 7-9일 (검수 제외 시 2-3일)

---

## 💰 예산

| 항목 | 비용 | 비고 |
|------|------|------|
| 영어 번역 | $0 | 직접 번역 완료 |
| 네이티브 검수 (옵션) | $25-30 | Upwork/Fiverr |
| 스크린샷 제작 | $0 | 직접 제작 |
| **총계** | **$0-30** | |

---

## 🆘 문제 해결

### 문제: 일부 문자열이 여전히 한국어로 표시됨
**해결**: 
```cmd
REM Lint로 누락 확인
gradlew.bat lintDebug

REM MissingTranslation 경고 확인 후 values-en/strings.xml에 추가
```

### 문제: 레이아웃 깨짐 (텍스트 잘림)
**해결**:
- "Days Sober" 같은 긴 텍스트는 `maxLines = 2` 설정
- 버튼은 `weight(1f)` 사용하여 공간 균등 분배

### 문제: 에뮬레이터에서 언어 변경이 앱에 반영되지 않음
**해결**:
```cmd
REM 앱 재시작 또는 ADB 명령어 사용
adb shell am force-stop kr.sweetapps.alcoholictimer
adb shell am start -n kr.sweetapps.alcoholictimer/.feature.start.StartActivity
```

---

## 🎉 다음 단계

### 일본어 추가를 원하신다면?
`I18N_IMPLEMENTATION_GUIDE.md`의 "Week 5-6: 일본어 준비" 섹션 참조

### 자동화하고 싶다면?
Crowdin 플랫폼 연동 고려 (Phase 2 이후)

### 더 많은 언어를 추가하려면?
`INTERNATIONALIZATION_PLAN.md`의 Phase 2-3 계획 참조

---

**작성일**: 2025-10-27  
**버전**: 1.0  
**상태**: ✅ 영어 번역 완료, 테스트 준비 완료!

**다음 액션**: 에뮬레이터에서 영어로 앱 실행해보세요! 🚀

