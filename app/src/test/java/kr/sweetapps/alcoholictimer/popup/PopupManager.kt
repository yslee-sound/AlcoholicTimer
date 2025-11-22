// PopupManager used for unit tests
package kr.sweetapps.alcoholictimer.popup

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
                return PopupResult.Emergency(em)
            }
        } catch (e: Exception) {
            // on data failure, do not crash; return None
            return PopupResult.None
        }

        // Update policy
        try {
            val up = repo.getUpdate()
            if (up != null && up.is_active) {
                val current = sysInfo.currentVersionCode
                // only consider update if target_version_code > current OR force update
                if (up.target_version_code > current || up.is_force_update) {
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
            return PopupResult.None
        }

        // Notice policy
        try {
            val notice = repo.getNotice()
            if (notice != null && notice.is_active) {
                if (prefs.lastSeenNoticeVersion != notice.notice_version) {
                    return PopupResult.Notice(notice)
                }
            }
        } catch (e: Exception) {
            return PopupResult.None
        }

        return PopupResult.None
    }
}

