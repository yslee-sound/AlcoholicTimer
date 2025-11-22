package kr.sweetapps.alcoholictimer.ads

import kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository

/**
 * Simple test Fetcher that returns a predefined HTTP status and body.
 */
class MockPolicyRepository(private val code: Int = 200, private val body: String? = null) : AdPolicyRepository.Fetcher {
    override fun get(url: String): Pair<Int, String?> = Pair(code, body)
}

