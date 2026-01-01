package kr.sweetapps.alcoholictimer.util.notification

import android.content.Context
import kr.sweetapps.alcoholictimer.R

/**
 * 리텐션 알림 메시지 유틸리티
 *
 * [UPDATED] 다국어(i18n) 지원 - strings.xml 기반으로 변경 (2026-01-02)
 * 하드코딩된 문자열 대신 Context를 통해 리소스에서 문구를 가져옴
 *
 * @since 2025-12-31
 */
object RetentionMessages {

    // ==================== 그룹 A: 신규 유저 ====================

    object GroupA {
        // 1차 알림 (24시간 후)
        val TITLE_1_RES = R.string.notif_group_a_1_title
        val MESSAGE_1_RES = R.string.notif_group_a_1_body

        // 2차 알림 (1차 발송 2일 후 = 3일차)
        val TITLE_2_RES = R.string.notif_group_a_2_title
        val MESSAGE_2_RES = R.string.notif_group_a_2_body

        // 3차 알림 (2차 발송 4일 후 = 7일차)
        val TITLE_3_RES = R.string.notif_group_a_3_title
        val MESSAGE_3_RES = R.string.notif_group_a_3_body

        // Helper functions
        fun getTitle1(context: Context) = context.getString(TITLE_1_RES)
        fun getMessage1(context: Context) = context.getString(MESSAGE_1_RES)
        fun getTitle2(context: Context) = context.getString(TITLE_2_RES)
        fun getMessage2(context: Context) = context.getString(MESSAGE_2_RES)
        fun getTitle3(context: Context) = context.getString(TITLE_3_RES)
        fun getMessage3(context: Context) = context.getString(MESSAGE_3_RES)
    }

    // ==================== 그룹 B: 활성 유저 ====================

    object GroupB {
        // 3일 알림
        val TITLE_3D_RES = R.string.notif_group_b_3day_title
        val MESSAGE_3D_RES = R.string.notif_group_b_3day_body

        // 7일 알림
        val TITLE_7D_RES = R.string.notif_group_b_7day_title
        val MESSAGE_7D_RES = R.string.notif_group_b_7day_body

        // 30일 알림
        val TITLE_30D_RES = R.string.notif_group_b_30day_title
        val MESSAGE_30D_RES = R.string.notif_group_b_30day_body

        // Helper functions
        fun getTitle3D(context: Context) = context.getString(TITLE_3D_RES)
        fun getMessage3D(context: Context) = context.getString(MESSAGE_3D_RES)
        fun getTitle7D(context: Context) = context.getString(TITLE_7D_RES)
        fun getMessage7D(context: Context) = context.getString(MESSAGE_7D_RES)
        fun getTitle30D(context: Context) = context.getString(TITLE_30D_RES)
        fun getMessage30D(context: Context) = context.getString(MESSAGE_30D_RES)
    }

    // ==================== 그룹 C: 휴식 유저 ====================

    object GroupC {
        // D+1 알림 (24시간 후)
        val TITLE_D1_RES = R.string.notif_group_c_1_title
        val MESSAGE_D1_RES = R.string.notif_group_c_1_body

        // D+3 알림 (3일 후)
        val TITLE_D3_RES = R.string.notif_group_c_2_title
        val MESSAGE_D3_RES = R.string.notif_group_c_2_body

        // Helper functions
        fun getTitleD1(context: Context) = context.getString(TITLE_D1_RES)
        fun getMessageD1(context: Context) = context.getString(MESSAGE_D1_RES)
        fun getTitleD3(context: Context) = context.getString(TITLE_D3_RES)
        fun getMessageD3(context: Context) = context.getString(MESSAGE_D3_RES)
    }
}

