# 1. 기본 속성 및 어노테이션 유지 (필수)
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,EnclosingMethod,InnerClasses,JavascriptInterface

# 2. 광고 (AdMob & UMP) - 가장 안전한 광범위 설정
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

# 3. Firebase 및 데이터 모델 (Post 클래스 보호)
-keep class com.google.firebase.** { *; }
-keep class kr.sweetapps.alcoholictimer.data.model.** { *; }
# Post 클래스의 내부 필드가 서버와 매칭되도록 보존
-keepclassmembers class kr.sweetapps.alcoholictimer.data.model.Post { <fields>; <methods>; }

# 4. JSON 파싱 및 네트워크 (Moshi, Retrofit)
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-dontwarn com.squareup.moshi.**
-dontwarn retrofit2.**

# 5. Jetpack Compose 및 코루틴
-keep class androidx.compose.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 6. 앱 내부 커스텀 클래스 보존
-keep class kr.sweetapps.alcoholictimer.ui.ad.** { *; }
-keep class kr.sweetapps.alcoholictimer.BuildConfig { *; }

# 7. (선택) 로그 제거 시 빨간 줄이 난다면 아래 3줄만 사용하거나 아예 삭제하세요
-dontnote android.util.Log
-dontwarn android.util.Log