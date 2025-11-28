package kr.sweetapps.alcoholictimer.ads

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository

class AdPolicyRepositoryIntegrationTest {
    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun test_parsePolicyFromMockServer() = runBlocking {
        val body = """
            [
              {
                "id": 123,
                "app_id": "kr.sweetapps.alcoholictimer",
                "is_active": true,
                "ad_app_open_enabled": true,
                "ad_interstitial_enabled": true,
                "ad_banner_enabled": false,
                "app_open_max_per_hour": 5,
                "app_open_max_per_day": 50,
                "app_open_cooldown_seconds": 10,
                "ad_interstitial_max_per_hour": 2,
                "ad_interstitial_max_per_day": 10
              }
            ]
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        // Create a fetcher that ignores the requested URL and calls the mock server URL instead
        val fetcher = object : AdPolicyRepository.Fetcher {
            override fun get(url: String): Pair<Int, String?> {
                val url = server.url("/rest/v1/ad_policy?is_active=eq.true&select=").toString()
                try {
                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5_000
                    conn.readTimeout = 5_000
                    val code = conn.responseCode
                    val text = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else conn.errorStream?.bufferedReader()?.use { it.readText() }
                    return Pair(code, text)
                } catch (_: Throwable) {
                    return Pair(-1, null)
                }
            }
        }

        val repo = AdPolicyRepository("kr.sweetapps.alcoholictimer", fetcher)
        val policy = repo.getPolicy()
        assertNotNull(policy)
        assertEquals(123L, policy!!.id)
        assertEquals("kr.sweetapps.alcoholictimer", policy.appId)
        assertTrue(policy.isActive)
        assertTrue(policy.adInterstitialEnabled)
        assertEquals(2, policy.adInterstitialMaxPerHour)
        assertEquals(10, policy.adInterstitialMaxPerDay)
    }
}
