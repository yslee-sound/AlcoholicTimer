package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kr.sweetapps.alcoholictimer.data.supabase.model.UpdatePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 업데이트 정책 저장소
 *
 * Supabase의 update_policies 테이블과 연동하여
 * 앱 업데이트 정책을 가져옵니다.
 *
 * 우선순위:
 * 1. is_active = true인 정책만 조회
 * 2. 현재 앱 버전보다 높은 버전만 조회
 * 3. 대상 버전 범위에 포함되는 정책만 조회
 *
 * @property client Supabase 클라이언트
 * @property context 앱 Context
 */
class UpdatePolicyRepository(
    private val client: SupabaseClient,
    private val context: Context
) {
    companion object {
        private const val TABLE_NAME = "update_policy"
        private const val PREF_NAME = "update_policy_prefs"
        private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
        private const val KEY_LATER_COUNT_PREFIX = "update_later_count_"
        private const val KEY_LAST_DISMISSED_PREFIX = "update_last_dismissed_"
    }

    /**
     * 현재 앱 버전 코드를 가져옵니다.
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    .longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 현재 앱 버전 이름을 가져옵니다.
     */
    private fun getCurrentVersionName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    .versionName ?: "1.0.0"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName ?: "1.0.0"
            }
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * 활성화된 업데이트 정책을 가져옵니다.
     *
     * @return 표시할 업데이트 정책 또는 null
     */
    suspend fun getActivePolicy(): UpdatePolicy? = withContext(Dispatchers.IO) {
        try {
            val currentVersionCode = getCurrentVersionCode()
            val currentVersionName = getCurrentVersionName()

            Log.d("UpdatePolicyRepo", "getActivePolicy: app=${context.packageName} currentVersionCode=$currentVersionCode currentVersionName=$currentVersionName")

            val appId = context.packageName
            val policies = try {
                client.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("is_active", true)
                            eq("app_id", appId)
                        }
                    }
                    .decodeList<UpdatePolicy>()
                    .sortedByDescending { it.targetVersionCode }
            } catch (e: Exception) {
                Log.e("UpdatePolicyRepo", "decodeList failed: ${e.message}", e)
                emptyList<UpdatePolicy>()
            }

            Log.d("UpdatePolicyRepo", "policies fetched count=${policies.size}")
            policies.forEach { p -> Log.d("UpdatePolicyRepo", "policy id=${p.id} target=${p.targetVersionCode} force=${p.isForceUpdate}") }

            val chosen = policies.firstOrNull { p ->
                try {
                    p.targetVersionCode > currentVersionCode.toLong()
                } catch (_: Exception) { false }
            }
            Log.d("UpdatePolicyRepo", "chosen policy=${chosen?.id}")
            chosen
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 사용자가 "나중에" 버튼을 누른 버전을 저장합니다.
     *
     * @param version 무시할 버전
     */
    fun dismissVersion(version: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_VERSION_CODE, version).apply()
    }

    /**
     * 무시된 버전을 가져옵니다.
     */
    private fun getDismissedVersion(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISMISSED_VERSION_CODE, null)
    }

    /**
     * 무시 기록을 초기화합니다. (디버그용)
     */
    fun clearDismissedVersion() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DISMISSED_VERSION_CODE).apply()
    }

    // UpdateStats helpers - stored per targetVersionCode
    fun getUpdateStats(versionCode: Long): kr.sweetapps.alcoholictimer.data.supabase.model.UpdateStats {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = versionCode.toString()
        val later = prefs.getInt(KEY_LATER_COUNT_PREFIX + key, 0)
        val lastMs = prefs.getLong(KEY_LAST_DISMISSED_PREFIX + key, 0L)
        return kr.sweetapps.alcoholictimer.data.supabase.model.UpdateStats(later, lastMs)
    }

    fun recordLaterClicked(versionCode: Long, nowMs: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = versionCode.toString()
        val keyCount = KEY_LATER_COUNT_PREFIX + key
        val prev = prefs.getInt(keyCount, 0)
        prefs.edit().putInt(keyCount, prev + 1).putLong(KEY_LAST_DISMISSED_PREFIX + key, nowMs).apply()
    }

    fun clearStats(versionCode: Long) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = versionCode.toString()
        prefs.edit().remove(KEY_LATER_COUNT_PREFIX + key).remove(KEY_LAST_DISMISSED_PREFIX + key).apply()
    }
}
