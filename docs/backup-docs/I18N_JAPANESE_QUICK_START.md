# 일본어 현지화 빠른 시작 가이드

**작업 완료일**: 2025년 10월 28일

---

## ✅ 완료된 작업

### 1. 일본어 리소스 파일 생성
- **파일**: `app/src/main/res/values-ja/strings.xml`
- **문자열 수**: 205개
- **상태**: ✅ 완료

### 2. 통화 포맷 (1엔 = 10원)
```kotlin
// FormatUtils.kt
private const val WON_TO_YEN_RATE = 10.0

// 10,000원 → ¥1,000
// 50,000원 → ¥5,000
```

### 3. 날짜/시간 포맷
- **날짜**: `yyyy年MM月dd日` (예: 2025年10月28日)
- **시간**: `H:mm` (24시간제)

---

## 🧪 테스트 방법

### Android 에뮬레이터/디바이스에서 테스트

1. **언어 변경**
   ```
   Settings → System → Languages & input → Languages
   → Add a language → 日本語 (Japanese)
   → 日本語를 최상단으로 드래그
   ```

2. **앱 재시작**
   - 앱을 완전히 종료하고 재실행
   - 모든 텍스트가 일본어로 표시되는지 확인

3. **확인 항목**
   - [ ] 메인 화면의 모든 텍스트가 일본어
   - [ ] 금액이 `¥` 기호로 표시
   - [ ] 날짜가 `yyyy年MM月dd日` 형식
   - [ ] 레벨 이름이 일본어로 표시

---

## 📋 주요 번역 예시

### 화면 제목
- 금주 진행 → 禁酒の進捗
- 기록 → 記録
- 레벨 → レベル
- 설정 → 設定

### 통계 항목
- 절약 금액 → 節約金額
- 성공률 → 成功率
- 평균 기간 → 평均期間
- 총 금주일수 → 総禁酒日数

### 버튼/액션
- 시작 → 開始
- 계속 → 続ける
- 삭제 → 削除
- 취소 → キャンセル
- 저장 → 保存

---

## 💴 통화 변환 예시

| 원화 (KRW) | 엔화 (JPY) |
|------------|------------|
| 1,000원 | ¥100 |
| 5,000원 | ¥500 |
| 10,000원 | ¥1,000 |
| 50,000원 | ¥5,000 |
| 100,000원 | ¥10,000 |

---

## 🛠️ 수정된 파일

1. `app/src/main/res/values-ja/strings.xml` (새 파일)
2. `app/src/main/java/com/example/alcoholictimer/core/util/FormatUtils.kt`
3. `app/src/main/java/com/example/alcoholictimer/feature/addrecord/AddRecordActivity.kt`
4. `app/src/main/java/com/example/alcoholictimer/feature/detail/DetailActivity.kt`
5. `app/src/main/java/com/example/alcoholictimer/feature/run/RunActivity.kt` - 통화 표시 다국어 지원 수정

### 주요 수정 사항
- **RunActivity.kt**: 금액 표시 시 "원" 하드코딩 제거
  - 엔화(¥), 달러($), 원화 자동 감지 및 올바른 표시
  - 기존 문제: "¥72원"으로 중복 표시
  - 수정 후: "¥72"로 정확하게 표시

---

## 📱 Play Store 준비

### 일본어 메타데이터

**앱 이름**
```
アルコールタイマー
```

**짧은 설명**
```
禁酒記録と成功率を簡単に追跡できるアプリ
```

**전체 설명**
```
🍺 アルコールタイマー - あなたの禁酒チャレンジをサポート

禁酒・節酒の習慣形成をサポートするシンプルなタイマーアプリです。

主な機能：
✅ 禁酒日数の記録と追跡
✅ 目標達成率の可視化
✅ レベルシステムでモチベーション維持
✅ 節約金額の自動計算
✅ 進捗通知と励まし

シンプルで使いやすいUIで、あなたの禁酒チャレンジを応援します。
```

---

## 🔍 트러블슈팅

### Q: 앱이 여전히 한국어로 표시됩니다
A: 
1. 디바이스 언어 설정 확인
2. 앱 완전 종료 후 재시작
3. 앱 캐시 삭제 시도

### Q: 금액이 원화로 표시됩니다
A:
1. `Locale.getDefault().language`가 "ja"인지 확인
2. FormatUtils.kt의 when 구문 확인

### Q: 날짜 형식이 변경되지 않습니다
A:
1. SimpleDateFormat의 locale 설정 확인
2. AddRecordActivity.kt, DetailActivity.kt 수정 확인

---

## 📖 추가 문서

- **상세 가이드**: [I18N_JAPANESE_DONE.md](I18N_JAPANESE_DONE.md)
- **전체 계획**: [INTERNATIONALIZATION_PLAN.md](INTERNATIONALIZATION_PLAN.md)
- **영어 가이드**: [I18N_ENGLISH_DONE.md](I18N_ENGLISH_DONE.md)

---

## ✨ 다음 단계

1. **QA 테스트**
   - 전체 체크리스트 실행
   - 실기기에서 테스트

2. **네이티브 검수**
   - 일본어 원어민 검수
   - 번역 품질 개선

3. **Play Store 출시**
   - 스크린샷 준비
   - 메타데이터 등록

---

**작성자**: AlcoholicTimer 개발팀  
**버전**: 1.0

