// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.androidx.navigation.safeargs) apply false // [NEW] Navigation Safe Args 플러그인 추가
    id("com.google.gms.google-services") version "4.4.4" apply false
    // [FIX] Crashlytics 플러그인 버전 다운그레이드 (3.0.2 → 2.9.9)
    // 이유: 3.0.2는 KSP와 순환 의존성 발생, 2.9.9는 안정적으로 작동
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
}

// Gradle 구성 캐시 정리 (AGP/Gradle 업데이트 후 캐시 충돌 시 사용)
tasks.register("purgeConfigCache") {
    group = "help"
    description = "Deletes .gradle/configuration-cache in the root project"
    doLast {
        val dir = file(".gradle/configuration-cache")
        if (dir.exists()) {
            if (dir.deleteRecursively()) {
                println("[purgeConfigCache] Deleted: ${dir}")
            } else {
                println("[purgeConfigCache] Failed to delete: ${dir}. Try closing IDE/daemons and re-run.")
            }
        } else {
            println("[purgeConfigCache] Nothing to delete. ${dir} not found.")
        }
    }
}
