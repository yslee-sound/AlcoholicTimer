package com.sweetapps.alcoholictimer.feature.profile

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.sweetapps.alcoholictimer.R
import com.sweetapps.alcoholictimer.core.ui.BaseActivity
import com.sweetapps.alcoholictimer.core.ui.LocalSafeContentPadding
import com.sweetapps.alcoholictimer.core.ui.AdmobBanner

class NicknameEditActivity : BaseActivity() {
    override fun getScreenTitle(): String = getString(R.string.profile_edit_title)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen(
                showBackButton = true,
                onBackClick = { finish() },
                bottomAd = { AdmobBanner() }
            ) {
                NicknameEditScreen()
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

        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.profile_edit_instruction),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            OutlinedTextField(
                value = nicknameText,
                onValueChange = { nicknameText = it },
                label = { Text(stringResource(R.string.profile_nickname_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    saveNicknameAndFinish(nicknameText.trim())
                })
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { saveNicknameAndFinish(nicknameText.trim()) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = nicknameText.isNotBlank()
            ) {
                Text(text = stringResource(R.string.profile_save), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { finish() },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(text = stringResource(R.string.profile_cancel), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    private fun saveNicknameAndFinish(nickname: String) {
        if (nickname.isNotBlank()) {
            saveNickname(nickname)
            val resultIntent = Intent().apply { putExtra("new_nickname", nickname) }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getNickname(): String {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        return sharedPref.getString("nickname", getString(R.string.default_nickname)) ?: getString(R.string.default_nickname)
    }

    private fun saveNickname(nickname: String) {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        sharedPref.edit { putString("nickname", nickname) }
    }
}
