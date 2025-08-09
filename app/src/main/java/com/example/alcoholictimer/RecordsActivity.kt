package com.example.alcoholictimer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class SobrietyRecord(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val targetDays: Int,
    val actualDays: Int,
    val isCompleted: Boolean,
    val status: String,
    val createdAt: Long
)

class RecordsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                RecordsScreen()
            }
        }
    }

    override fun getScreenTitle(): String = "금주 기록"

    @Composable
    private fun RecordsScreen() {
        val context = LocalContext.current
        var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }

        // 기록 로드
        LaunchedEffect(Unit) {
            records = loadSobrietyRecords(context)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "최근 활동",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (records.isEmpty()) {
                // 기록이 없을 때
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 금주 기록이 없습니다.\n첫 번째 금주를 시작해보세요!",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // 기록이 있을 때 카드 형태로 표시
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records) { record ->
                        SobrietyRecordCard(
                            record = record,
                            onClick = {
                                // DetailActivity로 이동
                                DetailActivity.start(
                                    context = context,
                                    startTime = record.startTime,
                                    endTime = record.endTime,
                                    targetDays = record.targetDays.toFloat(),
                                    actualDays = record.actualDays,
                                    isCompleted = record.isCompleted
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewRecordsScreen() {
        BaseScreen {
            RecordsScreen()
        }
    }
}

@Composable
fun SobrietyRecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    val startDate = dateFormatter.format(Date(record.startTime))
    val endDate = dateFormatter.format(Date(record.endTime))
    val startTime = timeFormatter.format(Date(record.startTime))
    val endTime = timeFormatter.format(Date(record.endTime))

    val duration = record.endTime - record.startTime
    val durationDays = (duration / (24 * 60 * 60 * 1000)).toInt()
    val durationHours = ((duration % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val durationMinutes = ((duration % (60 * 60 * 1000)) / (60 * 1000)).toInt()

    val progressPercent = if (record.targetDays > 0) {
        ((record.actualDays.toFloat() / record.targetDays) * 100).toInt()
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() }, // 클릭 이벤트 추가
        colors = CardDefaults.cardColors(
            containerColor = if (record.isCompleted) Color(0xFFE8F5E8) else Color(0xFFFFF3CD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단: 상태와 날짜
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 상태 배지
                Surface(
                    color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = record.status,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = if (startDate == endDate) startDate else "$startDate ~ $endDate",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 중앙: 주요 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 왼쪽: 달성 일수
                Column {
                    Text(
                        text = "${record.actualDays}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "달성",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 중앙: 목표 대비 진행률
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${progressPercent}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Text(
                        text = "목표 달성률",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 오른쪽: 목표 일수
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${record.targetDays}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "목표",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 하단: 상세 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "시작: $startTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "종료: $endTime",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (durationDays > 0 || durationHours > 0 || durationMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "지속 시간: ${durationDays}일 ${durationHours}시간 ${durationMinutes}분",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun loadSobrietyRecords(context: android.content.Context): List<SobrietyRecord> {
    return try {
        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
        val recordsJson = sharedPref.getString("sobriety_records", "[]") ?: "[]"
        val jsonArray = JSONArray(recordsJson)

        val records = mutableListOf<SobrietyRecord>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            records.add(
                SobrietyRecord(
                    id = jsonObject.getString("id"),
                    startTime = jsonObject.getLong("startTime"),
                    endTime = jsonObject.getLong("endTime"),
                    targetDays = jsonObject.getInt("targetDays"),
                    actualDays = jsonObject.getInt("actualDays"),
                    isCompleted = jsonObject.getBoolean("isCompleted"),
                    status = jsonObject.getString("status"),
                    createdAt = jsonObject.getLong("createdAt")
                )
            )
        }

        // 최신 순으로 정렬
        records.sortedByDescending { it.createdAt }
    } catch (e: Exception) {
        emptyList()
    }
}
