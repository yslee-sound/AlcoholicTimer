package kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
class Tab02ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        Constants.USER_SETTINGS_PREFS,
        Context.MODE_PRIVATE
    )

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

    init {
        // [REFACTORED] TimerTimeManager의 elapsedMillis를 구독하여 통계 갱신
        viewModelScope.launch {
            TimerTimeManager.elapsedMillis.collect { elapsedMillis ->
                calculateStatsFromElapsed(elapsedMillis)
            }
        }
    }

    /**
     * [REFACTORED] 통계 계산 (TimerTimeManager에서 받은 시간 사용)
     * @param currentTimerElapsed 현재 타이머의 경과 시간 (배속 이미 적용됨)
     */
    private fun calculateStatsFromElapsed(currentTimerElapsed: Long) {
        try {
            val allRecords = _records.value
            val period = _selectedPeriod.value
            val detailPeriod = _selectedDetailPeriod.value
            val weekRange = _selectedWeekRange.value

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
            val hoursPerTime = Constants.DrinkingSettings.getDurationValue(durationIndex)

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
                // [FIX] Tab 1, Tab 3와 동일하게 '순수 경과 일수(Duration)'로 통일
                // 기존의 +1.0 보정 제거 (0-based 순수 경과 시간)
                val timerDaysPrecise = (currentTimerElapsed / Constants.DAY_IN_MILLIS.toDouble())

                totalDaysFromCurrentTimer = if (rangeFilter != null) {
                    // 범위 필터가 있을 때는 간단한 포함 여부 체크
                    val now = System.currentTimeMillis()
                    val timerStartInRange = startTime >= rangeFilter.first
                    val timerNowInRange = now <= rangeFilter.second

                    if (timerStartInRange && timerNowInRange) {
                        timerDaysPrecise
                    } else {
                        // [FIX] 일부만 겹치면 DateOverlapUtils 사용 (보정 제거)
                        DateOverlapUtils.overlapDays(
                            startTime,
                            now,
                            rangeFilter.first,
                            rangeFilter.second
                        )
                    }
                } else {
                    // 전체 기간: TimerTimeManager 값 그대로 사용
                    timerDaysPrecise
                }
            }

            // 5. 총합 계산
            val totalDays = totalDaysFromRecords + totalDaysFromCurrentTimer
            val weeks = totalDays / 7.0
            val savedMoney = weeks * timesPerWeek * costPerTime
            val totalBottles = weeks * timesPerWeek
            val totalKcal = totalBottles * 200.0 // 1병당 약 200kcal

            // 6. StateFlow 업데이트
            _statsState.value = StatsData(
                totalDays = totalDays.toFloat(),
                totalKcal = totalKcal,
                totalBottles = totalBottles,
                savedMoney = savedMoney
            )

        } catch (e: Exception) {
            Log.e("Tab02ViewModel", "통계 계산 실패", e)
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
     * [NEW] 초기 기간 설정 (화면 진입 시 한 번만 호출)
     */
    fun initializePeriod(periodMonth: String, initialDateText: String) {
        if (_selectedPeriod.value.isEmpty()) {
            _selectedPeriod.value = periodMonth
            _selectedDetailPeriod.value = initialDateText
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
}