import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// local.properties 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// release 관련 태스크 실행 여부 (configuration 시점에 1회 계산)
// bundleRelease / assembleRelease / publishRelease / 끝이 Release 인 태스크 포함
val isReleaseTaskRequested: Boolean = gradle.startParameter.taskNames.any { name ->
    val lower = name.lowercase()
    ("release" in lower && ("assemble" in lower || "bundle" in lower || "publish" in lower)) || lower.endsWith("release")
}

android {
    namespace = "kr.sweetapps.alcoholictimer"
    compileSdk = 36

    // 버전 코드 전략: yyyymmdd + 2자리 시퀀스 (NN)
    // 이전 사용: 2025100800 -> 신규: 2025100801
    val releaseVersionCode = 2025102803
    val releaseVersionName = "1.1.3"
    defaultConfig {
        applicationId = "kr.sweetapps.alcoholictimer" // Play Console용 applicationId는 유지
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
        val supabaseUrl = localProperties.getProperty("supabase.url")
            ?: System.getenv("SUPABASE_URL")
            ?: "https://your-project.supabase.co"
        val supabaseKey = localProperties.getProperty("supabase.key")
            ?: System.getenv("SUPABASE_KEY")
            ?: "your-anon-key"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    signingConfigs {
        // 환경변수 기반 release 서명 (키 미설정 시 경고만 출력 -> 로컬 debug 빌드 영향 X)
        create("release") {
            val ksPath = System.getenv("KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank()) {
                storeFile = file(ksPath)
            } else {
                println("[WARN] Release keystore not configured - will build unsigned bundle. Set KEYSTORE_PATH before production release.")
            }
            storePassword = System.getenv("KEYSTORE_STORE_PW") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
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
            // 서명 강제: 실제 release 관련 태스크(assembleRelease/bundleRelease 등) 요청 시에만 검사
            val hasKeystore = !System.getenv("KEYSTORE_PATH").isNullOrBlank()
            if (isReleaseTaskRequested && !hasKeystore) {
                throw GradleException("Unsigned release build blocked. Set KEYSTORE_PATH, KEYSTORE_STORE_PW, KEY_ALIAS, KEY_PASSWORD env vars before running a release build.")
            }
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            // 빌드타입별 배너 광고 유닛ID (릴리즈 실제 ID)
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-8420908105703273/3187272865\"")
            // 빌드타입별 전면 광고 유닛ID (릴리즈 실제 ID)
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"ca-app-pub-8420908105703273/2270912481\"")
            // 네이티브 광고는 사용하지 않으므로 빈 문자열 (폐기됨)
            buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"\"")
        }
        // debug 설정 변경 없음
        getByName("debug") {
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.app.update.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    // AdMob & UMP (명시 버전 사용) -> 버전 카탈로그 참조로 변경
    implementation(libs.google.ads)
    implementation(libs.ump)

    // ConstraintLayout for native ad layout (카탈로그 참조)
    implementation(libs.androidx.constraintlayout)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.robolectric)
    testImplementation(libs.org.json)

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
    doLast {
        val ksPath = System.getenv("KEYSTORE_PATH")
        val alias = System.getenv("KEY_ALIAS")
        val hasStorePw = !System.getenv("KEYSTORE_STORE_PW").isNullOrEmpty()
        val hasKeyPw = !System.getenv("KEY_PASSWORD").isNullOrEmpty()
        println("KEYSTORE_PATH=${ksPath ?: "<not set>"}")
        if (!ksPath.isNullOrBlank()) {
            val f = file(ksPath)
            println(" - exists=${f.exists()} size=${if (f.exists()) f.length() else 0}")
        }
        println("KEY_ALIAS=${alias ?: "<not set>"}")
        println("KEYSTORE_STORE_PW set=${hasStorePw}")
        println("KEY_PASSWORD set=${hasKeyPw}")
    }
}

// (단순화) designTokenCheck 커스텀 태스크 제거.
// 필요 시 별도 스크립트나 독립 Gradle 플러그인/CI 스텝으로 수행 권장

// 릴리즈 빌드 전 광고 설정 검증 태스크
tasks.register("verifyReleaseAdConfig") {
    group = "verification"
    description = "릴리즈 빌드 전에 광고 설정이 올바른지 검증합니다"

    // Configuration cache 호환성
    notCompatibleWithConfigurationCache("Uses project state directly")

    doLast {
        println("\n" + "=".repeat(80))
        println("🔍 릴리즈 빌드 광고 설정 검증 중...")
        println("=".repeat(80))

        var hasError = false
        val warnings = mutableListOf<String>()
        val checks = mutableListOf<String>()

        val projectDir = project.projectDir

        // 1. DebugAdHelper.kt 파일 검증
        val debugAdHelperFile = File(projectDir, "src/main/java/kr/sweetapps/alcoholictimer/core/ui/DebugAdHelper.kt")
        if (debugAdHelperFile.exists()) {
            val content = debugAdHelperFile.readText()

            // BuildConfig.DEBUG 체크가 있는지 확인
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
        val baseActivityFile = File(projectDir, "src/main/java/kr/sweetapps/alcoholictimer/core/ui/BaseActivity.kt")
        if (baseActivityFile.exists()) {
            val content = baseActivityFile.readText()

            // BuildConfig.DEBUG 체크가 있는지 확인
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
        val standardScreenFile = File(projectDir, "src/main/java/kr/sweetapps/alcoholictimer/core/ui/StandardScreen.kt")
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
        val buildContent = buildFile.readText()

        // 릴리즈 빌드에 실제 광고 ID가 설정되어 있는지 확인
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
