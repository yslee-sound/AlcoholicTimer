package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

/**
 * 모든 액티비티의 베이스 클래스
 * 공통된 햄버거 메뉴와 네비게이션 기능을 제공합니다.
 */
abstract class BaseActivity : ComponentActivity() {

    // 별명 상태를 관리하는 mutable state
    private var nicknameState = mutableStateOf("")

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

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxWidth(0.75f) // 화면 가로의 3/4만 차지
                        .background(Color.White),
                    drawerContainerColor = Color.White,
                    drawerShape = RoundedCornerShape(0.dp) // 라운딩 제거 (직각 모서리)
                ) {
                    DrawerMenu(
                        nickname = currentNickname,
                        onNicknameClick = {
                            scope.launch { drawerState.close() }
                            navigateToNicknameEdit()
                        },
                        onItemSelected = { menuItem ->
                            scope.launch { drawerState.close() }
                            handleMenuSelection(menuItem)
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text(getScreenTitle(), color = Color.Black) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = Color.Black,
                                navigationIconContentColor = Color.Black,
                                actionIconContentColor = Color.Black
                            ),
                            navigationIcon = {
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
                                        tint = Color.Black
                                    )
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth().height(1.dp),
                            color = Color.LightGray
                        )
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(paddingValues)
                        .statusBarsPadding()
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

    private fun saveNickname(nickname: String) {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("nickname", nickname)
            apply()
        }
    }

    private fun handleMenuSelection(menuItem: String) {
        when (menuItem) {
            "금주" -> {
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                val startTime = sharedPref.getLong("start_time", 0L)

                // 진행 중인 금주가 있는 경우
                if (startTime > 0) {
                    navigateToActivity(RunActivity::class.java)
                } else {
                    // 진행 중인 금주가 없는 경우
                    navigateToActivity(StartActivity::class.java)
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
    }

    /**
     * 별명 편집 화면으로 네비게이션
     */
    private fun navigateToNicknameEdit() {
        val intent = Intent(this, NicknameEditActivity::class.java)
        startActivity(intent)
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
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(72.dp)
                .align(Alignment.Start)
                .padding(start = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF888888),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "아바타",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize(0.9f) // 원의 70% 크기로 설정
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nickname,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp)
                .clickable { onNicknameClick() }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        menuItems.forEach { (title, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemSelected(title) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 16.sp
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        settingsItems.forEach { (title, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemSelected(title) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 16.sp
                )
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
