package com.example.alcoholictimer.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.RecordsDataLoader
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

class AddTestRecordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AddTestRecordScreen(
                    onSave = { record ->
                        // 기록 저장
                        val success = saveTestRecord(record)
                        if (success) {
                            Toast.makeText(this, "테스트 기록이 추가되었습니다", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this, "선택한 시간이 기존 기록과 중복됩니다", Toast.LENGTH_LONG).show()
                        }
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun saveTestRecord(record: SobrietyRecord): Boolean {
        return try {
            val currentRecords = RecordsDataLoader.loadSobrietyRecords(this).toMutableList()

            // 시간 중복 체크
            val hasTimeConflict = currentRecords.any { existingRecord ->
                // 새 기록의 시간 범위와 기존 기록의 시간 범위가 겹치는지 확인
                val newStart = record.startTime
                val newEnd = record.endTime
                val existingStart = existingRecord.startTime
                val existingEnd = existingRecord.endTime

                // 시간 범위 겹침 체크: 새 기록이 기존 기록과 겹치는 경우
                (newStart < existingEnd && newEnd > existingStart)
            }

            if (hasTimeConflict) {
                Log.w("AddTestRecord", "시간이 중복되는 기록이 이미 존재합니다")
                return false
            }

            // 새 기록 추가
            currentRecords.add(record)

            // 완료 시간(endTime) 기준으로 내림차순 정렬 (최신이 위로)
            currentRecords.sortByDescending { it.endTime }

            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            val jsonString = SobrietyRecord.toJsonArray(currentRecords)

            sharedPref.edit { putString("sobriety_records", jsonString) }

            Log.d("AddTestRecord", "테스트 기록 저장 완료: ${record.id}")
            true
        } catch (e: Exception) {
            Log.e("AddTestRecord", "테스트 기록 저장 실패", e)
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTestRecordScreen(
    onSave: (SobrietyRecord) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var targetDays by remember { mutableStateOf("30.0") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)) }
    var startTime by remember { mutableStateOf(Pair(9, 0)) } // 시, 분
    var endTime by remember { mutableStateOf(Pair(18, 0)) } // 시, 분

    // 날짜 포맷터
    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())

    // 실제 기간 계산 및 완료 상태 계산
    val (actualDays, isCompleted, status) = remember(startDate, endDate, startTime, endTime, targetDays) {
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startDate
            set(Calendar.HOUR_OF_DAY, startTime.first)
            set(Calendar.MINUTE, startTime.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = endDate
            set(Calendar.HOUR_OF_DAY, endTime.first)
            set(Calendar.MINUTE, endTime.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val durationMillis = endCalendar.timeInMillis - startCalendar.timeInMillis
        val actualDaysFloat = durationMillis / (24 * 60 * 60 * 1000.0)
        val targetDaysFloat = targetDays.toDoubleOrNull() ?: 0.0

        val completed = actualDaysFloat >= targetDaysFloat
        val statusText = if (completed) "성공" else "실패"

        Triple(actualDaysFloat, completed, statusText)
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
        // 상단 앱바
        TopAppBar(
            title = {
                Text(
                    text = "테스트 기록 추가",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF2C3E50)
            )
        )

        // 스크롤 가능한 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 기간 설정 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "금주 기간 설정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 시작일 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "시작일",
                            fontSize = 14.sp,
                            color = Color(0xFF636E72)
                        )

                        Surface(
                            modifier = Modifier.clickable {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = startDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCalendar = Calendar.getInstance()
                                        newCalendar.set(year, month, dayOfMonth)
                                        startDate = newCalendar.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF74B9FF).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "날짜 선택",
                                    tint = Color(0xFF74B9FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dateFormat.format(Date(startDate)),
                                    fontSize = 14.sp,
                                    color = Color(0xFF74B9FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 시작 시간 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "시작 시간",
                            fontSize = 14.sp,
                            color = Color(0xFF636E72)
                        )

                        Surface(
                            modifier = Modifier.clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        startTime = Pair(hourOfDay, minute)
                                    },
                                    startTime.first,
                                    startTime.second,
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF74B9FF).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "시간 선택",
                                    tint = Color(0xFF74B9FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", startTime.first, startTime.second),
                                    fontSize = 14.sp,
                                    color = Color(0xFF74B9FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 종료일 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "종료일",
                            fontSize = 14.sp,
                            color = Color(0xFF636E72)
                        )

                        Surface(
                            modifier = Modifier.clickable {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = endDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCalendar = Calendar.getInstance()
                                        newCalendar.set(year, month, dayOfMonth)
                                        endDate = newCalendar.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF74B9FF).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "날짜 선택",
                                    tint = Color(0xFF74B9FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dateFormat.format(Date(endDate)),
                                    fontSize = 14.sp,
                                    color = Color(0xFF74B9FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 종료 시간 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "종료 시간",
                            fontSize = 14.sp,
                            color = Color(0xFF636E72)
                        )

                        Surface(
                            modifier = Modifier.clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        endTime = Pair(hourOfDay, minute)
                                    },
                                    endTime.first,
                                    endTime.second,
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF74B9FF).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "시간 선택",
                                    tint = Color(0xFF74B9FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", endTime.first, endTime.second),
                                    fontSize = 14.sp,
                                    color = Color(0xFF74B9FF)
                                )
                            }
                        }
                    }
                }
            }

            // 목표 일수 설정 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "목표 일수",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 목표 일수 (소수점 지원)
                    OutlinedTextField(
                        value = targetDays,
                        onValueChange = { newValue ->
                            // 숫자와 소수점만 허용
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                targetDays = newValue
                            }
                        },
                        label = { Text("목표 일수 (소수점 가능, 예: 0.1일)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF74B9FF),
                            focusedLabelColor = Color(0xFF74B9FF)
                        ),
                        supportingText = {
                            Text(
                                text = "실제 기간: ${String.format(Locale.getDefault(), "%.2f", actualDays)}일",
                                color = if (isCompleted) Color(0xFF00B894) else Color(0xFFE17055)
                            )
                        }
                    )
                }
            }

            // 완료 상태 표시 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "완료 상태",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = status,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color(0xFF00B894) else Color(0xFFE17055)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isCompleted) {
                            "목표 일수를 달성했습니다! 🎉"
                        } else {
                            "목표 일수에 도달하지 못했습니다."
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF636E72)
                    )
                }
            }

            // 저장 및 취소 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 취소 버튼
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF636E72)
                    )
                ) {
                    Text("취소", fontSize = 16.sp)
                }

                // 저장 버튼
                Button(
                    onClick = {
                        try {
                            val targetDaysDouble = targetDays.toDoubleOrNull() ?: 0.0

                            // 시작 시간과 종료 시간을 포함한 정확한 타임스탬프 계산
                            val startCalendar = Calendar.getInstance().apply {
                                timeInMillis = startDate
                                set(Calendar.HOUR_OF_DAY, startTime.first)
                                set(Calendar.MINUTE, startTime.second)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val endCalendar = Calendar.getInstance().apply {
                                timeInMillis = endDate
                                set(Calendar.HOUR_OF_DAY, endTime.first)
                                set(Calendar.MINUTE, endTime.second)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val actualDurationDays = ((endCalendar.timeInMillis - startCalendar.timeInMillis) / (24.0 * 60 * 60 * 1000)).coerceAtLeast(0.0)
                            val defaultGoalDays = 30.0
                            val computedPercentage = if (targetDaysDouble > 0.0) {
                                com.example.alcoholictimer.utils.PercentUtils.roundPercent((actualDurationDays / targetDaysDouble) * 100.0)
                            } else {
                                // 목표가 0일인 경우 기본 목표(30일) 대비 진행률로 계산
                                com.example.alcoholictimer.utils.PercentUtils.roundPercent((actualDurationDays / defaultGoalDays) * 100.0)
                            }

                            val record = SobrietyRecord(
                                id = "test_${System.currentTimeMillis()}",
                                startTime = startCalendar.timeInMillis,
                                endTime = endCalendar.timeInMillis,
                                targetDays = targetDaysDouble.toInt(), // 기존 호환성을 위해 Int로 변환(소수는 절삭)
                                actualDays = actualDays.toInt(), // 계산된 실제 일수
                                isCompleted = isCompleted,
                                status = status,
                                createdAt = System.currentTimeMillis(),
                                percentage = computedPercentage,
                                isTestRecord = true
                            )

                            onSave(record)
                        } catch (e: Exception) {
                            Log.e("AddTestRecord", "기록 생성 실패", e)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF74B9FF),
                        contentColor = Color.White
                    )
                ) {
                    Text("저장", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            // 하단 여백
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Preview 함수들
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddTestRecordScreenPreview() {
    MaterialTheme {
        AddTestRecordScreen(
            onSave = { },
            onCancel = { }
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun AddTestRecordScreenCompactPreview() {
    MaterialTheme {
        AddTestRecordScreen(
            onSave = { },
            onCancel = { }
        )
    }
}

@Preview(showBackground = true, widthDp = 600, heightDp = 800)
@Composable
fun AddTestRecordScreenTabletPreview() {
    MaterialTheme {
        AddTestRecordScreen(
            onSave = { },
            onCancel = { }
        )
    }
}
