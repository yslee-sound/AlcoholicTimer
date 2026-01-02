import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services") // Google Services
    // [FIX] KSP를 먼저 선언하여 태스크 의존성 순서 제어
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" // Room Database용 KSP 플러그인
    id("androidx.room") version "2.6.1" // [NEW] Room Gradle 플러그인 (2025-12-25)
    alias(libs.plugins.firebase.crashlytics) // Firebase Crashlytics (카탈로그에서 버전 관리)
    id("com.google.firebase.firebase-perf")
}

// 중복 commonmark(com.atlassian.commonmark)으로 인한 Duplicate class 에러 방지
configurations.all {
    exclude(group = "com.atlassian.commonmark", module = "commonmark")
}

// local.properties 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// Helper: sanitize property values (remove surrounding quotes and whitespace)
fun String?.sanitizeProp(): String? = this?.trim()?.trim('"')?.trim('\'')?.trim()

// [NEW] Helper: AdMob 키를 빌드 타입에 맞게 가져오는 함수
fun getAdMobKey(keyName: String, buildTypeSuffix: String): String {
    val key = "${keyName}_${buildTypeSuffix.uppercase()}"
    val value = localProperties.getProperty(key)?.sanitizeProp()

    if (value.isNullOrBlank()) {
        println("⚠️ Warning: $key not found in local.properties, using empty string")
        return ""
    }

    return value
}

// UMP 테스트 기기 해시 (local.properties에서 읽어 Debug 빌드에 주입)
val umpTestDeviceHash = localProperties.getProperty("UMP_TEST_DEVICE_HASH")?.sanitizeProp() ?: ""

// [NEW] AdMob 테스트 기기 ID (local.properties에서 읽어 Debug 빌드에 주입)
val adMobTestDeviceId = localProperties.getProperty("ADMOB_TEST_DEVICE_ID")?.sanitizeProp() ?: ""

// release 관련 태스크 실행 여부 (configuration 시점에 1회 계산)
// bundleRelease / assembleRelease / publishRelease / 끝이 Release 인 태스크 포함
val isReleaseTaskRequested: Boolean = gradle.startParameter.taskNames.any { name ->
    val lower = name.lowercase()
    ("release" in lower && ("assemble" in lower || "bundle" in lower || "publish" in lower)) || lower.endsWith("release")
}

// 안전: 릴리즈 관련 태스크가 요청된 경우(릴리즈 빌드 파이프라인 등) 디버그 전용 해시를 빈값으로 강제합니다.
// 이렇게 하면 실수로 릴리즈 빌드에 로컬 디버그 해시가 포함되는 것을 방지합니다.
val debugUmpTestDeviceHash = if (isReleaseTaskRequested) "" else umpTestDeviceHash
val debugAdMobTestDeviceId = if (isReleaseTaskRequested) "" else adMobTestDeviceId

android {
    namespace = "kr.sweetapps.alcoholictimer"
    compileSdk = 36


    // 버전 코드 전략: yyyymmdd + 2자리 시퀀스 (NN)
    // 이전 사용: 2025100800 -> 신규: 2025100801
    val releaseVersionCode = 2025123100
    val releaseVersionName = "1.1.8"
    defaultConfig {
        applicationId = "kr.sweetapps.alcoholictimer"
        minSdk = 21
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        ndk {
            // Play Console 경고 대응: 네이티브 심볼 업로드용 심볼 테이블 생성 (FULL 은 용량↑)
            debugSymbolLevel = "SYMBOL_TABLE"
        }

        // Supabase 설정 (local.properties → 환경변수 → 기본값 순)
        val supabaseUrl = (localProperties.getProperty("supabase.url")?.sanitizeProp()
            ?: System.getenv("SUPABASE_URL")?.trim()?.trim('"')?.trim('\'')
            ?: "https://your-project.supabase.co")
        val supabaseKey = (localProperties.getProperty("supabase.key")?.sanitizeProp()
            ?: System.getenv("SUPABASE_KEY")?.trim()?.trim('"')?.trim('\'')
            ?: "your-anon-key")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")

        // [NEW] 테스트 기기 설정 (Debug에서만 값 주입, Release는 빈 문자열)
        buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"$debugUmpTestDeviceHash\"")
        buildConfigField("String", "ADMOB_TEST_DEVICE_ID", "\"$debugAdMobTestDeviceId\"")
    }

    signingConfigs {
        // [UPDATED] local.properties 기반 Release 서명 설정
        create("release") {
            // local.properties에서 키스토어 정보 읽기
            val keystorePath = localProperties.getProperty("STORE_FILE")?.sanitizeProp()
            val storePass = localProperties.getProperty("STORE_PASSWORD")?.sanitizeProp()
            val alias = localProperties.getProperty("KEY_ALIAS")?.sanitizeProp()
            val keyPass = localProperties.getProperty("KEY_PASSWORD")?.sanitizeProp()

            // 키스토어 파일 설정
            if (!keystorePath.isNullOrBlank()) {
                try {
                    storeFile = file(keystorePath)
                    storePassword = storePass ?: ""
                    keyAlias = alias ?: ""
                    keyPassword = keyPass ?: ""

                    println("[INFO] ✅ Release 서명 설정 완료: $keystorePath")
                } catch (e: Exception) {
                    println("[WARN] ⚠️ 키스토어 파일을 찾을 수 없습니다: $keystorePath")
                    println("[WARN] Release 빌드는 서명되지 않은 상태로 생성됩니다.")
                }
            } else {
                println("[WARN] ⚠️ local.properties에 STORE_FILE이 설정되지 않았습니다.")
                println("[WARN] Release 빌드는 서명되지 않은 상태로 생성됩니다.")
                println("[INFO] local.properties에 다음 항목을 추가하세요:")
                println("       STORE_FILE=path/to/your/keystore.jks")
                println("       STORE_PASSWORD=your_store_password")
                println("       KEY_ALIAS=your_key_alias")
                println("       KEY_PASSWORD=your_key_password")
            }
        }
    }


    buildTypes {
        release {
            // 릴리스 번들 최적화: 코드/리소스 축소 (ProGuard/R8)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // [UPDATED] 서명 설정: local.properties 기반
            val keystorePath = localProperties.getProperty("STORE_FILE")?.sanitizeProp()
            val hasKeystore = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

            if (isReleaseTaskRequested) {
                if (!hasKeystore) {
                    throw GradleException("""
                        ❌ Release 빌드를 위한 서명 설정이 필요합니다!
                        
                        local.properties 파일에 다음 항목을 추가하세요:
                        ─────────────────────────────────────────
                        STORE_FILE=path/to/your/keystore.jks
                        STORE_PASSWORD=your_store_password
                        KEY_ALIAS=your_key_alias
                        KEY_PASSWORD=your_key_password
                        ─────────────────────────────────────────
                    """.trimIndent())
                }
                signingConfig = signingConfigs.getByName("release")
                println("[INFO] ✅ Release 빌드에 서명 적용: $keystorePath")
            } else if (hasKeystore) {
                // Release 빌드 아니더라도 키스토어가 있으면 적용
                signingConfig = signingConfigs.getByName("release")
            }
            // [NEW] Crashlytics 자동 활성화 (Release 빌드)
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"

            // [NEW] 테스트 기기 설정 오버라이드 (Release에서는 빈 문자열)
            buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"\"")
            buildConfigField("String", "ADMOB_TEST_DEVICE_ID", "\"\"")

            // [UPDATED] local.properties에서 AdMob 키 읽기 (Release)
            val adMobAppId = getAdMobKey("ADMOB_APP_ID", "RELEASE")
            val adMobInterstitialId = getAdMobKey("ADMOB_INTERSTITIAL_ID", "RELEASE")
            val adMobOpenId = getAdMobKey("ADMOB_OPEN_ID", "RELEASE")
            // [NEW] Native Ad Unit ID (Release)
            val adMobNativeId = getAdMobKey("ADMOB_NATIVE_ID", "RELEASE")

            // Manifest용 (App ID)
            manifestPlaceholders["ADMOB_APP_ID"] = adMobAppId

            // Kotlin 코드용 (BuildConfig)
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"$adMobInterstitialId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$adMobNativeId\"")
            buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"$adMobOpenId\"")

            // [DEPRECATED] 배너 광고는 제거되었지만 호환성을 위해 빈 문자열 유지
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"\"")
        }
        // debug 빌드 타입: 테스트용 광고 ID + .debug suffix
        getByName("debug") {
            applicationIdSuffix = ".debug"  // kr.sweetapps.alcoholictimer.debug
            versionNameSuffix = "-debug"
            // [UPDATED] Crashlytics 활성화 (5회 탭 테스트 기능을 위해)
            // 참고: Debug 빌드도 Firebase Dev 프로젝트로 데이터를 전송하여 테스트 가능
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"

            // [UPDATED] local.properties에서 AdMob 키 읽기 (Debug)
            val adMobAppId = getAdMobKey("ADMOB_APP_ID", "DEBUG")
            val adMobInterstitialId = getAdMobKey("ADMOB_INTERSTITIAL_ID", "DEBUG")
            val adMobOpenId = getAdMobKey("ADMOB_OPEN_ID", "DEBUG")
            // [NEW] Native Ad Unit ID (Debug)
            val adMobNativeId = getAdMobKey("ADMOB_NATIVE_ID", "DEBUG")

            // Manifest용 (App ID)
            manifestPlaceholders["ADMOB_APP_ID"] = adMobAppId

            // Kotlin 코드용 (BuildConfig)
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"$adMobInterstitialId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$adMobNativeId\"")
            buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"$adMobOpenId\"")

            // [DEPRECATED] 배너 광고는 제거되었지만 호환성을 위해 빈 문자열 유지
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"\"")

            // [NOTE] 테스트 기기 설정은 defaultConfig에서 이미 주입됨
            // UMP_TEST_DEVICE_HASH와 ADMOB_TEST_DEVICE_ID는 자동으로 Debug 값 사용
        }
    }

    // Java/Kotlin 타깃 유지
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        // viewBinding 등 미사용
    }

    lint {
        // 릴리스 치명적 이슈 CI fail fast
        abortOnError = true
        warningsAsErrors = false // 초기 온보딩: 경고는 유지, 필요시 true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}

// [NEW] Room Database 스키마 export 경로 설정 (2025-12-25)
// Room Gradle 플러그인을 사용한 권장 방식
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    // [NEW] Material Components for Snackbar (인앱 업데이트용, 2026-01-02)
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    // Accompanist System UI Controller: control status/navigation bar colors from Compose
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.app.update.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    // [NEW] WorkManager for retention notifications (2025-12-31)
    implementation(libs.androidx.work.runtime.ktx)

    // Markwon: 완전한 Markdown 렌더링(이미지/테이블/확장 지원)
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")

    // Firebase (BOM으로 버전 통합 관리)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // [UPDATED] 최신 안정 버전
    implementation("com.google.firebase:firebase-analytics-ktx")
    // [NEW] Crashlytics: Gradle로 자동 제어 (Debug=비활성화, Release=활성화)
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx") // [NEW] Firestore 추가
    implementation("com.google.firebase:firebase-storage-ktx") // [NEW] Firebase Storage 추가 (2025-12-19)
    // 👇 [NEW] Remote Config 라이브러리 추가
    implementation("com.google.firebase:firebase-config-ktx")

    // [NEW] Coil: 이미지 로딩 라이브러리 (2025-12-19)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // AdMob & UMP (명시 버전 사용)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    // UMP (User Messaging Platform) SDK for GDPR consent
    implementation("com.google.android.ump:user-messaging-platform:2.1.0")

    // AndroidX Preference for reading TCF strings directly
    implementation("androidx.preference:preference-ktx:1.2.1")

    // ConstraintLayout for native ad layout (카탈로그 참조)
    implementation(libs.androidx.constraintlayout)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)

    // [NEW] Room Database (KSP 방식 - Kotlin 2.0 호환)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation(libs.junit)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.robolectric)
    testImplementation(libs.org.json)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// signingReport 대안: 서명 환경변수 및 키스토어 존재 여부를 출력하는 헬퍼 태스크
// 구성 캐시 문제로 signingReport 가 실패할 때 빠르게 상태를 확인하는 용도
tasks.register("printReleaseSigningEnv") {
    group = "help"
    description = "Prints release signing env vars and keystore file existence"

    // Diagnostic helper: not compatible with configuration cache
    notCompatibleWithConfigurationCache("Diagnostic task referencing environment and filesystem")

    // Capture environment and file info at configuration time
    val ksPathCfg: String? = System.getenv("KEYSTORE_PATH")
    val aliasCfg: String? = System.getenv("KEY_ALIAS")
    val hasStorePwCfg: Boolean = !System.getenv("KEYSTORE_STORE_PW").isNullOrEmpty()
    val hasKeyPwCfg: Boolean = !System.getenv("KEY_PASSWORD").isNullOrEmpty()
    val ksFileCfg = ksPathCfg?.let { file(it) }
    val ksExistsCfg = ksFileCfg?.exists() ?: false
    val ksSizeCfg = if (ksExistsCfg) ksFileCfg!!.length() else 0L

    doLast {
        println("KEYSTORE_PATH=${ksPathCfg ?: "<not set>"}")
        if (!ksPathCfg.isNullOrBlank()) {
            println(" - exists=${ksExistsCfg} size=${ksSizeCfg}")
        }
        println("KEY_ALIAS=${aliasCfg ?: "<not set>"}")
        println("KEYSTORE_STORE_PW set=${hasStorePwCfg}")
        println("KEY_PASSWORD set=${hasKeyPwCfg}")
    }
}

// 릴리즈 빌드 전 광고 설정 검증 태스크
tasks.register("verifyReleaseAdConfig") {
    group = "verification"
    description = "릴리즈 빌드 전에 광고 설정이 올바른지 검증합니다"

    // Diagnostic: file system checks; mark incompatible with configuration cache
    notCompatibleWithConfigurationCache("Performs file system checks and reads project files")

    // Capture project dir at configuration time
    val projectDirCfg = project.projectDir

    doLast {
        val projectDir = projectDirCfg

        println("\n" + "=".repeat(80))
        println("🔍 릴리즈 빌드 광고 설정 검증 중...")
        println("=".repeat(80))

        var hasError = false
        val warnings = mutableListOf<String>()
        val checks = mutableListOf<String>()

        // 1. DebugAdHelper.kt 파일 검증
        val debugAdHelperFile = File(projectDir, "src/main/java/kr/sweetapps/alcoholictimer/ui/common/DebugAdHelper.kt")
        if (debugAdHelperFile.exists()) {
            val content = debugAdHelperFile.readText()
            if (!content.contains("BuildConfig.DEBUG")) {
                hasError = true
                println("❌ ERROR: DebugAdHelper.kt에 BuildConfig.DEBUG 체크가 없습니다!")
            } else {
                checks.add("✓ DebugAdHelper.kt에 BuildConfig.DEBUG 체크 존재")
            }
        } else {
            warnings.add("⚠️  WARNING: DebugAdHelper.kt 파일을 찾을 수 없습니다")
        }

        // 2. BaseActivity.kt 검증
        val baseActivityFile = File(projectDir, "src/main/java/kr/sweetapps/alcoholictimer/ui/common/BaseActivity.kt")
        if (baseActivityFile.exists()) {
            val content = baseActivityFile.readText()
            val hasBuildConfigCheck = content.contains("if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG)") ||
                                     content.contains("if (BuildConfig.DEBUG)")
            if (!hasBuildConfigCheck) {
                hasError = true
                println("❌ ERROR: BaseActivity.kt의 shouldHideBanner 로직에 BuildConfig.DEBUG 체크가 없습니다!")
            } else {
                checks.add("✓ BaseActivity.kt에 BuildConfig.DEBUG 체크 존재")
            }
        } else {
            hasError = true
            println("❌ ERROR: BaseActivity.kt 파일을 찾을 수 없습니다")
        }

        // 3. StandardScreen.kt 검증
        val standardScreenFile = File(projectDir, "src/main/java/kr/sweetapps/alcoholictimer/ui/tab_01/components/StandardScreen.kt")
        if (standardScreenFile.exists()) {
            val content = standardScreenFile.readText()
            val hasBuildConfigCheck = content.contains("if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG)") ||
                                     content.contains("if (BuildConfig.DEBUG)")
            if (!hasBuildConfigCheck) {
                hasError = true
                println("❌ ERROR: StandardScreen.kt의 shouldHideBanner 로직에 BuildConfig.DEBUG 체크가 없습니다!")
            } else {
                checks.add("✓ StandardScreen.kt에 BuildConfig.DEBUG 체크 존재")
            }
        }

        // 4. DetailActivity.kt 검증
        val detailActivityFile = File(projectDir, "src/main/java/kr/sweetapps/alcoholictimer/feature/detail/DetailActivity.kt")
        if (detailActivityFile.exists()) {
            val content = detailActivityFile.readText()
            val hasBuildConfigCheck = content.contains("if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG)") ||
                                     content.contains("if (BuildConfig.DEBUG)")
            if (!hasBuildConfigCheck) {
                hasError = true
                println("❌ ERROR: DetailActivity.kt의 shouldHideBanner 로직에 BuildConfig.DEBUG 체크가 없습니다!")
            } else {
                checks.add("✓ DetailActivity.kt에 BuildConfig.DEBUG 체크 존재")
            }
        }

        // 5. 광고 유닛 ID 검증 (릴리즈 빌드 설정 확인)
        val buildFile = File(projectDir, "build.gradle.kts")
        val buildContent = if (buildFile.exists()) buildFile.readText() else ""

        if (buildContent.contains("ca-app-pub-8420908105703273/3187272865")) {
            checks.add("✓ 릴리즈 BANNER 광고 유닛 ID 설정됨")
        } else {
            warnings.add("⚠️  WARNING: 릴리즈 BANNER 광고 유닛 ID가 올바르지 않을 수 있습니다")
        }

        if (buildContent.contains("ca-app-pub-8420908105703273/2270912481")) {
            checks.add("✓ 릴리즈 INTERSTITIAL 광고 유닛 ID 설정됨")
        } else {
            warnings.add("⚠️  WARNING: 릴리즈 INTERSTITIAL 광고 유닛 ID가 올바르지 않을 수 있습니다")
        }

        // 결과 출력
        println("\n✅ 통과한 검증:")
        checks.forEach { println("  $it") }

        if (warnings.isNotEmpty()) {
            println("\n⚠️  경고:")
            warnings.forEach { println("  $it") }
        }

        println("\n" + "=".repeat(80))

        if (hasError) {
            println("❌ 검증 실패! 릴리즈 빌드를 중단합니다.")
            println("=".repeat(80) + "\n")
            throw GradleException(
                """
                |릴리즈 빌드 광고 설정 검증 실패!
                |
                |DebugAdHelper가 릴리즈 빌드에서도 광고를 숨길 수 있는 상태입니다.
                |다음 파일들을 확인하고 BuildConfig.DEBUG 체크를 추가하세요:
                |  - BaseActivity.kt
                |  - StandardScreen.kt
                |  - DetailActivity.kt
                |
                |각 파일에서 shouldHideBanner 로직이 다음과 같이 구현되어야 합니다:
                |  if (BuildConfig.DEBUG) { ... } else false
                """.trimMargin()
            )
        } else {
            println("✅ 모든 검증 통과! 릴리즈 빌드를 계속 진행합니다.")
            println("=".repeat(80) + "\n")
        }
    }
}

// 모든 릴리즈 관련 태스크가 verifyReleaseAdConfig에 의존하도록 설정
tasks.configureEach {
    if (name.contains("Release", ignoreCase = true) &&
        (name.contains("assemble", ignoreCase = true) ||
         name.contains("bundle", ignoreCase = true))) {
        dependsOn("verifyReleaseAdConfig")
    }
}
