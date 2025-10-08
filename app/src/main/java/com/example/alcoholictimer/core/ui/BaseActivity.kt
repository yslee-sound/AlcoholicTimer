package com.example.alcoholictimer.core.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import com.example.alcoholictimer.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import com.example.alcoholictimer.feature.level.LevelActivity
import com.example.alcoholictimer.feature.start.StartActivity
import com.example.alcoholictimer.feature.run.RunActivity
import com.example.alcoholictimer.feature.settings.SettingsActivity
import com.example.alcoholictimer.feature.profile.NicknameEditActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

abstract class BaseActivity : ComponentActivity() {
    private var nicknameState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install SplashScreen for Android 12+ and provide backport for older versions
        val splashScreen: SplashScreen = installSplashScreen()
        // Remove exit animation immediately to avoid icon/shape lingering
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
        }
        super.onCreate(savedInstanceState)
        // 중복 system bar 설정 제거: Theme SideEffect에서만 처리 (AlcoholicTimerTheme)
        nicknameState.value = getNickname()
    }

    override fun onResume() {
        super.onResume()
        nicknameState.value = getNickname()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BaseScreen(
        applyBottomInsets: Boolean = true,
        content: @Composable () -> Unit
    ) {
        // Force light theme regardless of system setting
        AlcoholicTimerTheme(darkTheme = false) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val currentNickname by nicknameState

            val blurRadius by animateFloatAsState(
                targetValue = if (drawerState.targetValue == DrawerValue.Open) 8f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "blur"
            )

            val gradientBackground = Brush.linearGradient(
                colors = listOf(
                    colorResource(id = R.color.color_bg_gradient_start),
                    colorResource(id = R.color.color_bg_gradient_mid),
                    colorResource(id = R.color.color_bg_gradient_end)
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color(0xFFF8F9FA)
                                    )
                                )
                            ),
                        drawerContainerColor = Color.Transparent,
                        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    ) {
                        DrawerMenu(
                            nickname = currentNickname,
                            onNicknameClick = {
                                scope.launch {
                                    drawerState.close()
                                    var isNicknameEditNavigated = false
                                    snapshotFlow { drawerState.isAnimationRunning }
                                        .collect { isAnimating ->
                                            if (!isAnimating && drawerState.currentValue == DrawerValue.Closed && !isNicknameEditNavigated) {
                                                isNicknameEditNavigated = true
                                                navigateToNicknameEdit()
                                                return@collect
                                            }
                                        }
                                }
                            },
                            onItemSelected = { menuItem ->
                                scope.launch {
                                    drawerState.close()
                                    snapshotFlow { drawerState.isAnimationRunning }
                                        .collect { isAnimating ->
                                            if (!isAnimating && drawerState.currentValue == DrawerValue.Closed) {
                                                handleMenuSelection(menuItem)
                                                return@collect
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
                    topBar = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 4.dp,
                            color = Color.White
                        ) {
                            Column {
                                TopAppBar(
                                    title = {
                                        CompositionLocalProvider(
                                            LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.2f)
                                        ) {
                                            Text(
                                                text = getScreenTitle(),
                                                color = Color(0xFF2C3E50),
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        titleContentColor = Color(0xFF2C3E50),
                                        navigationIconContentColor = Color(0xFF2C3E50),
                                        actionIconContentColor = Color(0xFF2C3E50)
                                    ),
                                    navigationIcon = {
                                        Surface(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(48.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFF8F9FA),
                                            shadowElevation = 2.dp
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch { drawerState.open() }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Menu,
                                                    contentDescription = "메뉴",
                                                    tint = Color(0xFF2C3E50),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    // 배경 gradient를 인셋 패딩 적용 전후 구분하려고 두 개의 Box 레이어로 분리
                    Box(Modifier.fillMaxSize()) {
                        // 전체 화면(시스템 바 영역 포함)에 gradient 적용
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush = gradientBackground)
                        )
                        val insetModifier = if (applyBottomInsets) {
                            Modifier.windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                            )
                        } else {
                            Modifier.windowInsetsPadding(
                                // 하단 제외: 좌우만 적용
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                            )
                        }
                        // 실제 콘텐츠 레이어: 시스템 바 안전 패딩 + 스캐폴드 패딩 적용
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .then(insetModifier)
                                .blur(radius = blurRadius.dp)
                        ) { content() }
                    }
                }
            }
        }
    }

    private fun getNickname(): String {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        return sharedPref.getString("nickname", "알중이1") ?: "알중이1"
    }

    private fun handleMenuSelection(menuItem: String) {
        when (menuItem) {
            "금주" -> {
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                val startTime = sharedPref.getLong("start_time", 0L)
                if (startTime > 0) {
                    if (this !is RunActivity) navigateToActivity(RunActivity::class.java)
                } else {
                    if (this !is StartActivity) navigateToActivity(StartActivity::class.java)
                }
            }
            "기록" -> {
                if (this !is com.example.alcoholictimer.feature.records.RecordsActivity) {
                    navigateToActivity(com.example.alcoholictimer.feature.records.RecordsActivity::class.java)
                }
            }
            "레벨" -> if (this !is LevelActivity) navigateToActivity(LevelActivity::class.java)
            "설정" -> if (this !is SettingsActivity) navigateToActivity(SettingsActivity::class.java)
            "앱 정보" -> if (this !is com.example.alcoholictimer.feature.about.AboutActivity) navigateToActivity(com.example.alcoholictimer.feature.about.AboutActivity::class.java)
        }
    }

    @Suppress("DEPRECATION")
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    @Suppress("DEPRECATION")
    private fun navigateToNicknameEdit() {
        val intent = Intent(this, NicknameEditActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    protected abstract fun getScreenTitle(): String
}

@Composable
fun DrawerMenu(
    nickname: String,
    onNicknameClick: () -> Unit,
    onItemSelected: (String) -> Unit
) {
    val menuItems = listOf(
        "금주" to Icons.Filled.PlayArrow,
        "기록" to Icons.AutoMirrored.Filled.List,
        "레벨" to Icons.Filled.Star
    )
    val settingsItems = listOf(
        "설정" to Icons.Filled.Settings,
        "앱 정보" to Icons.Filled.Info
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onNicknameClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "아바타",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "프로필 편집",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )
        Text(
            text = "메뉴",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        menuItems.forEach { (title, icon) ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceOverlaySoft // was surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )
        Text(
            text = "설정",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        settingsItems.forEach { (title, icon) ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceOverlaySoft // was surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
