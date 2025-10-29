package com.sweetapps.alcoholictimer.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties

// 앱 테마
import com.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import com.sweetapps.alcoholictimer.R

@Composable
fun AppUpdateDialog(
    isVisible: Boolean,
    versionName: String,
    updateMessage: String = "",
    updateMessageResourceId: Int? = null,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    canDismiss: Boolean = true
) {
    if (!isVisible) return

    // 로컬라이즈된 문자열
    val titleText = stringResource(id = R.string.update_dialog_title)
    val versionText = stringResource(id = R.string.update_dialog_version_format, versionName)
    // 변경: 리소스 ID가 전달되면 우선 사용, 아니면 서버에서 받은 메시지(있으면) 사용, 둘 다 아니면 기본 리소스 사용
    val displayedUpdateMessage = when {
        updateMessageResourceId != null -> stringResource(id = updateMessageResourceId)
        updateMessage.isNotBlank() -> updateMessage
        else -> stringResource(id = R.string.update_dialog_default_message)
    }
    val laterText = stringResource(id = R.string.update_dialog_later)
    val updateBtnText = stringResource(id = R.string.update_dialog_update)
    val iconDesc = stringResource(id = R.string.update_dialog_icon_description)

    Dialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = iconDesc,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = versionText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = displayedUpdateMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(laterText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = updateBtnText,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
