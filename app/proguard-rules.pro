# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===================================================
# Google AdMob & Play Services Ads (간소화된 keep)
# ===================================================
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# UMP (동의 폼 안정성 위해 전체 유지)
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# Compose (runtime/ui 핵심 public API만)
-keep class androidx.compose.runtime.* { *; }
-keep class androidx.compose.ui.* { *; }
# 과도한 ** 제거로 범위 축소

# 코루틴 (필요 클래스만)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ===================================================
# 앱 커스텀 광고 관련 클래스
# ===================================================
-keep class kr.sweetapps.alcoholictimer.core.ads.** { *; }

# BuildConfig 보존 (광고 유닛 ID 포함 가능)
-keep class kr.sweetapps.alcoholictimer.BuildConfig { *; }

