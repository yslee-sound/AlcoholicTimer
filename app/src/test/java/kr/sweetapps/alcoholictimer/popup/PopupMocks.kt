// Mocks for popup policy unit tests
package kr.sweetapps.alcoholictimer.popup

class MockPolicyRepository {
    private var emergency: EmergencyPolicyConfig? = null
    private var notice: NoticePolicyConfig? = null
    private var update: UpdatePolicyConfig? = null
    private var shouldFail: Boolean = false

    fun setMockEmergency(c: EmergencyPolicyConfig?) { emergency = c }
    fun setMockNotice(n: NoticePolicyConfig?) { notice = n }
    fun setMockUpdate(u: UpdatePolicyConfig?) { update = u }
    fun setFail(flag: Boolean) { shouldFail = flag }

    fun getEmergency(): EmergencyPolicyConfig? {
        if (shouldFail) throw RuntimeException("mock failure")
        return emergency
    }

    fun getNotice(): NoticePolicyConfig? {
        if (shouldFail) throw RuntimeException("mock failure")
        return notice
    }

    fun getUpdate(): UpdatePolicyConfig? {
        if (shouldFail) throw RuntimeException("mock failure")
        return update
    }
}

class MockSharedPreferences {
    var laterCount: Int = 0
    var lastLaterTimeMillis: Long = 0L
    var lastSeenNoticeVersion: Int = -1

    fun clear() {
        laterCount = 0
        lastLaterTimeMillis = 0L
        lastSeenNoticeVersion = -1
    }
}

class MockSystemInfo(var currentVersionCode: Int)

class MockTimeProvider(var nowMillis: Long) {
    fun advanceMillis(ms: Long) { nowMillis += ms }
}

