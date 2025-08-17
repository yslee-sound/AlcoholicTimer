package com.example.alcoholictimer.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.RecordsDataLoader
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    externalRefreshTrigger: Int,
    onNavigateToDetail: (SobrietyRecord) -> Unit = {}
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("월") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDetailPeriod by remember { mutableStateOf("") }

    // 데이터 로딩 함수
    val loadRecords = {
        isLoading = true
        try {
            val loadedRecords = RecordsDataLoader.loadSobrietyRecords(context)
            records = loadedRecords
            Log.d("RecordsScreen", "기록 로딩 완료: ${loadedRecords.size}개")
        } catch (e: Exception) {
            Log.e("RecordsScreen", "기록 로딩 실패", e)
        } finally {
            isLoading = false
        }
    }

    // 기간에 따른 기록 필터링
    val filteredRecords = remember(records, selectedPeriod, selectedDetailPeriod) {
        when (selectedPeriod) {
            "주" -> {
                if (selectedDetailPeriod.isNotEmpty()) {
                    // 특정 주 필터링 로직
                    val weekIndex = selectedDetailPeriod.substringAfter("week_").toIntOrNull() ?: 0
                    val weeksAgo = System.currentTimeMillis() - (weekIndex * 7 * 24 * 60 * 60 * 1000L)
                    val weeksAgoEnd = weeksAgo + (7 * 24 * 60 * 60 * 1000L)
                    records.filter { it.endTime >= weeksAgo && it.endTime < weeksAgoEnd }
                } else {
                    // 전체 주간 데이터
                    val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                    records.filter { it.endTime >= oneWeekAgo }
                }
            }
            "월" -> {
                if (selectedDetailPeriod.isNotEmpty()) {
                    // 특정 월 필터링 로직
                    val monthIndex = selectedDetailPeriod.substringAfter("month_").toIntOrNull() ?: 0
                    val monthsAgo = System.currentTimeMillis() - (monthIndex * 30 * 24 * 60 * 60 * 1000L)
                    val monthsAgoEnd = monthsAgo + (30 * 24 * 60 * 60 * 1000L)
                    records.filter { it.endTime >= monthsAgo && it.endTime < monthsAgoEnd }
                } else {
                    // 전체 월간 데이터
                    val oneMonthAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                    records.filter { it.endTime >= oneMonthAgo }
                }
            }
            "년" -> {
                if (selectedDetailPeriod.isNotEmpty()) {
                    // 특정 년 필터링 로직
                    val yearIndex = selectedDetailPeriod.substringAfter("year_").toIntOrNull() ?: 0
                    val yearsAgo = System.currentTimeMillis() - (yearIndex * 365 * 24 * 60 * 60 * 1000L)
                    val yearsAgoEnd = yearsAgo + (365 * 24 * 60 * 60 * 1000L)
                    records.filter { it.endTime >= yearsAgo && it.endTime < yearsAgoEnd }
                } else {
                    // 전체 연간 데이터
                    val oneYearAgo = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L)
                    records.filter { it.endTime >= oneYearAgo }
                }
            }
            else -> records // "전체"
        }
    }

    // 초기 로딩 및 외부 트리거에 따른 새로고침
    LaunchedEffect(externalRefreshTrigger) {
        loadRecords()
    }

    // 배경 그라데이션
    val gradientBackground = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFE9ECEF)
        ),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBackground)
    ) {
        // 기간 선택 탭 섹션
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            PeriodSelectionSection(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { period: String ->
                    selectedPeriod = period
                    selectedDetailPeriod = "" // 기간이 변경되면 세부 기간 초기화
                },
                onPeriodClick = { period: String ->
                    // 세부 기간 텍스트 클릭 시 바텀시트 표시
                    showBottomSheet = true
                },
                selectedDetailPeriod = selectedDetailPeriod
            )
        }

        // 해당 기간에 대한 정보를 보여주는 섹션
        if (!isLoading && filteredRecords.isNotEmpty()) {
            PeriodStatisticsSection(
                records = filteredRecords,
                selectedPeriod = selectedPeriod,
                selectedDetailPeriod = selectedDetailPeriod,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 기록 목록 영역
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (isLoading) {
                // 로딩 상태
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF74B9FF)
                    )
                }
            } else if (filteredRecords.isEmpty()) {
                // 빈 상태
                EmptyRecordsState(selectedPeriod, selectedDetailPeriod)
            } else {
                // 기록 목록
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRecords) { record ->
                        RecordCard(
                            record = record,
                            onClick = { onNavigateToDetail(record) }
                        )
                    }

                    // 마지막 아이템 아래 여백
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // 바텀시트 표시
    if (showBottomSheet) {
        when (selectedPeriod) {
            "주" -> {
                WeekPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onWeekPicked = { weekStart, weekEnd, displayText ->
                        selectedDetailPeriod = displayText // 바텀시트에서 제공하는 displayText 사용
                        showBottomSheet = false
                    }
                )
            }
            "월" -> {
                MonthPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onMonthPicked = { year, month ->
                        selectedDetailPeriod = "${year}년 ${month}월" // 읽기 좋은 형태로 저장
                        showBottomSheet = false
                    }
                )
            }
            "년" -> {
                YearPickerBottomSheet(
                    isVisible = true,
                    onDismiss = { showBottomSheet = false },
                    onYearPicked = { year ->
                        selectedDetailPeriod = "${year}년" // 읽기 좋은 형태로 저장
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyRecordsState(selectedPeriod: String, selectedSubPeriod: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF74B9FF).copy(alpha = 0.1f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "빈 상태",
                    tint = Color(0xFF74B9FF),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "아직 금주 기록이 없습니다",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "금주를 시작하고 완료하면\n기록이 여기에 표시됩니다",
            fontSize = 14.sp,
            color = Color(0xFF636E72),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 선택된 기간에 대한 안내 문구
        if (selectedPeriod != "전체") {
            Text(
                text = "선택한 기간: $selectedPeriod",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecordCard(
    record: SobrietyRecord,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    val startDate = dateFormat.format(Date(record.startTime))
    val endDate = dateFormat.format(Date(record.endTime))

    val successRate = if (record.targetDays > 0) {
        (record.actualDays.toFloat() / record.targetDays * 100).coerceAtMost(100f)
    } else {
        0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 헤더 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$startDate ~ $endDate",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = record.status,
                        fontSize = 12.sp,
                        color = if (record.isCompleted) Color(0xFF00B894) else Color(0xFFE17055)
                    )
                }

                // 완료 아이콘
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (record.isCompleted)
                        Color(0xFF00B894).copy(alpha = 0.1f)
                    else
                        Color(0xFFE17055).copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (record.isCompleted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = if (record.isCompleted) "완료" else "미완료",
                        tint = if (record.isCompleted) Color(0xFF00B894) else Color(0xFFE17055),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 진행률 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "달성 일수",
                        fontSize = 12.sp,
                        color = Color(0xFF636E72)
                    )
                    Text(
                        text = "${record.actualDays}일",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "목표 일수",
                        fontSize = 12.sp,
                        color = Color(0xFF636E72)
                    )
                    Text(
                        text = "${record.targetDays}일",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF636E72)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 진행률 바
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "달성률",
                        fontSize = 12.sp,
                        color = Color(0xFF636E72)
                    )
                    Text(
                        text = "${successRate.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isCompleted) Color(0xFF00B894) else Color(0xFF74B9FF)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE9ECEF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(successRate / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (record.isCompleted)
                                    Color(0xFF00B894)
                                else
                                    Color(0xFF74B9FF)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodStatisticsSection(
    records: List<SobrietyRecord>,
    selectedPeriod: String,
    selectedDetailPeriod: String,
    modifier: Modifier = Modifier
) {
    // 통계 계산
    val totalRecords = records.size
    val completedRecords = records.count { it.isCompleted }
    val successRate = if (totalRecords > 0) {
        (completedRecords.toFloat() / totalRecords * 100).toInt()
    } else 0

    val totalDays = records.sumOf { it.actualDays }
    val averageDays = if (totalRecords > 0) {
        totalDays / totalRecords
    } else 0

    val maxDays = records.maxOfOrNull { it.actualDays } ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDetailPeriod.isNotEmpty()) {
                        "$selectedDetailPeriod 통계"
                    } else {
                        "$selectedPeriod 통계"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF74B9FF).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${totalRecords}개 기록",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF74B9FF),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 통계 그리드
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 성공률
                StatisticItem(
                    title = "성공률",
                    value = "$successRate%",
                    color = Color(0xFF00B894),
                    modifier = Modifier.weight(1f)
                )

                // 평균 지속일
                StatisticItem(
                    title = "평균 지속일",
                    value = "${averageDays}일",
                    color = Color(0xFF74B9FF),
                    modifier = Modifier.weight(1f)
                )

                // 최대 지속일
                StatisticItem(
                    title = "최대 지속일",
                    value = "${maxDays}일",
                    color = Color(0xFFE17055),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 총 누적 일수
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "총 누적 금주일",
                    fontSize = 14.sp,
                    color = Color(0xFF636E72)
                )
                Text(
                    text = "${totalDays}일",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF636E72),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, name = "금주 기록 화면 - 빈 상태")
@Preview(showBackground = true, name = "금주 기록 화면 - 데이터 있음", fontScale = 1.2f)
@Composable
fun PreviewRecordsScreen() {
    MaterialTheme {
        // 샘플 데이터로 프리뷰
        RecordsScreen(
            externalRefreshTrigger = 0,
            onNavigateToDetail = {}
        )
    }
}

@Preview(showBackground = true, name = "빈 상태")
@Composable
fun PreviewEmptyRecordsState() {
    MaterialTheme {
        EmptyRecordsState("전체", "")
    }
}

@Preview(showBackground = true, name = "기록 카드")
@Composable
fun PreviewRecordCard() {
    MaterialTheme {
        RecordCard(
            record = SobrietyRecord(
                id = "sample",
                startTime = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L),
                endTime = System.currentTimeMillis(),
                targetDays = 30,
                actualDays = 10,
                isCompleted = false,
                status = "진행 중",
                createdAt = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "기간 통계 섹션")
@Composable
fun PreviewPeriodStatisticsSection() {
    MaterialTheme {
        PeriodStatisticsSection(
            records = listOf(
                SobrietyRecord(
                    id = "1",
                    startTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L),
                    endTime = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L),
                    targetDays = 30,
                    actualDays = 10,
                    isCompleted = false,
                    status = "실패",
                    createdAt = System.currentTimeMillis()
                ),
                SobrietyRecord(
                    id = "2",
                    startTime = System.currentTimeMillis() - (60 * 24 * 60 * 60 * 1000L),
                    endTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L),
                    targetDays = 30,
                    actualDays = 30,
                    isCompleted = true,
                    status = "성공",
                    createdAt = System.currentTimeMillis()
                )
            ),
            selectedPeriod = "월",
            selectedDetailPeriod = "2024년 8월",
            modifier = Modifier.padding(16.dp)
        )
    }
}
