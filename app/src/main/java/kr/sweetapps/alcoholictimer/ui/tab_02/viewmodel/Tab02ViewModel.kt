package kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.model.SobrietyRecord
import kr.sweetapps.alcoholictimer.data.repository.RecordsDataLoader
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.util.utils.DateOverlapUtils
import kr.sweetapps.alcoholictimer.util.manager.TimerTimeManager
import java.util.Calendar

/**
 * [NEW] 통계 데이터 클래스
 * - 실시간으로 업데이트되는 통계 정보를 담음
 */
data class StatsData(
    val totalDays: Float = 0f,
    val totalKcal: Double = 0.0,
    val totalBottles: Double = 0.0,
    val savedMoney: Double = 0.0
)

/**
 * [CHANGED] 레벨 상태 데이터 클래스 (2025-12-25)
 * - 전체 누적 금주 일수(Lifetime Total)를 기준으로 레벨 관리
 * - 과거 기록 + 현재 타이머 합산 (= statsData.totalDays)
 */
data class LevelState(
    val currentLevel: kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.LevelInfo,
    val currentDays: Int,
    val progress: Float
)

/**
 * [NEW] Tab02(기록 화면) 상태 관리 ViewModel
 * - 기록 데이터 로딩 및 필터링 관리
 * - 기간 선택 상태 관리 (주/월/년)
 * - [FIX] 실시간 통계 계산 (진행 중인 타이머 포함)
 * - [FIX] SharedPreferences 변경 감지 (기록 추가/삭제 시 자동 갱신)
 */
class Tab02ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        Constants.USER_SETTINGS_PREFS,
        Context.MODE_PRIVATE
    )

    // [NEW] SharedPreferences 변경 감지 리스너
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            // [FIX] 기록 추가/삭제, 타이머 시작/완료 시 즉시 반영
            Constants.PREF_SOBRIETY_RECORDS,
            Constants.PREF_TIMER_COMPLETED,
            Constants.PREF_START_TIME -> {
                Log.d("Tab02ViewModel", "Data changed ($key), reloading records...")
                // 기록 목록 즉시 갱신 (QuitScreen에서 저장한 기록 반영)
                loadRecords()
            }
        }
    }

    // [NEW] 기록 목록 상태
    private val _records = MutableStateFlow<List<SobrietyRecord>>(emptyList())
    val records: StateFlow<List<SobrietyRecord>> = _records.asStateFlow()

    // [NEW] 로딩 상태
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // [NEW] 선택된 기간 (주/월/년) - 초기값은 빈 문자열 (외부에서 설정)
    private val _selectedPeriod = MutableStateFlow("")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    // [NEW] 선택된 세부 기간 (예: "2025년 12월") - 초기값은 빈 문자열 (외부에서 설정)
    private val _selectedDetailPeriod = MutableStateFlow("")
    val selectedDetailPeriod: StateFlow<String> = _selectedDetailPeriod.asStateFlow()

    // [NEW] 선택된 주 범위
    private val _selectedWeekRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val selectedWeekRange: StateFlow<Pair<Long, Long>?> = _selectedWeekRange.asStateFlow()

    // [NEW] 실시간 통계 데이터
    private val _statsState = MutableStateFlow(StatsData())
    val statsState: StateFlow<StatsData> = _statsState.asStateFlow()

    // [CHANGED] 레벨 상태 데이터 (전체 누적 일수 기준) (2025-12-25)
    private val _levelState = MutableStateFlow(
        LevelState(
            currentLevel = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.levels.first(),
            currentDays = 0,
            progress = 0f
        )
    )
    val levelState: StateFlow<LevelState> = _levelState.asStateFlow()

    // [NEW] 타이머 시작 시각 (인디케이터 표시용) (2026-01-02)
    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    // [NEW] 타이머 완료 여부 (인디케이터 색상 제어용) (2026-01-02)
    private val _isTimerCompleted = MutableStateFlow(false)
    val isTimerCompleted: StateFlow<Boolean> = _isTimerCompleted.asStateFlow()

    // [NEW] 상세 화면 이동을 위한 일회성 이벤트 (Route 저장)
    // 이 값이 null이 아니면 목록 화면이 해당 route로 즉시 이동함
    private val _pendingDetailRoute = MutableStateFlow<String?>(null)
    val pendingDetailRoute: StateFlow<String?> = _pendingDetailRoute.asStateFlow()

    /**
     * [NEW] 상세 화면 이동 예약
     * @param route 이동할 DetailScreen의 전체 경로
     */
    fun setPendingDetailRoute(route: String) {
        Log.d("Tab02ViewModel", "Detail route pending: $route")
        _pendingDetailRoute.value = route
    }

    /**
     * [NEW] 상세 화면 이동 이벤트 소비 (한 번만 처리)
     */
    fun consumePendingDetailRoute() {
        Log.d("Tab02ViewModel", "Detail route consumed")
        _pendingDetailRoute.value = null
    }

    init {
        // [FIX] SharedPreferences 변경 감지 시작
        sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        Log.d("Tab02ViewModel", "Preference change listener registered")

        // [NEW] 과거 기록을 UserStatusManager에 주입 (2025-12-25)
        viewModelScope.launch {
            _records.collect { allRecords ->
                // 필터 없이 모든 기록의 '총 금주 일수' 합산
                val totalHistoryDays = allRecords.sumOf { record ->
                    // overlapDays에 null을 넣으면 전체 기간(startTime ~ endTime) 계산됨
                    DateOverlapUtils.overlapDays(record.startTime, record.endTime, null, null)
                }
                // ★핵심: Float로 전달 (소수점 유지)
                kr.sweetapps.alcoholictimer.util.manager.UserStatusManager.updateHistoryDays(totalHistoryDays.toFloat())

                Log.d("Tab02ViewModel", "Updated History to Manager: $totalHistoryDays days (precise, from ${allRecords.size} records)")
            }
        }

        // [NEW] UserStatusManager 구독하여 레벨 상태 동기화 (2025-12-26)
        viewModelScope.launch {
            kr.sweetapps.alcoholictimer.util.manager.UserStatusManager.userStatus.collect { userStatus ->
                // ★핵심: UserStatusManager의 정밀한 데이터 사용
                val currentLevel = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.getLevelInfo(userStatus.days)

                // 다음 레벨 찾기
                val currentIndex = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.levels.indexOf(currentLevel)
                val progress = if (currentIndex == kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.levels.size - 1) {
                    1.0f // Legend (최고 레벨)
                } else {
                    // ★핵심: Float 오버로딩 버전 사용 (소수점 정밀도)
                    kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.getLevelProgress(userStatus.totalDaysPrecise)
                }

                _levelState.value = LevelState(
                    currentLevel = currentLevel,
                    currentDays = userStatus.days,
                    progress = progress
                )

                Log.d("Tab02ViewModel", "Level synced from Manager: Lv.${userStatus.level}, ${userStatus.totalDaysPrecise} days, ${(progress * 100).toInt()}% progress")
            }
        }

        // [CHANGED] combine을 사용하여 통계 재계산 (레벨 계산은 제거) (2025-12-26)
        viewModelScope.launch {
            combine(
                _records,
                _selectedPeriod,
                _selectedDetailPeriod,
                _selectedWeekRange,
                TimerTimeManager.elapsedMillis
            ) { records, period, detailPeriod, weekRange, elapsedMillis ->
                // 통계 계산 (과거 기록 + 현재 타이머 합산)
                calculateStatsFromAllStates(records, period, detailPeriod, weekRange, elapsedMillis)
            }.collect { statsData ->
                // 계산된 통계를 StateFlow에 반영
                _statsState.value = statsData

                // [REMOVED] updateLevelStateFromTotalDays 호출 제거 (2025-12-26)
                // 레벨 상태는 위의 UserStatusManager 구독 블록에서 자동으로 업데이트됨
            }
        }
    }

    /**
     * [REFACTORED] 통계 계산 (모든 상태를 파라미터로 받아 계산)
     * @param allRecords 모든 기록 목록
     * @param period 선택된 기간 (주/월/년/전체)
     * @param detailPeriod 세부 기간 (예: "2025년 12월")
     * @param weekRange 선택된 주 범위
     * @param currentTimerElapsed 현재 타이머의 경과 시간 (배속 이미 적용됨)
     * @return 계산된 통계 데이터
     */
    private fun calculateStatsFromAllStates(
        allRecords: List<SobrietyRecord>,
        period: String,
        detailPeriod: String,
        weekRange: Pair<Long, Long>?,
        currentTimerElapsed: Long
    ): StatsData {
        return try {
            // 1. 현재 기간 범위 계산
            val rangeFilter: Pair<Long, Long>? = when {
                period.contains("주") || period.contains("Week") -> {
                    weekRange ?: getCurrentWeekRange()
                }
                period.contains("월") || period.contains("Month") -> {
                    if (detailPeriod.isNotEmpty()) {
                        parseMonthRange(detailPeriod)
                    } else {
                        getCurrentMonthRange()
                    }
                }
                period.contains("년") || period.contains("Year") -> {
                    if (detailPeriod.isNotEmpty()) {
                        parseYearRange(detailPeriod)
                    } else {
                        getCurrentYearRange()
                    }
                }
                else -> null // 전체 기간
            }

            // 2. 사용자 설정값 가져오기
            val (costIndex, freqIndex, durationIndex) = Constants.getUserSettings(getApplication())
            val costPerTime = Constants.DrinkingSettings.getCostValue(costIndex)
            val timesPerWeek = Constants.DrinkingSettings.getFrequencyValue(freqIndex)

            // 3. 저장된 기록 합산
            var totalDaysFromRecords = 0.0
            allRecords.forEach { record ->
                val overlap = if (rangeFilter != null) {
                    DateOverlapUtils.overlapDays(
                        record.startTime,
                        record.endTime,
                        rangeFilter.first,
                        rangeFilter.second
                    )
                } else {
                    DateOverlapUtils.overlapDays(record.startTime, record.endTime, null, null)
                }
                totalDaysFromRecords += overlap
            }

            // 4. [REFACTORED] 진행 중인 타이머 - TimerTimeManager에서 받은 값 사용
            val startTime = sharedPref.getLong(Constants.PREF_START_TIME, 0L)
            val timerCompleted = sharedPref.getBoolean(Constants.PREF_TIMER_COMPLETED, false)

            // [NEW] startTime StateFlow 업데이트 (인디케이터 표시용) (2026-01-02)
            _startTime.value = startTime
            // [NEW] isTimerCompleted StateFlow 업데이트 (인디케이터 색상 제어용) (2026-01-02)
            _isTimerCompleted.value = timerCompleted

            var totalDaysFromCurrentTimer = 0.0
            if (startTime > 0 && !timerCompleted && currentTimerElapsed > 0) {
                // [FIX] 가상 종료 시간 계산 (배속 적용된 시간)
                val virtualEndTime = startTime + currentTimerElapsed

                if (rangeFilter != null) {
                    // [FIX] 필터 기간과 타이머 기간의 '교집합'만 계산
                    // 과거 주를 선택한 경우, 타이머 시작일이 필터 종료일보다 미래면 overlap = 0
                    val overlapDays = DateOverlapUtils.overlapDays(
                        startTime,
                        virtualEndTime,
                        rangeFilter.first,
                        rangeFilter.second
                    )
                    totalDaysFromCurrentTimer = overlapDays

                    // [DEBUG] 필터링 결과 상세 로그
                    Log.d("Tab02ViewModel", "=== Timer Filtering Debug ===")
                    Log.d("Tab02ViewModel", "Timer: start=$startTime, virtualEnd=$virtualEndTime")
                    Log.d("Tab02ViewModel", "Filter: ${rangeFilter.first} to ${rangeFilter.second}")
                    Log.d("Tab02ViewModel", "Overlap: $overlapDays days")

                    // [FIX] 명확한 검증: 타이머가 필터 범위를 벗어났는지 확인
                    if (startTime > rangeFilter.second) {
                        Log.d("Tab02ViewModel", "⚠️ Timer started AFTER filter period - forcing 0")
                        totalDaysFromCurrentTimer = 0.0
                    } else if (virtualEndTime < rangeFilter.first) {
                        Log.d("Tab02ViewModel", "⚠️ Timer ended BEFORE filter period - forcing 0")
                        totalDaysFromCurrentTimer = 0.0
                    }
                } else {
                    // 전체 기간: TimerTimeManager 값 그대로 사용
                    val timerDaysPrecise = (currentTimerElapsed / Constants.DAY_IN_MILLIS.toDouble())
                    totalDaysFromCurrentTimer = timerDaysPrecise
                    Log.d("Tab02ViewModel", "Timer (no filter): $timerDaysPrecise days")
                }
            } else {
                Log.d("Tab02ViewModel", "No active timer: startTime=$startTime, completed=$timerCompleted, elapsed=$currentTimerElapsed")
            }

            // 5. 총합 계산
            val totalDays = totalDaysFromRecords + totalDaysFromCurrentTimer
            val weeks = totalDays / 7.0
            val savedMoney = weeks * timesPerWeek * costPerTime
            val totalBottles = weeks * timesPerWeek
            val totalKcal = totalBottles * Constants.DrinkingSettings.CALORIES_PER_DRINK // [FIX] 상수로 관리

            // 6. StatsData 반환
            StatsData(
                totalDays = totalDays.toFloat(),
                totalKcal = totalKcal,
                totalBottles = totalBottles,
                savedMoney = savedMoney
            )

        } catch (e: Exception) {
            Log.e("Tab02ViewModel", "통계 계산 실패", e)
            StatsData() // 오류 발생 시 빈 데이터 반환
        }
    }

    /**
     * [CHANGED] 레벨 상태 계산 (누적 일수 기준) (2025-12-25)
     * - statsData.totalDays를 기준으로 레벨 계산 (과거 기록 + 현재 타이머 합산)
     * - 이전 로직과 달리, '전체 누적 금주 일수'를 반영하여 레벨 유지/하락이 가능
     * @param totalDays 전체 누적 금주 일수 (Float)
     */
    private fun updateLevelStateFromTotalDays(totalDays: Float) {
        try {
            // 1. 누적 일수가 0 이하면 초기 상태 (0일, Lv.1)
            if (totalDays <= 0f) {
                _levelState.value = LevelState(
                    currentLevel = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.levels.first(),
                    currentDays = 0,
                    progress = 0f
                )
                Log.d("Tab02ViewModel", "Level: No days - reset to 0 days, Lv.1")
                return
            }

            // 2. 현재 일수 계산 (floor 방식, '꽉 채운 일수')
            val currentDays = totalDays.toInt()

            // 3. 현재 레벨 정보 가져오기
            val currentLevel = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.getLevelInfo(currentDays)

            // 4. 다음 레벨까지의 진행률 계산
            val currentIndex = kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.levels.indexOf(currentLevel)
            val nextLevel = if (currentIndex in 0 until kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.levels.size - 1) {
                kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions.levels[currentIndex + 1]
            } else {
                null // Legend 레벨은 다음 레벨 없음
            }

            val progress = if (nextLevel != null && nextLevel.start > currentLevel.start) {
                val progressInLevel = currentDays - currentLevel.start
                val totalNeeded = nextLevel.start - currentLevel.start
                (progressInLevel.toFloat() / totalNeeded.toFloat()).coerceIn(0f, 1f)
            } else {
                1f // Legend 레벨은 100%
            }

            // 5. 상태 업데이트
            _levelState.value = LevelState(
                currentLevel = currentLevel,
                currentDays = currentDays,
                progress = progress
            )

            Log.d("Tab02ViewModel", "Level: $currentDays days (from totalDays=$totalDays), Lv.${currentIndex + 1}, progress=${(progress * 100).toInt()}%")

        } catch (e: Exception) {
            Log.e("Tab02ViewModel", "레벨 계산 실패", e)
            // 오류 발생 시 초기 상태 유지
        }
    }

    /**
     * [NEW] 현재 주 범위 가져오기
     */
    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val weekStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val weekEndInclusive = cal.timeInMillis + (24 * 60 * 60 * 1000L - 1)
        return weekStart to weekEndInclusive
    }

    /**
     * [NEW] 월 범위 파싱
     */
    private fun parseMonthRange(detailPeriod: String): Pair<Long, Long> {
        val numbers = Regex("(\\d+)").findAll(detailPeriod).map { it.value.toInt() }.toList()
        return if (numbers.size >= 2) {
            // [FIX] 큰 숫자(1000 이상)를 연도로, 작은 숫자를 월로 자동 판별 (2025-12-25)
            // 예: "12/2025" → year=2025, month=12 또는 "2025/12" → year=2025, month=12
            val num1 = numbers[0]
            val num2 = numbers[1]
            val year = if (num1 > 100) num1 else num2
            val month = (if (num1 > 100) num2 else num1) - 1 // 월은 0-indexed (Calendar.JANUARY = 0)

            val cal = Calendar.getInstance()
            cal.set(year, month, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            val monthEnd = cal.timeInMillis

            Log.d("Tab02ViewModel", "parseMonthRange: input='$detailPeriod' → year=$year, month=${month+1}, range=$monthStart~$monthEnd")
            monthStart to monthEnd
        } else {
            getCurrentMonthRange()
        }
    }

    /**
     * [NEW] 년 범위 파싱
     */
    private fun parseYearRange(detailPeriod: String): Pair<Long, Long> {
        val yearMatch = Regex("(\\d{4})").find(detailPeriod)
        return if (yearMatch != null) {
            val year = yearMatch.groupValues[1].toInt()
            getYearRange(year)
        } else {
            getCurrentYearRange()
        }
    }

    /**
     * [FIXED] 초기 기간 설정 (전략적 UX 적용)
     * - 앱 최초 실행 시: defaultFilter(All)로 설정
     * - 탭 이동 후 복귀 시: 기존 선택값 유지 (if empty 체크 덕분)
     */
    fun initializePeriod(defaultFilter: String) {
        // [핵심] 값이 비어있을 때만(최초 실행 시) 초기화합니다.
        // 이미 사용자가 'Month'나 'Week'를 선택했다면, 이 조건문이 false가 되어 무시됩니다.
        if (_selectedPeriod.value.isEmpty()) {
            _selectedPeriod.value = defaultFilter
            _selectedDetailPeriod.value = "" // 'All'은 세부 기간 텍스트가 필요 없음
        }
    }

    /**
     * [NEW] 기록 데이터 로딩
     */
    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedRecords = RecordsDataLoader.loadSobrietyRecords(getApplication())
                _records.value = loadedRecords
                Log.d("Tab02ViewModel", "기록 로딩 완료: ${loadedRecords.size}개")
            } catch (e: Exception) {
                Log.e("Tab02ViewModel", "기록 로딩 실패", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * [NEW] 선택된 기간 업데이트
     */
    fun updateSelectedPeriod(period: String) {
        _selectedPeriod.value = period
        _selectedDetailPeriod.value = ""
        if (period != _selectedPeriod.value) {
            _selectedWeekRange.value = null
        }
    }

    /**
     * [NEW] 선택된 세부 기간 업데이트
     */
    fun updateSelectedDetailPeriod(detailPeriod: String) {
        _selectedDetailPeriod.value = detailPeriod
    }

    /**
     * [NEW] 선택된 주 범위 업데이트
     */
    fun updateSelectedWeekRange(weekRange: Pair<Long, Long>?) {
        _selectedWeekRange.value = weekRange
    }

    /**
     * [NEW] 필터링된 기록 가져오기
     */
    fun getFilteredRecords(
        periodWeek: String,
        periodMonth: String,
        periodYear: String
    ): List<SobrietyRecord> {
        val period = _selectedPeriod.value
        val detailPeriod = _selectedDetailPeriod.value
        val weekRange = _selectedWeekRange.value
        val allRecords = _records.value

        return when (period) {
            periodWeek -> {
                val range = weekRange ?: run {
                    val cal = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.SUNDAY
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    }
                    val weekStart = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_WEEK, 6)
                    val weekEndInclusive = cal.timeInMillis + (24 * 60 * 60 * 1000L - 1)
                    weekStart to weekEndInclusive
                }
                allRecords.filter { it.endTime >= range.first && it.startTime <= range.second }
            }
            periodMonth -> {
                val range: Pair<Long, Long> = if (detailPeriod.isNotEmpty()) {
                    val numbers = Regex("(\\d+)").findAll(detailPeriod).map { it.value.toInt() }.toList()
                    if (numbers.size >= 2) {
                        val year = numbers[0]
                        val month = numbers[1] - 1
                        val cal = Calendar.getInstance()
                        cal.set(year, month, 1, 0, 0, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val monthStart = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        cal.add(Calendar.MILLISECOND, -1)
                        val monthEnd = cal.timeInMillis
                        monthStart to monthEnd
                    } else {
                        getCurrentMonthRange()
                    }
                } else {
                    getCurrentMonthRange()
                }
                allRecords.filter { it.endTime >= range.first && it.startTime <= range.second }
            }
            periodYear -> {
                val range: Pair<Long, Long> = if (detailPeriod.isNotEmpty()) {
                    val yearMatch = Regex("(\\d{4})").find(detailPeriod)
                    if (yearMatch != null) {
                        val year = yearMatch.groupValues[1].toInt()
                        getYearRange(year)
                    } else {
                        getCurrentYearRange()
                    }
                } else {
                    getCurrentYearRange()
                }
                allRecords.filter { it.endTime >= range.first && it.startTime <= range.second }
            }
            else -> allRecords
        }
    }

    /**
     * [NEW] 현재 월 범위 가져오기
     */
    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val monthEnd = cal.timeInMillis
        return monthStart to monthEnd
    }

    /**
     * [NEW] 현재 년 범위 가져오기
     */
    private fun getCurrentYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yearStart = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val yearEnd = cal.timeInMillis
        return yearStart to yearEnd
    }

    /**
     * [NEW] 특정 년도의 범위 가져오기
     */
    private fun getYearRange(year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, 0, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yearStart = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val yearEnd = cal.timeInMillis
        return yearStart to yearEnd
    }

    /**
     * [NEW] ViewModel 종료 시 리스너 해제 (메모리 누수 방지)
     */
    override fun onCleared() {
        super.onCleared()
        sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        Log.d("Tab02ViewModel", "Preference change listener unregistered")
    }
}