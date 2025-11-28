// PopupManager used for unit tests
package kr.sweetapps.alcoholictimer.popup

import kr.sweetapps.alcoholictimer.ads.AdController

class PopupManager(
    private val repo: MockPolicyRepository,
    private val prefs: MockSharedPreferences,
    private val sysInfo: MockSystemInfo,
    private val timeProvider: MockTimeProvider
) {

    fun getPopupToShow(): PopupResult {
        // Emergency has highest priority
        try {
            val em = repo.getEmergency()
            if (em != null && em.is_active) {
                // signal ad system that a full-screen popup is showing
                try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                return PopupResult.Emergency(em)
            }
        } catch (e: Exception) {
            // on data failure, do not crash; ensure no popup flag
            try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
            return PopupResult.None
        }

        // Update policy
        try {
            val up = repo.getUpdate()
            if (up != null && up.is_active) {
                val current = sysInfo.currentVersionCode
                // only consider update if target_version_code > current OR force update
                if (up.target_version_code > current || up.is_force_update) {
                    // signal full-screen for update dialogs
                    try { AdController.setFullScreenAdShowing(true) } catch (_: Throwable) {}
                    if (up.is_force_update) {
                        return PopupResult.Update(up)
                    }

                    // not force: check later count and reshow interval
                    if (prefs.laterCount >= up.max_later_count) {
                        return PopupResult.Update(up)
                    }

                    val last = prefs.lastLaterTimeMillis
                    val intervalMillis = up.reshow_interval_hours * 3600_000L
                    if (last <= 0L || (timeProvider.nowMillis - last) >= intervalMillis) {
                        return PopupResult.Update(up)
                    }
                    // otherwise, do not show update now
                }
            }
        } catch (e: Exception) {
            try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
            return PopupResult.None
        }

        // Notice policy
        try {
            val notice = repo.getNotice()
            if (notice != null && notice.is_active) {
                // show only if policy version is strictly greater than last seen
                if (notice.notice_version > prefs.lastSeenNoticeVersion) {
                    // treat notice as non-fullscreen in tests by default; do not set full-screen flag
                    return PopupResult.Notice(notice)
                }
            }
        } catch (e: Exception) {
            try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
            return PopupResult.None
        }

        // No popup to show: ensure ad flag cleared
        try { AdController.notifyFullScreenDismissed() } catch (_: Throwable) {}
        return PopupResult.None
    }
}
