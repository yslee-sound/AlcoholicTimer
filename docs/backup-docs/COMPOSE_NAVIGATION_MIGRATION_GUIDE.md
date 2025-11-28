# Jetpack Compose Navigation 마이그레이션 가이드

## 🎯 목표
Multiple Activity → **Single Activity + Compose Navigation**으로 전환하여 광고 깜빡임 문제를 근본적으로 해결

## 📚 Jetpack Compose 정석 아키텍처

```
MainActivity (Single Activity)
├── Scaffold
│   ├── TopBar (고정)
│   ├── Drawer (고정)
│   └── NavHost
│       ├── StartScreen
│       ├── RunScreen
│       ├── RecordsScreen
│       ├── LevelScreen
│       └── SettingsScreen
└── 배너 광고 (Activity 레벨, 절대 재생성 안됨) ✅
```

## ✅ 장점

1. **광고 깜빡임 완전 제거** ⭐⭐⭐⭐⭐
   - Activity가 하나이므로 광고 View가 절대 재생성되지 않음
   - 화면 전환 시 Composable만 교체

2. **Compose의 장점 100% 활용**
   - 선언형 UI의 이점 극대화
   - 애니메이션과 전환 효과 자연스러움

3. **성능 향상**
   - Activity 생성 오버헤드 없음
   - 메모리 사용량 감소

4. **유지보수성 향상**
   - 코드 구조 단순화
   - 상태 관리 용이

## 📋 구현 단계

### 1단계: 의존성 추가 ✅

`gradle/libs.versions.toml`:
```toml
[versions]
navigationCompose = "2.9.0"

[libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

`app/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.androidx.navigation.compose)
}
```

### 2단계: Screen 정의

`navigation/Screen.kt`:
```kotlin
package kr.sweetapps.alcoholictimer.navigation

sealed class Screen(val route: String) {
    object Start : Screen("start")
    object Run : Screen("run")
    object Records : Screen("records")
    object AllRecords : Screen("all_records")
    object Level : Screen("level")
    object Settings : Screen("settings")
    object About : Screen("about")
    object NicknameEdit : Screen("nickname_edit")
    object Detail : Screen("detail/{startTime}/{endTime}/{targetDays}/{actualDays}/{isCompleted}") {
        fun createRoute(
            startTime: Long,
            endTime: Long,
            targetDays: Float,
            actualDays: Int,
            isCompleted: Boolean
        ) = "detail/$startTime/$endTime/$targetDays/$actualDays/$isCompleted"
    }
}
```

### 3단계: NavGraph 생성

`navigation/NavGraph.kt`:
```kotlin
package kr.sweetapps.alcoholictimer.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kr.sweetapps.alcoholictimer.feature.start.StartScreen
import kr.sweetapps.alcoholictimer.feature.run.RunScreen
import kr.sweetapps.alcoholictimer.feature.records.RecordsScreen
import kr.sweetapps.alcoholictimer.feature.level.LevelScreen
import kr.sweetapps.alcoholictimer.feature.settings.SettingsScreen
// ... 기타 import

@Composable
fun AlcoholicTimerNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Start.route) {
            StartScreen(
                onNavigateToRun = { navController.navigate(Screen.Run.route) }
            )
        }
        
        composable(Screen.Run.route) {
            RunScreen(
                onNavigateToQuit = { /* ... */ }
            )
        }
        
        composable(Screen.Records.route) {
            RecordsScreen(
                onNavigateToDetail = { start, end, target, actual, completed ->
                    navController.navigate(
                        Screen.Detail.createRoute(start, end, target, actual, completed)
                    )
                }
            )
        }
        
        composable(Screen.Level.route) {
            LevelScreen()
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        
        // Detail Screen with arguments
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("startTime") { type = NavType.LongType },
                navArgument("endTime") { type = NavType.LongType },
                navArgument("targetDays") { type = NavType.FloatType },
                navArgument("actualDays") { type = NavType.IntType },
                navArgument("isCompleted") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val startTime = backStackEntry.arguments?.getLong("startTime") ?: 0L
            val endTime = backStackEntry.arguments?.getLong("endTime") ?: 0L
            val targetDays = backStackEntry.arguments?.getFloat("targetDays") ?: 30f
            val actualDays = backStackEntry.arguments?.getInt("actualDays") ?: 0
            val isCompleted = backStackEntry.arguments?.getBoolean("isCompleted") ?: false
            
            DetailScreen(
                startTime = startTime,
                endTime = endTime,
                targetDays = targetDays,
                actualDays = actualDays,
                isCompleted = isCompleted,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

### 4단계: MainActivity 리팩토링

`MainActivity.kt`:
```kotlin
package kr.sweetapps.alcoholictimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import kr.sweetapps.alcoholictimer.core.ui.AdmobBanner
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.navigation.AlcoholicTimerNavGraph
import kr.sweetapps.alcoholictimer.navigation.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 시스템 바 설정
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        
        setContent {
            AlcoholicTimerTheme(darkTheme = false) {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 시작 화면 결정 (금주 진행 중이면 Run, 아니면 Start)
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val startDestination = if (startTime > 0) Screen.Run.route else Screen.Start.route
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu(
                currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route,
                onNavigate = { screen ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(screen.route) {
                            // 동일 화면 중복 방지
                            launchSingleTop = true
                            // 백스택 관리
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize()) {
                // 컨텐츠 영역 (가중치로 배너 공간 제외)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(paddingValues)
                ) {
                    AlcoholicTimerNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
                
                // 🎯 고정 배너 영역 (절대 재생성 안됨!)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(predictAnchoredBannerHeightDp())
                            .navigationBarsPadding()
                    ) {
                        AdmobBanner()
                    }
                }
            }
        }
    }
}
```

### 5단계: Screen Composable 변환

기존 Activity를 Composable 함수로 변환:

**이전 (Activity)**:
```kotlin
class StartActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StartScreen()
        }
    }
}
```

**이후 (Composable)**:
```kotlin
@Composable
fun StartScreen(
    onNavigateToRun: () -> Unit = {}
) {
    // UI 구현
    // Activity 전환 대신 navigation callback 호출
    Button(onClick = onNavigateToRun) {
        Text("금주 시작")
    }
}
```

## 📊 마이그레이션 체크리스트

### Phase 1: 준비 ✅
- [x] Navigation Compose 의존성 추가
- [x] Screen sealed class 생성
- [x] NavGraph 생성
- [x] MainActivity 생성
- [x] AndroidManifest.xml 수정

### Phase 2: 핵심 화면 전환 (진행 중)
- [x] MainActivity 리팩토링
- [ ] StartActivity → StartScreen (기존 UI 재사용)
- [ ] RunActivity → RunScreen (기존 UI 재사용)
- [ ] RecordsActivity → RecordsScreen (기존 UI 재사용)
- [ ] LevelActivity → LevelScreen (기존 UI 재사용)
- [ ] SettingsActivity → SettingsScreen (기존 UI 재사용)

### Phase 3: 보조 화면 전환
- [ ] NicknameEditActivity → NicknameEditScreen
- [ ] DetailActivity → DetailScreen
- [ ] AboutActivity → AboutScreen
- [ ] QuitActivity → QuitScreen (Dialog로 전환 가능)

### Phase 4: 정리
- [ ] 기존 BaseActivity 제거
- [ ] AndroidManifest.xml에서 불필요한 Activity 선언 제거
- [ ] 테스트 및 검증

## 🎯 예상 효과

### Before (Multiple Activity)
```
화면 전환 시간: 200-300ms
광고 깜빡임: ❌ 있음
메모리 사용: 높음 (Activity 스택)
코드 복잡도: 높음 (BaseActivity, Intent 등)
```

### After (Single Activity + Navigation)
```
화면 전환 시간: 50-100ms ⚡
광고 깜빡임: ✅ 없음 (완전히 고정)
메모리 사용: 낮음 (Composable만 교체)
코드 복잡도: 낮음 (선언형, 명확한 흐름)
```

## 💡 마이그레이션 팁

1. **점진적 전환**
   - 한 번에 모든 화면을 전환하지 말고, 핵심 화면부터 시작
   - 기존 Activity와 새 Navigation을 혼용 가능

2. **상태 관리**
   - ViewModel을 사용하여 화면 간 상태 공유
   - Navigation arguments로 데이터 전달

3. **백스택 관리**
   - `popUpTo`로 백스택 제어
   - `launchSingleTop`으로 중복 방지

4. **딥링크 설정**
   - 필요시 Navigation에 딥링크 추가

## 🔗 참고 자료

- [Jetpack Navigation Compose 공식 문서](https://developer.android.com/jetpack/compose/navigation)
- [Navigation Best Practices](https://developer.android.com/guide/navigation/navigation-principles)
- [Single Activity: Why, When, and How](https://www.youtube.com/watch?v=2k8x8V77CrU)

## 📝 다음 단계

1. `navigation` 패키지 생성
2. `Screen.kt` 작성
3. `NavGraph.kt` 작성
4. `MainActivity` 리팩토링
5. 각 화면을 순차적으로 Composable로 전환

**이것이 Jetpack Compose의 정석입니다!** 🚀

