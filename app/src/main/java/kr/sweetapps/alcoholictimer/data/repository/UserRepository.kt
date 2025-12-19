package kr.sweetapps.alcoholictimer.data.repository

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.UUID

/**
 * [Standardized] 사용자 프로필 Repository
 * - 안드로이드 표준 저장소(DefaultSharedPreferences) 사용
 * - 데이터 디버깅을 위한 전체 키 로깅 기능 포함
 */
class UserRepository(private val context: Context) {

    // [업계 표준] 모든 앱 설정은 '기본 저장소' 하나로 관리합니다.
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val TAG = "UserRepository"
        private const val KEY_INSTALLATION_ID = "installation_id"
        private const val KEY_AVATAR_INDEX = "avatar_index"

        // [CHECK] 로그(SAVED_ALL_DATA)를 확인한 후, 실제 저장된 키 이름으로 수정이 필요할 수 있습니다.
        private const val KEY_NICKNAME = "nickname"
    }

    /**
     * [Standard] 닉네임 가져오기 (하위 호환성 유지)
     * 1. DefaultSharedPreferences 확인
     * 2. 없으면 user_settings (레거시) 확인
     * 3. 레거시에서 찾으면 자동으로 표준 저장소로 마이그레이션
     */
    fun getNickname(): String? {
        // [DEBUG] 현재 저장소의 모든 데이터를 출력하여 '진짜 닉네임 키'를 찾습니다.
        val allEntries = prefs.all
        Log.d("SAVED_ALL_DATA", ">>> 현재 저장된 모든 설정값: $allEntries")

        // 1. 표준 저장소 확인
        var nickname = prefs.getString(KEY_NICKNAME, null)
        Log.d(TAG, "DefaultSharedPreferences 조회 결과: '$nickname' (Key: '$KEY_NICKNAME')")

        // 2. 없으면 구버전 저장소(user_settings) 확인 (마이그레이션)
        if (nickname.isNullOrBlank()) {
            Log.d(TAG, "표준 저장소에 닉네임 없음. 레거시 저장소(user_settings) 확인 중...")
            try {
                val legacyPrefs = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                nickname = legacyPrefs.getString("nickname", null)
                Log.d(TAG, "Legacy 저장소 조회 결과: '$nickname'")

                if (!nickname.isNullOrBlank()) {
                    // 구버전 데이터를 찾았으면 표준 저장소로 이사(저장) 시킴
                    saveNickname(nickname)
                    Log.i(TAG, "✅ Legacy 닉네임 마이그레이션 완료: '$nickname' (user_settings → DefaultSharedPreferences)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "마이그레이션 실패", e)
            }
        } else {
            Log.d(TAG, "✅ 표준 저장소에서 닉네임 발견: '$nickname'")
        }

        return nickname
    }

    /**
     * [Standard] 닉네임 저장하기
     * - 이 함수를 사용하면 표준 위치에 확실하게 저장됩니다.
     */
    fun saveNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
        Log.d(TAG, "닉네임 저장 완료: $nickname (Key: $KEY_NICKNAME)")
    }

    // --- 기존 기능 유지 (Installation ID & Avatar) ---

    fun getInstallationId(): String {
        var installationId = prefs.getString(KEY_INSTALLATION_ID, null)
        if (installationId == null) {
            installationId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_INSTALLATION_ID, installationId).apply()
            Log.d(TAG, "새로운 Installation ID 생성: $installationId")
        }
        return installationId
    }

    suspend fun getAvatarIndex(): Int {
        return prefs.getInt(KEY_AVATAR_INDEX, 0)
    }

    suspend fun updateAvatar(index: Int): Boolean {
        prefs.edit().putInt(KEY_AVATAR_INDEX, index).apply()
        return true
    }
}

