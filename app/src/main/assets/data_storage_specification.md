# 데이터 저장 및 기록 관리 사양서

## 1. 개요

AlcoholicTimer 앱의 모든 데이터 저장 및 기록 관리에 대한 상세 사양을 정의합니다.

> 참고: 2025-10-02 버전부터 "레벨 테스트 모드" 기능은 제거되었습니다. 아래 문서의 test_settings 및 test_mode 항목은 호환성 설명을 위한 Deprecated 항목입니다.

---

## 2. 데이터 저장 구조

### (1) SharedPreferences 구조

#### A. 기본 설정 저장소
- **이름**: `"user_settings"`
- **모드**: `Context.MODE_PRIVATE`
- **용도**: 금주 기록, 사용자 설정 저장

#### B. 테스트 설정 저장소 (Deprecated)
- **이름**: `"test_settings"`
- **모드**: `Context.MODE_PRIVATE`
- **용도**: [Deprecated] 과거 테스트 모드 설정 저장. 현 버전에서는 사용하지 않으며, 존재하더라도 앱이 무시합니다.

### (2) 데이터 키 구조

#### A. 금주 기록 관련
```
키 이름: "sobriety_records"
데이터 타입: String (JSON Array)
설명: 모든 금주 기록을 JSON 배열 형태로 저장
```

#### B. 현재 진행 중인 금주 정보
```
키 이름: "start_time"
데이터 타입: Long
설명: 현재 금주 시작 시간 (밀리초)

키 이름: "target_days"
데이터 타입: Int
설명: 목표 금주 기간 (일 단위)
```

#### C. 테스트 모드 설정 (Deprecated)
```
키 이름: "test_mode"
데이터 타입: Int
설명: [Deprecated] 0=실제모드, 1=분단위, 2=초단위. 현 버전에서는 읽더라도 적용되지 않으며 항상 실제 모드 기준으로 동작합니다.
```

---

## 3. 금주 기록 데이터 모델

### (1) SobrietyRecord 클래스 구조

```kotlin
data class SobrietyRecord(
    val id: Long,              // 고유 ID (저장 시간 기반)
    val startDate: String,     // 금주 시작 날짜 ("yyyy-MM-dd HH:mm:ss")
    val endDate: String,       // 금주 종료 날짜 ("yyyy-MM-dd HH:mm:ss")
    val duration: Int,         // 목표 기간 (일 또는 분 단위)
    val achievedDays: Int,     // 실제 달성 기간
    val achievedLevel: Int,    // 달성한 레벨
    val levelTitle: String,    // 달성한 레벨 타이틀
    val isCompleted: Boolean   // 목표 완료 여부
)
```

### (2) JSON 저장 형식

```json
[
    {
        "id": "1704067200000",
        "startTime": 1704067200000,
        "endTime": 1704153600000,
        "targetDays": 7,
        "actualDays": 7,
        "isCompleted": true,
        "status": "completed",
        "createdAt": 1704153600000
    }
]
```

---

## 4. 데이터 저장 위치 및 관리

### (1) 기록 저장 위치
- **파일 경로**: `RunActivity.kt` - `saveRecord()` 함수
- **저장소**: SharedPreferences("user_settings")
- **키**: "sobriety_records"
- **형식**: JSON Array String

### (2) 기록 로드 위치
- **파일 경로**: `RecordsActivity.kt` - `loadSobrietyRecords()` 함수
- **저장소**: SharedPreferences("user_settings")
- **키**: "sobriety_records"

### (3) 기록 초기화 위치
- **파일 경로**: `TestActivity.kt` - "모든 기록 초기화" 버튼
- **저장소**: SharedPreferences("user_settings")
- **동작**: 전체 clear() + remove("sobriety_records")

---

## 5. SharedPreferences 관리 클래스

### (1) SharedPreferencesManager 클래스
- **위치**: `utils/SharedPreferencesManager.kt`
- **용도**: SharedPreferences 통합 관리
- **싱글톤 패턴** 적용

### (2) 주요 메서드
```kotlin
fun getBoolean(key: String, defaultValue: Boolean): Boolean
fun setBoolean(key: String, value: Boolean)
fun getLong(key: String, defaultValue: Long): Long
fun setLong(key: String, value: Long)
fun getString(key: String, defaultValue: String?): String?
fun setString(key: String, value: String)
fun getInt(key: String, defaultValue: Int): Int
fun setInt(key: String, value: Int)
```

---

## 6. 상수 관리

### (1) Constants 클래스
- **위치**: `utils/Constants.kt`
- **SharedPreferences 이름**: `"AlcoholicTimerPrefs"`
- **기록 키**: `"records"`

### (2) 주요 상수
```kotlin
const val PREFS_NAME = "AlcoholicTimerPrefs"
const val PREF_KEY_TEST_MODE = "test_mode" // Deprecated - 현 버전에서 무시됨
const val PREF_START_TIME = "start_time"
const val PREF_TARGET_DAYS = "target_days"
const val PREF_RECORDS = "records"
```

---

## 7. 데이터 일관성 확인사항

### (1) 현재 상태
- **SharedPreferencesManager**: "user_settings" 사용
- **Constants**: "AlcoholicTimerPrefs" 정의 (테스트 모드 관련은 무시됨)
- **실제 저장/로드**: "user_settings" 사용
- **일관성**: 모든 주요 데이터 접근이 "user_settings"로 통일됨

### (2) 권장사항
- 모든 데이터 접근을 SharedPreferencesManager를 통해 수행
- Constants에 정의된 키 이름들을 실제 코드에서 활용
- 하드코딩된 문자열을 상수로 대체

---

## 8. 데이터 백업 및 복원

### (1) 현재 상태
- 자동 백업: 미구현
- 수동 백업: 미구현
- 클라우드 동기화: 미구현

### (2) 향후 확장 가능성
- JSON 파일 내보내기/가져오기
- Google Drive 백업
- 앱 간 데이터 이전

---

## 9. 디버깅 및 로깅

### (1) 로그 태그
- **TestActivity**: 기록 초기화 관련
- **RunActivity**: 기록 저장 관련
- **RecordsActivity**: 기록 로드 관련

### (2) 주요 로그 포인트
- 기록 저장 시점
- 기록 로드 시점
- 초기화 전후 상태
- JSON 파싱 오류

---

## 10. 관련 파일 목록

### (1) 데이터 모델
- `utils/SobrietyRecord.kt` - 금주 기록 데이터 클래스
- `utils/Constants.kt` - 상수 정의

### (2) 데이터 관리
- `utils/SharedPreferencesManager.kt` - SharedPreferences 관리

### (3) 데이터 사용
- `RunActivity.kt` - 기록 저장
- `RecordsActivity.kt` - 기록 로드 및 표시
- `TestActivity.kt` - 기록 초기화

---

## 11. 버전 히스토리

| 버전 | 날짜 | 변경사항 |
|------|------|----------|
| 1.0 | 2025-01-10 | 초기 문서 작성 |
| 1.1 | 2025-10-02 | 테스트 모드(레벨 테스트 모드) 제거, 관련 항목 Deprecated 표시 |

---

*이 문서는 AlcoholicTimer 앱의 데이터 저장 구조를 정의하며, 개발 및 유지보수 시 참고용으로 활용됩니다.*
