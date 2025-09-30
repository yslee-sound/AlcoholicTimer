package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.alcoholictimer.ui.theme.AlcoholicTimerTheme

/**
 * 모든 액티비티의 베이스 클래스
 * 공통된 햄버거 메뉴와 네비게이션 기능을 제공합니다.
 */
abstract class BaseActivity : ComponentActivity() {

    // 별명 상태를 관리하는 mutable state
    private var nicknameState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge 및 시스템 바 스타일 표준 API로 설정
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            )
        )

        // 초기 별명 로드
        nicknameState.value = getNickname()
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 나타날 때마다 별명을 업데이트
        nicknameState.value = getNickname()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BaseScreen(content: @Composable () -> Unit) {
        AlcoholicTimerTheme {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val currentNickname by nicknameState

            // 드로어 상태에 따른 블러 효과 애니메이션
            val blurRadius by animateFloatAsState(
                targetValue = if (drawerState.targetValue == DrawerValue.Open) 8f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "blur"
            )

            // 모던한 그라데이션 배경 색상
            val gradientBackground = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF8F9FA),
                    Color(0xFFE9ECEF)
                ),
                start = Offset(0f, 0f),
                end = Offset.Infinite
            )

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxWidth(0.8f) // 조금 더 넓게
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
                                    // 드로어가 완전히 닫힐 때까지 대기
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
                                            LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)
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
                                                .size(40.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFF8F9FA),
                                            shadowElevation = 2.dp
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        drawerState.open()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Menu,
                                                    contentDescription = "메뉴",
                                                    tint = Color(0xFF2C3E50),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = gradientBackground)
                            .padding(top = paddingValues.calculateTopPadding()) // TopAppBar 높이만큼만 상단 패딩 적용
                            .blur(radius = blurRadius.dp) // 블러 효과 적용
                    ) {
                        content()
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

                // 진행 중인 금주가 있는 경우
                if (startTime > 0) {
                    if (this !is RunActivity) {
                        navigateToActivity(RunActivity::class.java)
                    }
                } else {
                    // 진행 중인 금주가 없는 경우
                    if (this !is StartActivity) {
                        navigateToActivity(StartActivity::class.java)
                    }
                }
            }
            "기록" -> {
                if (this !is RecordsActivity) {
                    navigateToActivity(RecordsActivity::class.java)
                }
            }
            "레벨" -> {
                if (this !is LevelActivity) {
                    navigateToActivity(LevelActivity::class.java)
                }
            }
            "설정" -> {
                if (this !is SettingsActivity) {
                    navigateToActivity(SettingsActivity::class.java)
                }
            }
            "테스트" -> {
                if (this !is TestActivity) {
                    navigateToActivity(TestActivity::class.java)
                }
            }
        }
    }

    /**
     * 효과 없이 액티비티 전환
     */
    @Suppress("DEPRECATION")
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /**
     * 별명 편집 화면으로 네비게이션
     */
    @Suppress("DEPRECATION")
    private fun navigateToNicknameEdit() {
        val intent = Intent(this, NicknameEditActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /**
     * 각 액티비티에서 구현해야 할 화면 제목
     */
    protected abstract fun getScreenTitle(): String
}

@Composable
fun DrawerMenu(
    nickname: String,
    onNicknameClick: () -> Unit,
    onItemSelected: (String) -> Unit
) {
    val menuItems = listOf(
        // 변경: List는 RTL 대응을 위해 AutoMirrored 아이콘 사용
        "금주" to Icons.Filled.PlayArrow,
        "기록" to Icons.AutoMirrored.Filled.List,
        "레벨" to Icons.Filled.Star
    )
    val settingsItems = listOf(
        "설정" to Icons.Filled.Settings,
        "테스트" to Icons.Filled.Build
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // 프로필 섹션을 카드 스타일로 업데이트
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNicknameClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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

        // 메뉴 위 Divider → HorizontalDivider로 변경
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )

        // 메인 메뉴 섹션
        Text(
            text = "메뉴",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        menuItems.forEach { (title, icon) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
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

        // 설정 위 Divider → HorizontalDivider로 변경
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )

        // 설정 섹션
        Text(
            text = "설정",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        settingsItems.forEach { (title, icon) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
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

@Preview(showBackground = true)
@Composable
fun PreviewDrawerMenu() {
    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = rememberDrawerState(DrawerValue.Open),
            drawerContent = {
                ModalDrawerSheet {
                    DrawerMenu(
                        nickname = "알중이1",
                        onNicknameClick = {},
                        onItemSelected = {}
                    )
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {}
        }
    }
}
