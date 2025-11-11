package kr.sweetapps.alcoholictimer

import android.content.Context
import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.core.ui.AdmobBanner
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import kr.sweetapps.alcoholictimer.core.ui.DrawerMenu
import kr.sweetapps.alcoholictimer.core.ui.LayoutConstants
import kr.sweetapps.alcoholictimer.core.ui.predictAnchoredBannerHeightDp
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.navigation.AlcoholicTimerNavGraph
import kr.sweetapps.alcoholictimer.navigation.Screen

/**
 * Single Activity 아키텍처의 메인 Activity
 *
 * 모든 화면은 Composable로 구현되며 Navigation Compose로 관리됩니다.
 * 광고는 Activity 레벨에 고정되어 화면 전환 시에도 재생성되지 않습니다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 시스템 바 설정
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE

        // 시스템 바 아이콘을 어둡게 설정 (흰 배경에서 보이도록)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    isAppearanceLightNavigationBars = true
                }
            }
        }

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
    val context = LocalContext.current

    // 시작 화면 결정 (금주 진행 중이면 Run, 아니면 Start)
    val sharedPref = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val startDestination = if (startTime > 0) Screen.Run.route else Screen.Start.route

    // 현재 화면 경로
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 닉네임 상태
    var nickname by remember {
        mutableStateOf(sharedPref.getString("nickname", context.getString(R.string.default_nickname))
            ?: context.getString(R.string.default_nickname))
    }

    // 닉네임 변경 감지
    LaunchedEffect(currentRoute) {
        nickname = sharedPref.getString("nickname", context.getString(R.string.default_nickname))
            ?: context.getString(R.string.default_nickname)
    }

    // 현재 선택된 메뉴 항목
    val selectedMenuItem = when (currentRoute) {
        Screen.Start.route, Screen.Run.route -> context.getString(R.string.drawer_menu_sobriety)
        Screen.Records.route, Screen.AllRecords.route -> context.getString(R.string.drawer_menu_records)
        Screen.Level.route -> context.getString(R.string.drawer_menu_level)
        Screen.Settings.route -> context.getString(R.string.drawer_menu_settings)
        Screen.About.route, Screen.AboutLicenses.route -> context.getString(R.string.drawer_menu_about)
        else -> null
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .background(Color.White),
                drawerContainerColor = Color.Transparent,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                DrawerMenu(
                    nickname = nickname,
                    selectedItem = selectedMenuItem,
                    onNicknameClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.NicknameEdit.route)
                        }
                    },
                    onItemSelected = { menuItem ->
                        scope.launch {
                            drawerState.close()

                            val targetRoute = when (menuItem) {
                                context.getString(R.string.drawer_menu_sobriety) -> {
                                    if (startTime > 0) Screen.Run.route else Screen.Start.route
                                }
                                context.getString(R.string.drawer_menu_records) -> Screen.Records.route
                                context.getString(R.string.drawer_menu_level) -> Screen.Level.route
                                context.getString(R.string.drawer_menu_settings) -> Screen.Settings.route
                                context.getString(R.string.drawer_menu_about) -> Screen.About.route
                                else -> null
                            }

                            targetRoute?.let { route ->
                                navController.navigate(route) {
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
                    }
                )
            }
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
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 배너 상단 간격
                    if (LayoutConstants.BANNER_TOP_GAP > 0.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(LayoutConstants.BANNER_TOP_GAP)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }

                    // 배너 상단 헤어라인
                    HorizontalDivider(
                        thickness = AppBorder.Hairline,
                        color = Color(0xFFE0E0E0)
                    )

                    // 배너 영역
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(predictAnchoredBannerHeightDp()),
                            contentAlignment = Alignment.Center
                        ) {
                            AdmobBanner()
                        }
                    }
                }
            }
        }
    }
}

