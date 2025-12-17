package kr.sweetapps.alcoholictimer.util

import kr.sweetapps.alcoholictimer.R

/**
 * 아바타 관리자
 * 20개의 프리셋 아바타(Vector Assets)를 관리합니다.
 *
 * Firestore에는 인덱스(0~19)만 저장하고,
 * 화면에는 로컬 리소스를 즉시 로딩합니다.
 */
object AvatarManager {

    /**
     * 20개의 아바타 리소스 ID 목록
     * avatar_00 ~ avatar_19
     */
    val avatars = listOf(
        R.drawable.avatar_00,
        R.drawable.avatar_01,
        R.drawable.avatar_02,
        R.drawable.avatar_03,
        R.drawable.avatar_04,
        R.drawable.avatar_05,
        R.drawable.avatar_06,
        R.drawable.avatar_07,
        R.drawable.avatar_08,
        R.drawable.avatar_09,
        R.drawable.avatar_10,
        R.drawable.avatar_11,
        R.drawable.avatar_12,
        R.drawable.avatar_13,
        R.drawable.avatar_14,
        R.drawable.avatar_15,
        R.drawable.avatar_16,
        R.drawable.avatar_17,
        R.drawable.avatar_18,
        R.drawable.avatar_19
    )

    /**
     * 인덱스로 아바타 리소스 ID를 가져옵니다.
     *
     * @param index 아바타 인덱스 (0~19)
     * @return 아바타 리소스 ID (R.drawable.avatar_xx)
     *
     * 안전 장치:
     * - 인덱스가 0~19 범위를 벗어나면 0번 아바타 반환
     * - null 또는 음수인 경우 0번 아바타 반환
     */
    fun getAvatarResId(index: Int?): Int {
        return try {
            when {
                index == null -> avatars[0]
                index < 0 -> avatars[0]
                index >= avatars.size -> avatars[0]
                else -> avatars[index]
            }
        } catch (e: Exception) {
            // 예외 발생 시에도 0번 아바타 반환
            avatars[0]
        }
    }

    /**
     * 전체 아바타 개수
     */
    val count: Int = avatars.size

    /**
     * 유효한 인덱스인지 확인
     */
    fun isValidIndex(index: Int?): Boolean {
        return index != null && index >= 0 && index < avatars.size
    }
}

