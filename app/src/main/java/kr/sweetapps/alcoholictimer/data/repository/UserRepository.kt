package kr.sweetapps.alcoholictimer.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 사용자 프로필 Repository
 * Firestore 컬렉션: users
 *
 * 아바타 시스템 관리
 * (v2.0) SharedPreferences를 사용한 로컬 저장
 */
class UserRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "UserRepository"
        private const val KEY_AVATAR_INDEX = "avatar_index"
        private const val KEY_INSTALLATION_ID = "installation_id"
    }

    /**
     * [NEW] Phase 3: 기기 고유 ID 가져오기
     * 앱 설치 시 한 번만 생성되고 계속 유지됨
     */
    fun getInstallationId(): String {
        var installationId = prefs.getString(KEY_INSTALLATION_ID, null)
        if (installationId == null) {
            installationId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_INSTALLATION_ID, installationId).apply()
            Log.d(TAG, "새로운 Installation ID 생성: $installationId")
        }
        return installationId
    }

    /**
     * 사용자의 아바타 인덱스 업데이트
     *
     * @param index 아바타 인덱스 (0~19)
     * @return 성공 여부
     */
    suspend fun updateAvatar(index: Int): Boolean {
        return try {
            // SharedPreferences에 저장
            prefs.edit().putInt(KEY_AVATAR_INDEX, index).apply()

            Log.d(TAG, "updateAvatar: 성공 - avatarIndex=$index")
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateAvatar: 실패", e)
            false
        }
    }

    /**
     * 사용자의 현재 아바타 인덱스 가져오기
     *
     * @return 아바타 인덱스 (없으면 0 반환)
     */
    suspend fun getAvatarIndex(): Int {
        return try {
            val avatarIndex = prefs.getInt(KEY_AVATAR_INDEX, 0)

            Log.d(TAG, "getAvatarIndex: avatarIndex=$avatarIndex")
            avatarIndex
        } catch (e: Exception) {
            Log.e(TAG, "getAvatarIndex: 실패, 기본값 0 반환", e)
            0
        }
    }
}

