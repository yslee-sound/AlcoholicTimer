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

        // [FIX] combine을 사용하여 5가지 상태 중 하나라도 변하면 즉시 통계 재계산
        viewModelScope.launch {
            combine(
                _records,
                _selectedPeriod,
                _selectedDetailPeriod,
                _selectedWeekRange,
                TimerTimeManager.elapsedMillis
            ) { records, period, detailPeriod, weekRange, elapsedMillis ->
                // [FIX] 모든 상태를 파라미터로 받아 통계 계산
                calculateStatsFromAllStates(records, period, detailPeriod, weekRange, elapsedMillis)
            }.collect { statsData ->
                // 계산된 결과를 StateFlow에 반영
                _statsState.value = statsData
                Log.d("Tab02ViewModel", "Stats updated: totalDays=${statsData.totalDays}, savedMoney=${statsData.savedMoney}")
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