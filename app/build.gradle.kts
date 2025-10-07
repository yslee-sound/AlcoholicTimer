import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.alcoholictimer" // 코드 패키지 구조는 유지 (선택)
    compileSdk = 36

    // 버전 코드 전략: yyyymmdd + 2자리 시퀀스 (NN)
    // 증가: 2025100502 -> 2025100503
    val releaseVersionCode = 2025100503
    val releaseVersionName = "1.0.0"

    defaultConfig {
        applicationId = "kr.sweetapps.alcoholictimer" // 변경: Play Console에서 com.example.* 금지 대응
        minSdk = 21
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Play Console 경고 대응: 네이티브 심볼 업로드용 심볼 테이블 생성 (FULL 은 용량↑)
            debugSymbolLevel = "SYMBOL_TABLE"
        }
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
            val hasKeystore = !System.getenv("KEYSTORE_PATH").isNullOrBlank()
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                println("[INFO] Skipping signingConfig assignment for release (no KEYSTORE_PATH)")
            }
        }
        // debug 설정 변경 없음
    }

    // Java/Kotlin 타깃 유지
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // 필요시 buildConfig true (기본 true) / viewBinding 등 미사용
    }

    lint {
        // 릴리스 치명적 이슈 CI fail fast
        abortOnError = true
        warningsAsErrors = false // 초기 온보딩: 경고는 유지, 필요시 true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
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
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    // org.json (Android 내장) 를 JVM 유닛 테스트 환경에서 사용하기 위한 의존성
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// 디자인 토큰 규칙 검증 태스크 (금지된 literal 사용 여부 검사)
tasks.register("designTokenCheck") {
    group = "verification"
    description = "Checks for disallowed raw design token usages (alpha/elevation literals)."
    // 구성 캐시 비호환 (간단 Substring 스캔 + project API 접근)
    notCompatibleWithConfigurationCache("Ad-hoc file IO with project APIs; simplicity over cache support.")
    doLast {
        val forbidden = mapOf(
            // Alpha 직접 사용 금지
            "alpha = 0.9f" to "Remove raw alpha 0.9f. Use solid colors or AppAlphas.SurfaceTint (0.1f) only.",
            "alpha = 0.95f" to "Remove raw alpha 0.95f.",
            // Card elevation literal 금지 (토큰 사용 강제)
            "CardDefaults.cardElevation(defaultElevation = 1.dp" to "Use AppElevation.CARD (2.dp) or ZERO.",
            "CardDefaults.cardElevation(defaultElevation = 6.dp" to "Use AppElevation.CARD_HIGH (4.dp).",
            "CardDefaults.cardElevation(defaultElevation = 8.dp" to "Use AppElevation.CARD_HIGH (4.dp) only.",
            // 4dp 도 직접 쓰지 않고 토큰 쓰도록(TopAppBar shadowElevation 제외)
            "CardDefaults.cardElevation(defaultElevation = 4.dp" to "Replace literal 4.dp with AppElevation.CARD_HIGH.",
            // 표준에서 제외된 alpha copy 패턴
            ".surface.copy(alpha =" to "Avoid surface.copy(alpha=..). Use solid surface or official tint token."
        )
        val src = file("src/main/java")
        if (!src.exists()) return@doLast
        val violations = mutableListOf<String>()
        src.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { f ->
                val text = f.readText()
                forbidden.forEach { (pattern, msg) ->
                    if (text.contains(pattern)) {
                        val line = (text.lineSequence().indexOfFirst { it.contains(pattern) } + 1).coerceAtLeast(1)
                        violations += "${f.relativeTo(project.projectDir)}:$line -> $pattern :: $msg"
                    }
                }
            }
        if (violations.isNotEmpty()) {
            throw GradleException(buildString {
                appendLine("Design token violations detected:\n")
                violations.forEach { appendLine(" - $it") }
                appendLine("\nFix by using AppElevation / AppAlphas tokens.")
            })
        } else {
            println("Design token check passed (no violations found).")
        }
    }
}

// check 파이프라인에 포함
tasks.named("check").configure { dependsOn("designTokenCheck") }
