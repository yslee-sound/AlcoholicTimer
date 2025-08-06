package com.example.alcoholictimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recordId = intent.getLongExtra("record_id", -1)

        setContent {
            BaseScreen {
                DetailScreen(recordId)
            }
        }
    }

    override fun getScreenTitle(): String = "기록 상세"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DetailScreen(recordId: Long) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "기록 상세보기",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 기록 ID 표시
            Text(
                text = "기록 ID: $recordId",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 추가 기록 정보 표시
            Text(
                text = "완료된 금주 기록의 상세정보가 여기에 표시됩니다.",
                fontSize = 16.sp
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewDetailScreen() {
        BaseScreen {
            DetailScreen(recordId = 1L)
        }
    }
}
