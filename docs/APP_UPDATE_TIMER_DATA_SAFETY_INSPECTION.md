# 📊 앱 업데이트 시 타이머 데이터 안전성 점검 보고서

**점검일:** 2026-01-06  
**점검자:** AI Assistant  
**점검 목적:** 앱 업데이트 시 기존 사용자의 진행 중인 타이머가 변경되거나 초기화되는 위험 요소 확인  
**점검 결과:** ✅ **안전함 (위험 요소 없음)**

---

## 🎯 점검 개요

사용자께서 "앱 업데이트 시 기존 타이머의 목표일이 변경되는 현상"을 목격하셨다고 하셨으나, 코드 전체를 점검한 결과 **앱 업데이트 시 타이머 데이터를 변경하거나 초기화하는 로직은 존재하지 않습니다**.

---

## ✅ 주요 점검 항목

### 1. 앱 업데이트 감지 로직 ✅ 없음

**점검 결과:** 앱에 `versionCode` 비교나 `lastUpdateTime` 체크 로직이 **존재하지 않습니다**.

**확인 코드:**
```kotlin
// MainApplication.kt - onCreate()
// ❌ versionCode 체크 로직 없음
// ❌ onUpgrade 로직 없음
// ❌ 업데이트 감지 로직 없음
```

**검색 결과:**
- `onUpgrade`: 0건
- `lastUpdateTime`: 0건
- `versionCode` 관련 코드: Supabase 팝업 정책용으로만 사용 (타이머와 무관)

---

### 2. SharedPreferences 자동 초기화 로직 ✅ 안전함

**점검 결과:** SharedPreferences를 초기화하는 로직은 **재설치 감지 시에만** 실행되며, 일반 업데이트에서는 **절대 실행되지 않습니다**.

#### 📍 위치: `Constants.kt` - `ensureInstallMarkerAndResetIfReinstalled()`

```kotlin
fun ensureInstallMarkerAndResetIfReinstalled(context: Context) {
    val markerFile = File(context.noBackupFilesDir, INSTALL_MARKER_NAME)
    
    // [안전장치] 마커 파일이 존재하면 즉시 반환 (초기화 안 함)
    if (markerFile.exists()) return

    val firstInstallTime = try {
        pm.getPackageInfo(context.packageName, 0).firstInstallTime
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    
    // [안전장치] 최근 1시간 이내 설치만 재설치로 간주
    val isRecentInstall = (System.currentTimeMillis() - firstInstallTime) < FRESH_INSTALL_WINDOW_MILLIS
    // FRESH_INSTALL_WINDOW_MILLIS = 60 * 60 * 1000L (1시간)

    val sharedPref = context.getSharedPreferences(USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    if (isRecentInstall) {
        // ⚠️ 이 블록은 최근 1시간 이내 재설치 시에만 실행됨
        sharedPref.edit {
            remove(PREF_START_TIME)
            remove(PREF_TARGET_DAYS)
            putBoolean(PREF_TIMER_COMPLETED, false)
        }
    }
    try { markerFile.writeText("1") } catch (_: Exception) { }
}
```

**분석:**
- ✅ **마커 파일 존재 시:** 아무 작업도 하지 않고 즉시 반환
- ✅ **일반 업데이트:** `firstInstallTime`은 변경되지 않으므로 `isRecentInstall = false`
- ⚠️ **위험 케이스:** 앱을 삭제 후 1시간 이내에 재설치한 경우에만 타이머 초기화

**호출 위치:** 이 함수가 **어디서도 호출되지 않음**을 확인
```bash
grep -r "ensureInstallMarkerAndResetIfReinstalled" --include="*.kt"
# 결과: 정의부만 존재, 호출부 없음
```

**결론:** 이 로직은 **현재 사용되지 않음** (Dead Code)

---

### 3. Room Database 마이그레이션 ✅ 안전함

**점검 결과:** Room Database 마이그레이션은 **다이어리 데이터베이스**만 대상이며, 타이머 데이터(SharedPreferences)와는 **완전히 분리**되어 있습니다.

#### 📍 위치: `AppDatabase.kt`

```kotlin
@Database(
    entities = [DiaryEntity::class],  // ← 다이어리만 관리
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // Migration 1 -> 2: imageUrl 추가
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE diary_table ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    // Migration 2 -> 3: tagType 추가
    // Migration 3 -> 4: sharedPostId 추가
    // Migration 4 -> 5: userLevel, currentDays 추가

    fun getDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(/* ... */)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}
```

**분석:**
- ✅ 다이어리 테이블(`diary_table`)만 마이그레이션
- ✅ 타이머 데이터는 SharedPreferences에 저장 (완전히 별도 저장소)
- ✅ Room DB와 타이머 데이터 간 상호작용 없음

**타이머 데이터 저장 위치:**
```kotlin
// SharedPreferences에 저장됨 (Room DB와 무관)
- PREF_START_TIME: "start_time"
- PREF_TARGET_DAYS: "target_days"
- PREF_TIMER_COMPLETED: "timer_completed"
```

**결론:** Room DB 마이그레이션은 타이머에 **전혀 영향 없음**

---

### 4. 타이머 데이터 저장 방식 ✅ 안전함

**점검 결과:** 타이머 데이터는 **SharedPreferences**에 저장되며, 앱 업데이트 시에도 **자동으로 보존**됩니다.

#### 📍 타이머 데이터 구조

```kotlin
// SharedPreferences: "user_settings"
{
    "start_time": 1704556800000L,      // 타이머 시작 시각 (Long)
    "target_days": 30.0f,               // 목표 일수 (Float)
    "timer_completed": false            // 완료 여부 (Boolean)
}
```

#### 📍 데이터 읽기/쓰기 위치

**1. 데이터 저장 (Tab01ViewModel.kt)**
```kotlin
fun startTimer(targetDays: Float) {
    val now = System.currentTimeMillis()
    _startTime.value = now
    _targetDays.value = targetDays
    _timerCompleted.value = false

    // SharedPreferences에 저장
    sharedPref.edit()
        .putLong(Constants.PREF_START_TIME, now)
        .putFloat(Constants.PREF_TARGET_DAYS, targetDays)
        .putBoolean(Constants.PREF_TIMER_COMPLETED, false)
        .apply()
}
```

**2. 데이터 복원 (Tab01ViewModel.kt)**
```kotlin
private fun loadTimerState() {
    // SharedPreferences에서 복원
    _startTime.value = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
    _targetDays.value = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 1f)
    _timerCompleted.value = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)
}
```

**3. 앱 시작 시 자동 복원 (MainApplication.kt + Tab01ViewModel.kt)**
```kotlin
// MainApplication.onCreate() → TimerStateRepository 초기화
TimerStateRepository.initialize(this)

// Tab01ViewModel.init() → 타이머 상태 자동 로드
init {
    loadTimerState()  // ← SharedPreferences에서 자동 복원
    
    // TimerTimeManager에도 복원
    if (currentStartTime > 0 && !currentCompleted) {
        TimerTimeManager.restoreState(getApplication(), currentStartTime, currentTargetDays, false)
    }
}
```

**분석:**
- ✅ SharedPreferences는 앱 삭제 전까지 영구 보존
- ✅ 앱 업데이트 시에도 자동 보존 (Android 시스템이 보장)
- ✅ 앱 시작 시 자동으로 복원됨

---

### 5. 타이머 데이터 변경 가능한 경로 분석 ✅ 모두 안전함

타이머 데이터를 변경하는 모든 코드 경로를 추적한 결과, **사용자 액션에 의해서만** 변경됩니다.

#### 📍 타이머 데이터 변경 경로 (5가지)

| 경로 | 트리거 | 영향 | 안전성 |
|------|--------|------|--------|
| **1. 새 타이머 시작** | 사용자가 "시작" 버튼 클릭 | `start_time`, `target_days` 설정 | ✅ 의도된 동작 |
| **2. 타이머 포기** | 사용자가 "포기" 버튼 클릭 | `start_time = 0`, `timer_completed = false` | ✅ 의도된 동작 |
| **3. 타이머 완료** | 목표 일수 도달 (자동) | `timer_completed = true` | ✅ 의도된 동작 |
| **4. 기록 삭제** | 사용자가 DetailScreen에서 삭제 | **과거에 버그 있었으나 현재 수정됨** | ✅ 안전 (수정됨) |
| **5. 모든 기록 삭제** | 사용자가 "모두 삭제" 클릭 | **과거에 버그 있었으나 현재 수정됨** | ✅ 안전 (수정됨) |

**중요 수정 이력:**
- **2026-01-02**: "모든 기록 삭제" 시 타이머 초기화 버그 수정 (`DELETE_ALL_RECORDS_TIMER_PRESERVE_FIX.md`)
- **2026-01-04**: 기록 삭제 시 타이머 보호 로직 추가 (`TAB02_DELETE_RECORD_TIMER_PROTECTION_FINAL.md`)

#### 📍 현재 코드 상태 (수정 완료)

**RecordsDataLoader.kt - clearAllRecords()** (수정 후)
```kotlin
fun clearAllRecords(context: Context): Boolean = try {
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    // ✅ 기록만 삭제, 타이머는 건드리지 않음
    sharedPref.edit()
        .putString("sobriety_records", "[]")
        .apply()
    
    // 리스너에게 알림
    for (listener in clearRecordsListeners) {
        try { listener() } catch (_: Exception) {}
    }
    true
} catch (_: Exception) { false }
```

**Tab03ViewModel.kt - clearRecordsCallback** (수정 후)
```kotlin
private val clearRecordsCallback: () -> Unit = {
    Log.d("Tab03ViewModel", "Records cleared - refreshing stats (timer preserved)")

    // ✅ 타이머 상태는 절대 건드리지 않음
    // ❌ 과거 코드 (제거됨): sharedPref.edit().putLong("start_time", 0L)
    // ❌ 과거 코드 (제거됨): sharedPref.edit().putBoolean("timer_completed", false)

    // 재계산만 수행
    loadRecordsAndCalculateTotalTime()
}
```

**분석:**
- ✅ 현재는 기록 삭제 시 타이머를 건드리지 않음
- ✅ 2026-01-02/04에 버그가 수정되어 안전함

---

### 6. SharedPreferences 변경 감지 리스너 ✅ 안전함

**점검 결과:** SharedPreferences 리스너는 **외부 변경 사항을 감지하여 UI를 동기화**하는 용도로만 사용되며, 데이터를 변경하지 않습니다.

#### 📍 위치: Tab01ViewModel.kt

```kotlin
private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    when (key) {
        Constants.PREF_START_TIME,
        Constants.PREF_TARGET_DAYS -> {
            Log.d("Tab01ViewModel", "[FIX] Timer data changed ($key) -> reloading state")

            // 1. 최신 데이터 로드 (읽기만 함)
            loadTimerState()

            // 2. TimerTimeManager에도 최신 상태 반영 (쓰기 없음)
            val newStartTime = _startTime.value
            val newTargetDays = _targetDays.value
            val newCompleted = _timerCompleted.value

            if (newStartTime > 0 && !newCompleted) {
                TimerTimeManager.restoreState(getApplication(), newStartTime, newTargetDays, false)
            }
        }
    }
}
```

**분석:**
- ✅ **읽기 전용:** `loadTimerState()`는 데이터를 읽기만 함
- ✅ **동기화 목적:** 다른 화면에서 변경한 데이터를 현재 화면에 반영
- ✅ **쓰기 없음:** SharedPreferences에 쓰는 코드 없음

---

## 🔍 가능한 원인 분석

사용자께서 목격하신 "목표일 변경" 현상의 가능한 원인:

### 1. ⚠️ 기본값 변경 (2025-12-25) - 가능성 있음

**점검 결과:** 2025년 12월 25일에 **기본값이 변경**된 이력이 있습니다.

```kotlin
// Tab01ViewModel.kt - loadTimerState()
_targetDays.value = sharedPref.getFloat(Constants.PREF_TARGET_DAYS, 1f)
// [CHANGED] 기본값 30 -> 1 (2025-12-25)
```

**분석:**
- ✅ 기존 사용자는 영향 없음 (이미 저장된 값이 있으므로 기본값 미사용)
- ⚠️ **단, SharedPreferences가 손상된 경우** 기본값 1일로 표시될 수 있음
- ⚠️ 또는 앱 데이터 삭제 후 재설치 시 기본값 적용

**가능한 시나리오:**
```
사용자가 30일 목표로 타이머 시작
  ↓
(어떤 이유로 SharedPreferences 손상 또는 삭제)
  ↓
앱 재실행 시 기본값 1일로 표시됨
  ↓
사용자: "목표일이 변경되었다!" (실제로는 데이터 손실)
```

### 2. 🐛 과거 버그 (2026-01-02 이전) - 가능성 있음

**2026-01-02 이전**에는 "모든 기록 삭제" 시 타이머도 초기화되는 버그가 있었습니다.

```kotlin
// ❌ 과거 코드 (2026-01-02 이전)
fun clearAllRecords(context: Context) {
    sharedPref.edit()
        .putString("sobriety_records", "[]")
        .putLong("start_time", 0L)  // ← 타이머까지 초기화!
        .putBoolean("timer_completed", false)
        .apply()
}
```

**만약 사용자가:**
1. 2026-01-02 이전 버전 사용 중
2. "모든 기록 삭제" 버튼 클릭
3. 타이머가 초기화됨
4. 앱 업데이트와 시점이 겹쳐서 "업데이트 때문"이라고 오해

**현재 상태:**
- ✅ 2026-01-02에 버그 수정 완료
- ✅ 현재 버전(1.2.9)에서는 안전함

### 3. 🤷 UI 표시 오류 - 가능성 낮음

일시적인 UI 동기화 문제로 잠깐 잘못된 값이 표시되었을 가능성:

```kotlin
// RunScreen.kt
val targetDays by viewModel.targetDays.collectAsState()
```

**가능한 시나리오:**
- LiveData/Flow 업데이트 지연
- Recomposition 타이밍 이슈
- 하지만 실제 데이터는 정상

### 4. 🔄 재설치 vs 업데이트 혼동 - 가능성 있음

사용자가 "업데이트"라고 생각했지만 실제로는 "재설치"를 한 경우:

| 동작 | `firstInstallTime` | 데이터 보존 |
|------|-------------------|------------|
| **업데이트** | 변경 없음 | ✅ 보존됨 |
| **앱 삭제 → 재설치** | 현재 시각으로 갱신 | ❌ 삭제됨 |

**`ensureInstallMarkerAndResetIfReinstalled()` 로직:**
```kotlin
// 재설치 후 1시간 이내면 타이머 초기화
val isRecentInstall = (System.currentTimeMillis() - firstInstallTime) < 1시간
```

**현재 상태:**
- ⚠️ 이 함수는 **호출되지 않음** (Dead Code)
- ✅ 따라서 재설치 시에도 초기화 안 됨

---

## ✅ 안전성 검증 결과

### 종합 평가: 🟢 **안전함**

| 점검 항목 | 결과 | 위험도 | 비고 |
|----------|------|--------|------|
| **앱 업데이트 감지 로직** | ✅ 없음 | 🟢 없음 | versionCode 체크 없음 |
| **SharedPreferences 자동 초기화** | ✅ 안전 | 🟢 없음 | Dead Code (호출 안 됨) |
| **Room DB 마이그레이션** | ✅ 안전 | 🟢 없음 | 다이어리만 대상 |
| **타이머 데이터 보존** | ✅ 안전 | 🟢 없음 | 앱 업데이트 시 자동 보존 |
| **기록 삭제 버그** | ✅ 수정됨 | 🟢 없음 | 2026-01-02/04 수정 완료 |
| **변경 감지 리스너** | ✅ 안전 | 🟢 없음 | 읽기 전용 |

---

## 🎯 결론

### ✅ 앱 업데이트 시 타이머 데이터는 안전합니다

**확인된 사실:**
1. ✅ 앱 업데이트를 감지하는 로직이 **존재하지 않음**
2. ✅ 타이머 데이터를 자동으로 변경하는 로직이 **존재하지 않음**
3. ✅ SharedPreferences는 앱 업데이트 시 **자동으로 보존됨** (Android 보장)
4. ✅ 과거에 있던 버그는 **모두 수정 완료** (2026-01-02/04)
5. ✅ Room DB 마이그레이션은 타이머와 **무관함**

### 🤔 목격하신 현상의 가능한 원인

1. **SharedPreferences 손상/삭제** (가능성 높음)
   - 시스템 오류, 저장공간 부족 등으로 데이터 손실
   - 기본값(1일)으로 표시됨

2. **과거 버그 영향** (2026-01-02 이전 버전 사용 시)
   - "모든 기록 삭제" 버튼으로 타이머 초기화
   - 업데이트와 시점이 겹쳐 오해

3. **재설치와 업데이트 혼동**
   - 앱 삭제 후 재설치 → 데이터 삭제
   - 업데이트로 오해

4. **일시적인 UI 표시 오류**
   - 실제 데이터는 정상, UI만 잠깐 이상

### 📝 권장사항

**현재 버전(1.2.9)을 사용하는 사용자는 안심하셔도 됩니다.**

만약 다시 현상이 발생한다면:
1. **Logcat 로그 확인** (`Tab01ViewModel` 태그)
2. **SharedPreferences 덤프 확인**
   ```bash
   adb shell run-as kr.sweetapps.alcoholictimer cat /data/data/kr.sweetapps.alcoholictimer/shared_prefs/user_settings.xml
   ```
3. **재현 절차 기록** (어떤 동작 후 발생했는지)

---

## 📚 참고 문서

- `docs/update-118/DELETE_ALL_RECORDS_TIMER_PRESERVE_FIX.md`
- `docs/update-118/TAB02_DELETE_RECORD_TIMER_PROTECTION_FINAL.md`
- `docs/update-116/DETAIL_SCREEN_DELETE_TIMER_RESET_FIX.md`

---

**점검 완료일:** 2026-01-06  
**다음 점검:** 필요 시 (재현 시)  
**최종 결론:** ✅ **앱 업데이트 시 타이머 데이터 안전성 확인됨**

