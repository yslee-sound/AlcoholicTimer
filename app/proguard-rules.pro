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
-keepattributes SourceFile,LineNumberTable

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

# ===================================================
# [FIX] 광고 렌더링 누락 방지 (WebView & JS) (2025-12-24)
# ===================================================
# 광고 콘텐츠(HTML/JS)를 렌더링하는 WebView 보호
-keep class android.webkit.** { *; }
-keep class com.google.android.gms.ads.internal.** { *; }

# 자바스크립트 인터페이스 메서드 보호 (광고 클릭/로드 트리거용)
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

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
-keep class kr.sweetapps.alcoholictimer.ui.ad.** { *; }

# BuildConfig 보존 (광고 유닛 ID 포함 가능)
-keep class kr.sweetapps.alcoholictimer.BuildConfig { *; }

# ===================================================
# 데이터 모델 클래스 (Firebase/Supabase 연동)
# [FIX] 릴리즈 빌드 난독화 방지 (2025-12-23)
# ===================================================
# Supabase 모델 클래스 보존
-keep class kr.sweetapps.alcoholictimer.data.supabase.model.** { *; }

# Firestore/Firebase 모델 클래스 보존
-keep class kr.sweetapps.alcoholictimer.data.model.** { *; }

# kotlinx.serialization 보존
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class kr.sweetapps.alcoholictimer.**$$serializer { *; }
-keepclassmembers class kr.sweetapps.alcoholictimer.** {
    *** Companion;
}
-keepclasseswithmembers class kr.sweetapps.alcoholictimer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===================================================
# 릴리즈 빌드: 디버그 로그 자동 제거 (2025-12-24)
# ===================================================
# Android Log 클래스의 디버그/정보 로그 제거
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# System.out.println 제거
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# --- AdMob Native Ads 지킴이 ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ads.** { *; }
-keep class com.google.android.gms.ads.nativead.** { *; }
-keep public class com.google.android.gms.ads.nativead.** {
   public *;
}

# (ViewBinding 사용 시 안전장치)
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static ** bind(android.view.View);
    public static ** inflate(android.view.LayoutInflater);
}

# --- App Open Ad (앱 오프닝 광고) 지킴이 ---
-keep class com.google.android.gms.ads.appopen.** { *; }