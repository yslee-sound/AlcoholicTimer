package kr.sweetapps.alcoholictimer.ui.tab_03.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.data.repository.UserRepository
import kr.sweetapps.alcoholictimer.ui.tab_03.components.AvatarSelectionDialog
import kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel.Tab05ViewModel
import kr.sweetapps.alcoholictimer.util.AvatarManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: Tab05ViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    val sp = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val uiState by viewModel.uiState.collectAsState()
    val currentNickname = remember {
        sp.getString("nickname", context.getString(R.string.default_nickname))
            ?: context.getString(R.string.default_nickname)
    }

    var nicknameText by remember { mutableStateOf(currentNickname) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필 편집") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (nicknameText.isNotBlank()) {
                                sp.edit().putString("nickname", nicknameText).apply()
                                userRepository.saveNickname(nicknameText)
                                // [FIX] ViewModel 상태 즉시 업데이트 (2025-12-23)
                                viewModel.refreshNickname(nicknameText)
                            }
                            onBack()
                        }
                    ) {
                        Text("완료", color = Color(0xFF6366F1))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111827)
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { showAvatarDialog = true }
            ) {
                Image(
                    painter = painterResource(id = AvatarManager.getAvatarResId(uiState.avatarIndex)),
                    contentDescription = "프로필 아바타",
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, Color(0xFFE0E0E0), CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "아바타 변경",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "아바타 변경",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = nicknameText,
                onValueChange = {
                    if (it.length <= 10) {
                        nicknameText = it
                    }
                },
                label = { Text("닉네임") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    focusedLabelColor = Color(0xFF6366F1)
                ),
                supportingText = {
                    Text(
                        text = "${nicknameText.length}/10",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "* 최대 10자까지 입력 가능합니다",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatarIndex = uiState.avatarIndex,
            onAvatarSelected = { index ->
                viewModel.updateAvatar(index)
            },
            onDismiss = { showAvatarDialog = false }
        )
    }
}

