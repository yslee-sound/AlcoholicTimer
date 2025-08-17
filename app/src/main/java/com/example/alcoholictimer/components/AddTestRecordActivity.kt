package com.example.alcoholictimer.components

import android.app.DatePickerDialog
import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.utils.RecordsDataLoader
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*

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
                            Toast.makeText(this, "기록 저장에 실패했습니다", Toast.LENGTH_SHORT).show()
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
            currentRecords.add(record)

            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            val jsonString = SobrietyRecord.toJsonArray(currentRecords)

            sharedPref.edit()
                .putString("sobriety_records", jsonString)
                .apply()

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
    var targetDays by remember { mutableStateOf("30") }
    var actualDays by remember { mutableStateOf("25") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)) }
    var isCompleted by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("실패") }

    // 날짜 포맷터
    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())

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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF2C3E50)
            )
        )

        // 스크롤 가능한 컨텐츠
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
                }
            }

            // 목표 및 달성 일수 설정 카드
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
                        text = "목표 및 달성 일수",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 목표 일수
                    OutlinedTextField(
                        value = targetDays,
                        onValueChange = { targetDays = it },
                        label = { Text("목표 일수") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF74B9FF),
                            focusedLabelColor = Color(0xFF74B9FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 달성 일수
                    OutlinedTextField(
                        value = actualDays,
                        onValueChange = { actualDays = it },
                        label = { Text("달성 일수") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF74B9FF),
                            focusedLabelColor = Color(0xFF74B9FF)
                        )
                    )
                }
            }

            // 완료 상태 설정 카드
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
                        text = "완료 상태",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 완료 여부 체크박스
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = {
                                isCompleted = it
                                status = if (it) "성공" else "실패"
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF00B894)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "목표 달성 완료",
                            fontSize = 14.sp,
                            color = Color(0xFF636E72)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 상태 텍스트 입력
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("상태") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF74B9FF),
                            focusedLabelColor = Color(0xFF74B9FF)
                        )
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
                            val targetDaysInt = targetDays.toIntOrNull() ?: 30
                            val actualDaysInt = actualDays.toIntOrNull() ?: 0

                            val record = SobrietyRecord(
                                id = "test_${System.currentTimeMillis()}",
                                startTime = startDate,
                                endTime = endDate,
                                targetDays = targetDaysInt,
                                actualDays = actualDaysInt,
                                isCompleted = isCompleted,
                                status = status,
                                createdAt = System.currentTimeMillis()
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
