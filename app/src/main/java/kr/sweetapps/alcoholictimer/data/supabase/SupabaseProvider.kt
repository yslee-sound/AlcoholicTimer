package kr.sweetapps.alcoholictimer.data.supabase

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kr.sweetapps.alcoholictimer.BuildConfig

/**
 * Supabase 클라이언트 싱글톤
 *
 * 앱 전체에서 사용할 Supabase 클라이언트를 제공합니다.
 */
object SupabaseProvider {
    private var client: SupabaseClient? = null

    /**
     * Supabase 클라이언트를 가져옵니다.
     */
    fun getClient(context: Context): SupabaseClient {
        return client ?: synchronized(this) {
            client ?: createClient(context).also { client = it }
        }
    }

    /**
     * Supabase 클라이언트를 생성합니다.
     */
    private fun createClient(context: Context): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
        }
    }

    /**
     * 클라이언트를 재설정합니다. (테스트용)
     */
    fun reset() {
        client = null
    }
}

