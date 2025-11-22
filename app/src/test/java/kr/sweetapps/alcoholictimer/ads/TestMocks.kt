package kr.sweetapps.alcoholictimer.ads

import kr.sweetapps.alcoholictimer.data.supabase.repository.AdPolicyRepository
import java.net.HttpURLConnection
import java.net.URL

// Simple Fetcher implementation for tests that performs GET on provided URL
class SimpleHttpFetcher : AdPolicyRepository.Fetcher {
    override fun get(url: String): Pair<Int, String?> {
        return try {
            val u = URL(url)
            val conn = (u.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
            }
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else conn.errorStream?.bufferedReader()?.use { it.readText() }
            Pair(code, body)
        } catch (t: Throwable) {
            Pair(-1, null)
        }
    }
}

// Fetcher that delegates to a provided function (useful to call MockWebServer explicitly)
class DelegatingFetcher(private val f: (String) -> Pair<Int, String?>) : AdPolicyRepository.Fetcher {
    override fun get(url: String): Pair<Int, String?> = f(url)
}

