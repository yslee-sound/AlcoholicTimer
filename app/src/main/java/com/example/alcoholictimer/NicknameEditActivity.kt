package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

class NicknameEditActivity : BaseActivity() {

    override fun getScreenTitle(): String = "별명 변경"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                BaseScreen {
                    NicknameEditScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NicknameEditScreen() {
        val currentNickname = getNickname()
        var nicknameText by remember { mutableStateOf(currentNickname) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        // 화면이 로드되면 텍스트 필드에 포커스를 주고 키보드를 띄움
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 안내 텍스트
            Text(
                text = "새로운 별명을 입력해주세요",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 별명 입력 필드
            OutlinedTextField(
                value = nicknameText,
                onValueChange = { nicknameText = it },
                label = { Text("별명") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        saveNicknameAndFinish(nicknameText.trim())
                    }
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 저장 버튼
            Button(
                onClick = {
                    saveNicknameAndFinish(nicknameText.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = nicknameText.isNotBlank()
            ) {
                Text(
                    text = "저장",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 취소 버튼
            OutlinedButton(
                onClick = {
                    finish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "취소",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    private fun saveNicknameAndFinish(nickname: String) {
        if (nickname.isNotBlank()) {
            saveNickname(nickname)

            // 결과를 이전 화면에 전달
            val resultIntent = Intent().apply {
                putExtra("new_nickname", nickname)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getNickname(): String {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        return sharedPref.getString("nickname", "알중이1") ?: "알중이1"
    }

    private fun saveNickname(nickname: String) {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        sharedPref.edit { putString("nickname", nickname) }
    }
}
