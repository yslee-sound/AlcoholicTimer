package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.jvm.java

/**
 * 모든 액티비티의 베이스 클래스
 * 공통된 햄버거 메뉴와 네비게이션 기능을 제공합니다.
 */
abstract class BaseActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    protected fun BaseScreen(content: @Composable () -> Unit) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.background(Color.White),
                    drawerContainerColor = Color.White
                ) {
                    DrawerMenu(
                        nickname = "알중이1",
                        onNicknameClick = {},
                        onItemSelected = { menuItem ->
                            scope.launch { drawerState.close() }
                            handleMenuSelection(menuItem)
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(getScreenTitle()) },
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
                                    contentDescription = "메뉴"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(paddingValues)
                ) {
                    content()
                }
            }
        }
    }

    private fun handleMenuSelection(menuItem: String) {
        when (menuItem) {
            "금주" -> {
                if (this !is StatusActivity) {
                    navigateToActivity(StatusActivity::class.java)
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
                if (this !is TestActivity) {
                    navigateToActivity(TestActivity::class.java)
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
        overridePendingTransition(0, 0) // 화면 전환 효과 제거
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
        "기록" to Icons.Default.List,
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
                .width(80.dp) // 가로 크기를 수동으로 조절 가능
                .height(72.dp) // 세로 크기를 수동으로 조절 가능
                .align(Alignment.Start)
                .padding(start = 8.dp)
        ) {
            Surface(
                shape = CircleShape, // 원형 배경으로 변경
                color = Color(0xFF888888), // 더 진한 그레이 톤 배경 적용
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "아바타",
                    tint = Color.White, // 아이콘을 밝은 흰색으로 변경
                    modifier = Modifier.size(40.dp).align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nickname,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp) // 별명도 오른쪽으로 이동
                .clickable { onNicknameClick() }
        )
        Divider(modifier = Modifier.padding(vertical = 12.dp))
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
        Divider(modifier = Modifier.padding(vertical = 8.dp))
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

@Composable
fun DrawerMenuPreview(
    nickname: String = "알중이1",
    onNicknameClick: () -> Unit = {},
    onItemSelected: (String) -> Unit = {}
) {
    val menuItems = listOf(
        "금주" to Icons.Default.PlayArrow,
        "기록" to Icons.Default.List,
        "레벨" to Icons.Default.Star
    )

    val settingsItems = listOf(
        "설정" to Icons.Default.Settings,
        "테스트" to Icons.Default.Build // 대체 아이콘 사용
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .width(80.dp) // 가로 크기를 수동으로 조절 가능
                .height(72.dp) // 세로 크기를 수동으로 조절 가능
                .align(Alignment.Start)
                .padding(start = 8.dp)
        ) {
            Surface(
                shape = CircleShape, // 원형 배경으로 변경
                color = Color(0xFF888888), // 더 진한 그레이 톤 배경 적용
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "아바타",
                    tint = Color.White, // 아이콘을 밝은 흰색으로 변경
                    modifier = Modifier.size(40.dp).align(Alignment.Center)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nickname,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp) // 별명도 오른쪽으로 이동
                .clickable { onNicknameClick() }
        )
        Divider(modifier = Modifier.padding(vertical = 12.dp))
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

        // 구분선
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // 설정 메뉴 목록
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
