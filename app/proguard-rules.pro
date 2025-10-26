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
# Google AdMob & Play Services Ads
# ===================================================
# AdMob SDK 클래스 보호 (난독화 시 광고 로드 실패 방지)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# 앱 오프닝 광고
-keep class com.google.android.gms.ads.appopen.** { *; }

# UMP (User Messaging Platform) - GDPR 동의
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# Play Services Ads 기본
-keep class com.google.android.gms.internal.ads.** { *; }
-dontwarn com.google.android.gms.internal.ads.**

# ===================================================
# Kotlin Coroutines (광고 SDK에서 사용)
# ===================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ===================================================
# Compose (UI 프레임워크)
# ===================================================
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# ===================================================
# 앱 커스텀 광고 관련 클래스
# ===================================================
# 광고 매니저 클래스 보호
-keep class com.sweetapps.alcoholictimer.core.ads.** { *; }

# BuildConfig 보호 (광고 유닛 ID 포함)
-keep class com.sweetapps.alcoholictimer.BuildConfig { *; }

