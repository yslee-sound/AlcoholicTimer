package com.example.alcoholictimer.utils

import java.util.Calendar
import java.util.TimeZone

object DateOverlapUtils {
    const val DAY_MS: Long = 24L * 60L * 60L * 1000L

    /**
     * 기록 구간 [recordStart, recordEnd] 과 기간 [periodStart, periodEnd] 가 겹치는 일수를 반환.
     * periodStart/periodEnd 가 null이면 기록 전체 기간을 일수로 반환.
     */
    @JvmStatic
    fun overlapDays(
        recordStart: Long,
        recordEnd: Long,
        periodStart: Long?,
        periodEnd: Long?
    ): Double {
        val safeStart = recordStart
        val safeEnd = recordEnd
        if (periodStart == null || periodEnd == null) {
            val duration = (safeEnd - safeStart).coerceAtLeast(0L)
            return duration / DAY_MS.toDouble()
        }
        val overlapStart = maxOf(safeStart, periodStart)
        val overlapEnd = minOf(safeEnd, periodEnd)
        val overlapMs = (overlapEnd - overlapStart).coerceAtLeast(0L)
        return overlapMs / DAY_MS.toDouble()
    }

    /**
     * 주어진 연/월(1-12)의 시작(해당 달 1일 00:00:00.000)과 끝(다음달 0ms 전)의 시간 범위를 반환.
     * 기본 타임존은 시스템 기본값. 필요시 Asia/Seoul 등 명시해 사용.
     */
    @JvmStatic
    fun monthRange(year: Int, month1to12: Int, timeZone: TimeZone = TimeZone.getDefault()): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone)
        cal.set(year, month1to12 - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return start to end
    }

    /** 편의 함수: 특정 타임존 기준으로 yyyy-MM-dd HH:mm 의 epoch ms 생성 */
    @JvmStatic
    fun ms(
        year: Int,
        month1to12: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone)
        cal.set(year, month1to12 - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

