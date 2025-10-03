import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Delete

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.alcoholictimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.alcoholictimer"
        minSdk = 21
        targetSdk = 36
        versionCode = 3
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java {
                // Legacy stub logical exclusion (물리 삭제 전 임시 조치)
                exclude("com/example/alcoholictimer/BaseActivity.kt")
                exclude("com/example/alcoholictimer/LevelActivity.kt")
                exclude("com/example/alcoholictimer/LevelDefinitions.kt")
                exclude("com/example/alcoholictimer/NicknameEditActivity.kt")
                exclude("com/example/alcoholictimer/QuitActivity.kt")
                exclude("com/example/alcoholictimer/RunActivity.kt")
                exclude("com/example/alcoholictimer/SettingsActivity.kt")
                exclude("com/example/alcoholictimer/StartActivity.kt")
                exclude("com/example/alcoholictimer/components/**")
                exclude("com/example/alcoholictimer/ui/**")
                exclude("com/example/alcoholictimer/utils/**")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)

    // SplashScreen API
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// --- Legacy stub removal task (manual run) ---
// 안전 확인 후 물리적으로 남아있는 루트 스텁/legacy 디렉터리를 제거하기 위한 태스크.
// 실행 방법: gradlew.bat :app:removeLegacyStubs
// 필요 시 1회 실행 후 태스크 삭제 가능.
tasks.register<Delete>("removeLegacyStubs") {
    val legacyRoot = "src/main/java/com/example/alcoholictimer"
    delete(
        "$legacyRoot/BaseActivity.kt",
        "$legacyRoot/LevelActivity.kt",
        "$legacyRoot/LevelDefinitions.kt",
        "$legacyRoot/NicknameEditActivity.kt",
        "$legacyRoot/QuitActivity.kt",
        "$legacyRoot/RunActivity.kt",
        "$legacyRoot/SettingsActivity.kt",
        "$legacyRoot/StartActivity.kt",
        "$legacyRoot/components",
        "$legacyRoot/ui",
        "$legacyRoot/utils"
    )
    doLast { println("[removeLegacyStubs] Legacy stub files/directories deleted (if existed).") }
}
