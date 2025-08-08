package com.example.alcoholictimer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : BaseActivity() {

    override fun getScreenTitle(): String = "설정"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    var selectedCost by remember { mutableStateOf("저") }
    var selectedFrequency by remember { mutableStateOf("주 1회 이하") }
    var selectedDuration by remember { mutableStateOf("짧음") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 음주 비용 설정
        Text("음주 비용", fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedCost == "저", onClick = { selectedCost = "저" })
                Text("저(1만원 이하)", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedCost == "중", onClick = { selectedCost = "중" })
                Text("중(1~5만원)", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedCost == "고", onClick = { selectedCost = "고" })
                Text("고(5만원 이상)", fontSize = 16.sp)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Black
        )

        // 음주 빈도 설정
        Text("음주 빈도", fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedFrequency == "주 1회 이하", onClick = { selectedFrequency = "주 1회 이하" })
                Text("주 1회 이하", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedFrequency == "주 2~3회", onClick = { selectedFrequency = "주 2~3회" })
                Text("주 2~3회", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedFrequency == "주 4회 이상", onClick = { selectedFrequency = "주 4회 이상" })
                Text("주 4회 이상", fontSize = 16.sp)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Black
        )

        // 음주 시간 설정
        Text("음주 시간", fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedDuration == "짧음", onClick = { selectedDuration = "짧음" })
                Text("짧음(2시간 이하)", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedDuration == "보통", onClick = { selectedDuration = "보통" })
                Text("보통(3~5시간)", fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedDuration == "김", onClick = { selectedDuration = "김" })
                Text("김(6시간 이상)", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 버튼들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            // 저장 버튼
            OutlinedButton(
                onClick = {
                    // TODO: SharedPreferences에 설정값 저장 로직 추가
                    // 예: saveSettings(selectedCost, selectedFrequency, selectedDuration)
                },
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                Text("저장", fontSize = 16.sp)
            }

            // 리셋 버튼
            OutlinedButton(
                onClick = {
                    selectedCost = "저"
                    selectedFrequency = "주 1회 이하"
                    selectedDuration = "짧음"
                },
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                Text("리셋", fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    SettingsScreen()
}
