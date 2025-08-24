package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

// Activity imports
import com.example.alcoholictimer.RecordsActivity
import com.example.alcoholictimer.RunActivity
import com.example.alcoholictimer.StartActivity
import com.example.alcoholictimer.LevelActivity
import com.example.alcoholictimer.SettingsActivity
import com.example.alcoholictimer.TestActivity
import com.example.alcoholictimer.NicknameEditActivity

/**
 * 모든 액티비티의 베이스 클래스
 * 공통된 햄버거 메뉴와 네비게이션 기능을 제공합니다.
 */
abstract class BaseActivity : ComponentActivity() {

    // 별명 상태를 관리하는 mutable state
    private var nicknameState = mutableStateOf("")

    // 공통 버튼 위치 상수
    companion object {
        val STANDARD_BUTTON_BOTTOM_PADDING = 16.dp
        val STANDARD_BUTTON_HORIZONTAL_PADDING = 32.dp
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge 활성화 (상태 바가 표시되도록)
        enableEdgeToEdge()

        // 상태 바와 내비게이션 바 색상 조정
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
    protected fun BaseScreen(content: @Composable () -> Unit) {
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
                                            fontSize = 20.sp
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
                                                imageVector = Icons.Default.Menu,
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
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /**
     * 별명 편집 화면으로 네비게이션
     */
    private fun navigateToNicknameEdit() {
        val intent = Intent(this, NicknameEditActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /**
     * 각 액티비티에서 구현해야 할 화면 제목
     */
    protected abstract fun getScreenTitle(): String

    /**
     * 표준화된 하단 버튼 영역을 생성하는 공통 컴포넌트
     * 모든 화면에서 동일한 위치에 버튼이 배치되도록 보장합니다.
     */
    @Composable
    fun StandardBottomButtonArea(
        content: @Composable RowScope.() -> Unit
    ) {
        // 하단 버튼 (표준 위치)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = STANDARD_BUTTON_HORIZONTAL_PADDING,
                    end = STANDARD_BUTTON_HORIZONTAL_PADDING,
                    bottom = STANDARD_BUTTON_BOTTOM_PADDING
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }

    /**
     * 표준화된 화면 레이아웃 구조
     * 상단 콘텐츠와 하단 버튼 영역을 분리하여 일관된 레이아웃을 제공합니다.
     */
    @Composable
    fun StandardScreenLayout(
        topContent: @Composable ColumnScope.() -> Unit,
        bottomButtons: @Composable RowScope.() -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 콘텐츠 (가변 크기)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    content = topContent
                )
            }

            // 표준 하단 버튼 영역
            StandardBottomButtonArea(content = bottomButtons)
        }
    }
}

@Composable
fun DrawerMenu(
    nickname: String,
    onNicknameClick: () -> Unit,
    onItemSelected: (String) -> Unit
) {
    val menuItems = listOf(
        "금주" to Icons.Default.PlayArrow,
        "기록" to Icons.AutoMirrored.Filled.List,
        "레벨" to Icons.Default.Star
    )
    val settingsItems = listOf(
        "설정" to Icons.Default.Settings,
        "테스트" to Icons.Default.Build
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
            color = Color.White,
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
                    color = Color(0xFF6C5CE7),
                    modifier = Modifier.size(56.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "아바타",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = nickname,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "프로필 편집",
                            fontSize = 12.sp,
                            color = Color(0xFF74B9FF)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 메뉴 위 Divider
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE0E0E0),
            thickness = 1.dp
        )

        // 메인 메뉴 섹션
        Text(
            text = "메뉴",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF74B9FF),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        menuItems.forEach { (title, icon) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF74B9FF).copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color(0xFF74B9FF),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            fontSize = 18.sp, // 기존 16.sp에서 18.sp로 증가
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 설정 위 Divider
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE0E0E0),
            thickness = 1.dp
        )

        // 설정 섹션
        Text(
            text = "설정",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF74B9FF),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        settingsItems.forEach { (title, icon) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF74B9FF).copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color(0xFF74B9FF),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            fontSize = 18.sp, // 기존 16.sp에서 18.sp로 증가
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "fontScale 1.0", fontScale = 1.0f)
@Preview(showBackground = true, name = "fontScale 2.0", fontScale = 2.0f)
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
