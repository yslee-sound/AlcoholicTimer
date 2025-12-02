// [NEW] 클린 아키텍처 리팩토링: AdVerifier를 data/source/remote로 이동
package kr.sweetapps.alcoholictimer.data.source.remote

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Stub AdVerifier: preserves API but avoids running complex checks when ads removed.
 */
object AdVerifier {
    private const val TAG = "AdVerifier"
    data class Result(val name: String, val ok: Boolean, val message: String)
    fun runChecks(activity: Activity, callback: (List<Result>) -> Unit) {
        val results = listOf(
            Result("PolicyFetchCompleted", true, "stubbed"),
            Result("InterstitialEnabled", false, "stubbed"),
            Result("AppOpenEnabled", false, "stubbed")
        )
        for (r in results) Log.d(TAG, "[${r.name}] ${r.ok} - ${r.message}")
        callback(results)
    }
}

