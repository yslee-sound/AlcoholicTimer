package kr.sweetapps.alcoholictimer.ui.tab_05.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.theme.UiConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicknameEditScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE) }
    val currentNickname = remember { sp.getString("nickname", context.getString(R.string.default_nickname)) ?: context.getString(R.string.default_nickname) }
    var nicknameText by rememberSaveable { mutableStateOf(currentNickname) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val maxLen = 20
    val trimmed = nicknameText.trim()
    val isOnlySpaces = nicknameText.isNotEmpty() && trimmed.isEmpty()
    val isTooLong = nicknameText.length > maxLen
    val isUnchanged = trimmed == currentNickname
    val isValid = trimmed.isNotEmpty() && !isTooLong

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Scaffold with white background and top app bar (back)
    Scaffold(
        topBar = {
            BackTopBar(title = stringResource(R.string.profile_edit_title), onBack = onCancel)
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                onValueChange = {
                    // 공백만 무한 입력 방지: 앞뒤 스페이스 허용은 하되 전체 공백만 되지 않게 유지
                    nicknameText = if (it.length <= maxLen) it else it.take(maxLen)
                },
                label = { Text(stringResource(R.string.profile_nickname_label)) },
                singleLine = true,
                supportingText = {
                    val countText = "${nicknameText.length}/$maxLen"
                    val err = when {
                        isOnlySpaces -> stringResource(R.string.error_only_spaces)
                        isTooLong -> stringResource(R.string.error_too_long)
                        else -> null
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = err ?: "\u00A0", color = if (err != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = countText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                isError = isOnlySpaces || isTooLong,
                placeholder = { Text(text = stringResource(R.string.profile_nickname_placeholder)) },
                trailingIcon = {
                    if (nicknameText.isNotEmpty()) {
                        IconButton(onClick = { nicknameText = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear_text))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (isValid) {
                        keyboardController?.hide()
                        saveNickname(sp, trimmed)
                        onDone()
                    }
                })
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Only Save button (full-width)
            Button(
                onClick = {
                    saveNickname(sp, trimmed)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = isValid && !isUnchanged
            ) {
                Text(text = stringResource(R.string.profile_save), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun saveNickname(sp: android.content.SharedPreferences, nickname: String) {
    if (nickname.isNotBlank()) {
        sp.edit { putString("nickname", nickname) }
    }
}
