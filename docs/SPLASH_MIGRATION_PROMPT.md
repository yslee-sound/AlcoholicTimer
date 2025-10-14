# 재사용 프롬프트: 시스템/Compose 하이브리드 스플래시 적용

아래 프롬프트를 그대로 복사해 새로운 앱에 스플래시 전략을 적용하세요. 프로젝트에 맞게 테마 이름과 아이콘 리소스 이름만 바꾸면 됩니다.

---

내 안드로이드 앱에 다음 스플래시 전략을 적용해줘.

요구 사항
1) Android 12+(API 31+)
   - installSplashScreen 사용.
   - setOnExitAnimationListener로 220ms 페이드 아웃 + scale 1.05 적용.
   - setKeepOnScreenCondition으로 최소 800ms 보장.
   - values-v31/themes.xml에서 스플래시 테마를 구성하고
     android:windowSplashScreenAnimatedIcon은 @drawable/splash_app_icon_inset을 사용.
   - 메인 테마 android:windowBackground는 @android:color/white로 설정.

2) Android 11-(API 30-)
   - 메인 테마 android:windowBackground를 @drawable/splash_screen으로 통일(values, v23, v29).
   - 런처 액티비티의 setContent를 최소 표시시간만큼 지연.
   - Compose 오버레이를 AnimatedVisibility(fadeIn/fadeOut + scaleIn/scaleOut)로 표시하고,
     오버레이 종료 시 window.setBackgroundDrawable(null) 호출.
   - 오버레이 아이콘은 @drawable/splash_app_icon, 크기 240dp, 배경은 흰색.

3) 드로어블
   - drawable/splash_screen.xml: 흰 배경 + 중앙 아이콘 레이어.
   - drawable-anydpi-v26/splash_app_icon_inset.xml: 아이콘 사방 24dp 인셋.

4) 파라미터
   - 최소 표시시간 800ms, 애니메이션 220ms.
   - 필요 시 인셋 28~32dp, 애니메이션 200~300ms로 조정.

내 프로젝트 테마 이름은 `Theme.YourApp`, 스플래시 테마는 `Theme.YourApp.Splash`, 아이콘은 `@drawable/splash_app_icon`이야.
해당 파일들('StartActivity.kt', 'values*/themes.xml', 'drawable/splash_screen.xml', 'drawable-anydpi-v26/splash_app_icon_inset.xml')을 생성/수정해줘.

---

템플릿 변수(선택)
- 메인 테마: Theme.YourApp (예: Theme.MyCompany.App)
- 스플래시 테마: Theme.YourApp.Splash
- 스플래시 아이콘: @drawable/splash_app_icon
- 인셋 아이콘: @drawable/splash_app_icon_inset

적용 팁
- 31+에서는 Compose 오버레이를 사용하지 않습니다(시스템 스플래시만 사용).
- 30-에서는 windowBackground + Compose 오버레이 조합으로 최소 표시시간과 전환 애니메이션을 보장합니다.
- 오버레이 종료 직후 반드시 `window.setBackgroundDrawable(null)`를 호출해 잔상을 제거하세요.

