package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    // 디버깅용 태그
    private val TAG = "RecordsActivity"

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
            Log.d(TAG, "로드된 기록: ${records.size}개")

            // 기록이 없으면 테스트용 더미 데이터 추가
            if (records.isEmpty()) {
                Log.d(TAG, "기록이 없어서 테스트용 더미 데이터 추가")
                records = listOf(
                    SobrietyRecord(
                        id = "test1",
                        startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
                        endTime = System.currentTimeMillis(),
                        targetDays = 30,
                        actualDays = 7,
                        isCompleted = false,
                        status = "중단됨",
                        createdAt = System.currentTimeMillis()
                    ),
                    SobrietyRecord(
                        id = "test2",
                        startTime = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L),
                        endTime = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L),
                        targetDays = 14,
                        actualDays = 14,
                        isCompleted = true,
                        status = "완료",
                        createdAt = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
                    )
                )
                Log.d(TAG, "더미 데이터 추가 완료: ${records.size}개")
                records.forEach { record ->
                    Log.d(TAG, "더미 데이터: id=${record.id}, actualDays=${record.actualDays}, targetDays=${record.targetDays}")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 상단: 기간 선택 탭 섹션
            item {
                PeriodSelectionSection()
            }

            // 중단: 통계 및 그래프 섹션
            item {
                StatisticsSection(records = records)
            }

            // 하단: 최근 활동 섹션 헤더
            item {
                Text(
                    text = "최근 활동",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (records.isEmpty()) {
                // 기록이 없을 때
                item {
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
                }
            } else {
                // 기록이 있을 때 각 카드를 개별 아이템으로 표시
                items(records) { record ->
                    SobrietyRecordCard(
                        record = record,
                        onClick = { handleCardClick(record) }
                    )
                }
            }
        }
    }

    private fun handleCardClick(record: SobrietyRecord) {
        Log.d(TAG, "===== 카드 클릭 시작 =====")
        Log.d(TAG, "카드 클릭: ${record.id}")
        Log.d(TAG, "actualDays=${record.actualDays}, targetDays=${record.targetDays}")
        Log.d(TAG, "startTime=${record.startTime}, endTime=${record.endTime}")
        Log.d(TAG, "isCompleted=${record.isCompleted}")

        try {
            // 데이터 유효성 검사 (더 관대하게)
            if (record.actualDays < 0) {
                Log.e(TAG, "잘못된 기록 데이터: actualDays=${record.actualDays}")
                return
            }

            Log.d(TAG, "Intent 생성 시작...")

            // targetDays가 0이면 기본값으로 설정
            val safeTargetDays = if (record.targetDays <= 0) 30 else record.targetDays

            // DetailActivity로 이동
            val intent = Intent(this@RecordsActivity, DetailActivity::class.java)
            intent.putExtra("start_time", record.startTime)
            intent.putExtra("end_time", record.endTime)
            intent.putExtra("target_days", safeTargetDays.toFloat())
            intent.putExtra("actual_days", record.actualDays)
            intent.putExtra("is_completed", record.isCompleted)

            Log.d(TAG, "Intent 데이터 전달: targetDays=$safeTargetDays, actualDays=${record.actualDays}")
            Log.d(TAG, "DetailActivity 호출...")
            startActivity(intent)
            Log.d(TAG, "startActivity 호출 완료")
            Log.d(TAG, "===== 카드 클릭 종료 =====")
        } catch (e: Exception) {
            Log.e(TAG, "CardDetail 화면 이동 중 오류", e)
            Log.e(TAG, "오류 스택트레이스: ${e.stackTraceToString()}")
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
    } catch (_: Exception) {
        emptyList()
    }
}

// 상단: 기간 선택 탭 섹션
@Composable
fun PeriodSelectionSection() {
    var selectedPeriod by remember { mutableStateOf("월") }
    val periods = listOf("주", "월", "년", "전체")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "기간별 보기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 라운딩된 한 줄 버튼 디자인 (iOS 스타일 segmented control)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFE0E0E0),
                        RoundedCornerShape(25.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                periods.forEach { period ->
                    val isSelected = period == selectedPeriod
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) Color.Black else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period,
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// 중단: 통계 및 그래프 섹션
@Composable
fun StatisticsSection(records: List<SobrietyRecord>) {
    val totalDays = records.sumOf { it.actualDays }
    val completedCount = records.count { it.isCompleted }
    val totalAttempts = records.size
    val successRate = if (totalAttempts > 0) (completedCount * 100) / totalAttempts else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "통계 요약",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 상단 통계 카드들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "총 금주일",
                    value = "${totalDays}일",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "성공률",
                    value = "${successRate}%",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "시도 횟수",
                    value = "${totalAttempts}회",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TODO: 향후 그래프 영역 추가 예정
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "그래프 영역\n(향후 구현 예정)",
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
