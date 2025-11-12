package kr.sweetapps.alcoholictimer.feature.profile

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicknameEditScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE) }
    val currentNickname = remember { sp.getString("nickname", context.getString(R.string.default_nickname)) ?: context.getString(R.string.default_nickname) }
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
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                saveNickname(sp, nicknameText.trim())
                onDone()
            })
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onCancel() },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(text = stringResource(R.string.profile_cancel), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = {
                    saveNickname(sp, nicknameText.trim())
                    onDone()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = nicknameText.isNotBlank()
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

