# Alcoholic Timer - 금주 타이머

> **v1.1.7** 익명 응원 챌린지가 추가된 금주 타이머

---

## 📱 주요 기능

### 하단 탭 구조 (3개)
1. **Tab 1: 금주 타이머** - 금주 시작/진행/종료
2. **Tab 2: 나의 건강 분석** - 기록 관리 + 통계 + 일기 + 레벨
3. **Tab 3: 커뮤니티** - 익명 응원 챌린지 + 설정

---

## 🎯 주요 화면

### 1. 금주 타이머
- 실시간 금주 진행 상황
- 절약한 금액 계산
- 절약한 시간 표시
- 현재 레벨 및 다음 레벨까지 남은 시간

### 2. 나의 건강 분석
- **기간별 통계**: 주간/월간/연간/전체
- **성공률 분석**: 목표 달성률
- **최장 기간 기록**: 개인 최고 기록
- **일기 기능**: 갈망 수준 기록, 하루 회고
- **레벨 요약**: 현재 레벨 및 다음 레벨 안내

### 3. 커뮤니티 (익명 응원 챌린지)
- **익명 게시글**: 닉네임 랜덤 생성
- **이미지 업로드**: 사진과 함께 응원 메시지
- **24시간 자동 삭제**: 게시글은 24시간 후 자동 삭제
- **좋아요/댓글**: 서로 응원하기
- **설정 메뉴**: 우측 상단 톱니바퀴 아이콘

### 4. 설정 (커뮤니티 → 설정)
- **프로필**: 닉네임, 아바타 변경
- **습관 설정**: 음주 비용, 빈도, 시간 설정
- **통화 설정**: 원화/엔화 선택
- **알림 설정**: 푸시 알림 관리
- **디버그 메뉴**: 개발자 도구

---

## 🏆 레벨 시스템 (11단계)

| 레벨 | 이름 | 필요 일수 |
|------|------|----------|
| Lv.1 | 금주 시작 | 1~3일 |
| Lv.2 | 3일 끊기 통과 | 4~7일 |
| Lv.3 | 1주차 클리어 | 8~14일 |
| Lv.4 | 피부의 변신 | 15~21일 |
| Lv.5 | 습관 형성 (21일) | 22~30일 |
| Lv.6 | 한 달의 기적 | 31~60일 |
| Lv.7 | 변한 몸매 | 61~99일 |
| Lv.8 | 100일, 프로 끊주 | 100~179일 |
| Lv.9 | 플러스 밸런스 | 180~299일 |
| Lv.10 | 금주 마스터 | 300~364일 |
| Legend | 레전드 오브 레전드 | 365일+ |

---

## 🛠 기술 스택

### Frontend
- **Kotlin** - 주 개발 언어
- **Jetpack Compose** - 선언형 UI
- **Material 3** - 디자인 시스템
- **Navigation Compose** - 화면 전환
- **Coil** - 이미지 로딩

### Backend
- **Firebase Firestore** - 실시간 데이터베이스
- **Firebase Storage** - 이미지 저장소
- **Firebase Analytics** - 사용자 분석
- **Firebase Crashlytics** - 오류 추적

### Local Storage
- **Room Database** - 로컬 데이터베이스
- **SharedPreferences** - 설정 저장

### Ads
- **AdMob** - 배너 광고, 전면 광고
- **UMP SDK** - 사용자 동의 관리

---

## 📂 프로젝트 구조

```
app/src/main/java/kr/sweetapps/alcoholictimer/
├── ui/
│   ├── tab_01/          # Tab 1: 금주 타이머
│   ├── tab_02/          # Tab 2: 나의 건강 분석
│   ├── tab_04/          # Tab 3: 커뮤니티
│   ├── tab_05/          # 설정 화면
│   ├── common/          # 공통 컴포넌트
│   ├── components/      # 재사용 컴포넌트
│   ├── theme/           # 테마 설정
│   └── main/            # 메인 네비게이션
├── data/
│   ├── model/           # 데이터 모델
│   └── repository/      # 데이터 저장소
├── analytics/           # Firebase Analytics
├── util/                # 유틸리티
└── ad/                  # 광고 관리
```

---

## 🚀 빌드 및 실행

### 사전 요구사항
- Android Studio Hedgehog | 2023.1.1+
- JDK 11+
- Android SDK 36+

### 빌드 명령어
```powershell
# Debug 빌드
.\gradlew assembleDebug

# Release 빌드
.\gradlew assembleRelease

# 테스트
.\gradlew test
```

### 환경 설정
`local.properties` 파일에 다음 키를 설정하세요:

```properties
# Firebase/Supabase
supabase.url=YOUR_SUPABASE_URL
supabase.key=YOUR_SUPABASE_ANON_KEY

# AdMob 테스트 (Debug 빌드용)
UMP_TEST_DEVICE_HASH=YOUR_TEST_DEVICE_HASH
ADMOB_TEST_DEVICE_ID=YOUR_TEST_DEVICE_ID
```

---

## 📝 최근 업데이트 (v1.1.7)

### 새로운 기능
- ✨ **익명 응원 챌린지** 추가
  - 익명 게시글 작성/조회
  - 이미지 업로드 지원
  - 24시간 자동 삭제
- 🎨 **탭 구조 개선**
  - "커뮤니티" 탭으로 명확한 의미 전달
  - 설정 메뉴를 커뮤니티 내부로 통합
- 🖼️ **이미지 처리**
  - 자동 압축 (1080px, 70% 퀄리티)
  - 원본 비율 유지
  - Firebase Storage 연동

### 개선 사항
- 📱 **UX 개선**
  - 슬라이드 애니메이션 추가
  - 키보드 자동 제어
  - 작성 중 뒤로가기 방지
- 🎨 **UI 개선**
  - 페이스북 스타일 바텀 시트
  - 깔끔한 리스트 디자인
  - 원본 비율 이미지 표시

---

## 📄 라이선스

이 프로젝트는 비공개 소스입니다.

### 사용된 오픈소스

#### App Icon
- **출처**: Free Wayfinding vector icons - Guidance icon set
- **제작자**: Streamline and Vincent le moign
- **라이선스**: CC BY 4.0
- **변경 사항**: 색상 및 형태 수정

---

## 👥 개발자

- **개발**: SweetApps
- **디자인**: SweetApps
- **기획**: SweetApps

---

## 📞 문의

- **앱 내 문의**: 설정 → 문의/제안
- **이메일**: support@sweetapps.kr

---

**마지막 업데이트**: 2025-12-19

