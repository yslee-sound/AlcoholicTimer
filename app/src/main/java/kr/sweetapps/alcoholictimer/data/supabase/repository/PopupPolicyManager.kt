package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple policy manager stub that decides whether to show an update dialog or notice.
 */
class PopupPolicyManager(
    private val emergencyRepo: EmergencyPolicyRepository,
    private val updateRepo: UpdatePolicyRepository,
    private val noticeRepo: NoticePolicyRepository,
    private val context: Context
) {
    /**
     * Suspend version: performs repo calls on IO dispatcher and returns a decision.
     */
    suspend fun decidePopup(osVersion: String): PopupDecision = withContext(Dispatchers.IO) {
        try {
            // Emergency policy has highest priority
            try {
                val em = try { emergencyRepo.getActivePolicy() } catch (e: Exception) { null }
                if (em != null && em.isActive) {
                    android.util.Log.d("PopupPolicyManager", "Deciding to show emergency dialog id=${em.id}")
                    return@withContext PopupDecision.ShowEmergency(em)
                }
            } catch (e: Exception) { e.printStackTrace() }

            val active = updateRepo.getActivePolicy()
            if (active != null) {
                // get current version code
                val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    (pkgInfo.longVersionCode).toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode
                }

                // Show update if targetVersionCode is greater OR equal (allow server-controlled active flag)
                android.util.Log.d("PopupPolicyManager", "active policy target=${active.targetVersionCode} current=$currentVersionCode isForce=${active.isForceUpdate}")
                if (active.targetVersionCode >= currentVersionCode) {
                    android.util.Log.d("PopupPolicyManager", "Deciding to show update dialog")
                    return@withContext PopupDecision.ShowUpdate(active)
                } else {
                    android.util.Log.d("PopupPolicyManager", "Policy target lower than current; not showing")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check notice policy (latest active announcement) and decide to show if not seen
        try {
            val announcement = try { noticeRepo.getLatestActiveAnnouncement() } catch (e: Exception) { null }
            if (announcement != null && announcement.isActive) {
                val prefs = context.getSharedPreferences("popup_prefs", Context.MODE_PRIVATE)
                val key = "last_notice_version_${context.packageName}"
                val lastSeen = prefs.getInt(key, -1)
                val currentVersion = announcement.noticeVersion
                android.util.Log.d("PopupPolicyManager", "Found announcement id=${announcement.id} version=$currentVersion lastSeen=$lastSeen")
                // show only if announcement version is strictly newer than last seen
                if (currentVersion > lastSeen) {
                    return@withContext PopupDecision.ShowNotice(announcement)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext PopupDecision.None
    }

    fun dismissUpdate(targetVersion: Int) {
        // store dismissal locally if needed (stub)
    }
}
