package com.example.alcoholictimer.feature.about

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.alcoholictimer.core.ui.BaseActivity
import com.example.alcoholictimer.R

class AboutActivity : BaseActivity() {
    override fun getScreenTitle(): String = getString(R.string.about_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BaseScreen { AboutScreen() } }
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val ccByUrl = "https://creativecommons.org/licenses/by/4.0/"
    val sourceUrl = "https://www.figma.com/files/team/1555631729927297611/resources/community/file/1149764730850773390?fuid=1555631727933133748"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 앱 아이콘 저작권 표기 카드
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_open_license_notice),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Text(text = stringResource(R.string.about_section_app_icon), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                LabeledText(label = stringResource(R.string.about_label_original_work), value = stringResource(R.string.about_value_icon_name))
                LabeledText(label = stringResource(R.string.about_label_author), value = stringResource(R.string.about_value_icon_author))
                LabeledText(
                    label = stringResource(R.string.about_label_source),
                    value = stringResource(R.string.about_label_source_link),
                    isLink = true,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, sourceUrl.toUri())) },
                    trailingContent = {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_copy),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(sourceUrl))
                                Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                LabeledText(
                    label = stringResource(R.string.about_label_license),
                    value = stringResource(R.string.about_value_license_cc_by),
                    isLink = true,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, ccByUrl.toUri())) }
                )
                LabeledText(label = stringResource(R.string.about_label_changes), value = stringResource(R.string.about_value_change_desc))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_notice_compliance),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F6C7B)
                )
            }
        }
    }
}

@Composable
private fun LabeledText(
    label: String,
    value: String,
    isLink: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val labelStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5F6C7B))
    val valueStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        if (isLink && onClick != null) {
            Text(
                text = value,
                modifier = Modifier.clickable(onClick = onClick),
                style = valueStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = TextDecoration.Underline
                )
            )
        } else {
            Text(text = value, style = valueStyle)
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
