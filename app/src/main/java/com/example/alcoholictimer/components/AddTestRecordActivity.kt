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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alcoholictimer.ui.theme.AlcoholicTimerTheme
import com.example.alcoholictimer.utils.RecordsDataLoader
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

class AddTestRecordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlcoholicTimerTheme {
                // 전역 테마에서 시스템 바를 설정하므로, 여기서는 개별 설정을 하지 않습니다.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    AddTestRecordScreen(
                        onSave = { record ->
                            val success = saveTestRecord(record)
                            if (success) {
                                Toast.makeText(this, "금주 기록이 추가되었습니다", Toast.LENGTH_SHORT).show()
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                Toast.makeText(this, "선택한 시간이 기존 기록과 중복됩니다", Toast.LENGTH_LONG).show()
                            }
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun saveTestRecord(record: SobrietyRecord): Boolean {
        return try {
            val currentRecords = RecordsDataLoader.loadSobrietyRecords(this).toMutableList()
            val hasTimeConflict = currentRecords.any { existingRecord ->
                val newStart = record.startTime
                val newEnd = record.endTime
                val existingStart = existingRecord.startTime
                val existingEnd = existingRecord.endTime
                (newStart < existingEnd && newEnd > existingStart)
            }
            if (hasTimeConflict) return false
            currentRecords.add(record)
            currentRecords.sortByDescending { it.endTime }
            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            val jsonString = SobrietyRecord.toJsonArray(currentRecords)
            sharedPref.edit { putString("sobriety_records", jsonString) }
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
    // 메모 제거
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableStateOf(Pair(9, 0)) }
    var endTime by remember { mutableStateOf(Pair(18, 0)) }

    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())

    // 현재, 시작/종료 밀리초 계산
    val startMillis by remember(startDate, startTime) {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, startTime.first)
                set(Calendar.MINUTE, startTime.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    val endMillis by remember(endDate, endTime) {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                timeInMillis = endDate
                set(Calendar.HOUR_OF_DAY, endTime.first)
                set(Calendar.MINUTE, endTime.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    val nowMillis = System.currentTimeMillis()

    // 유효성 체크
    val isRangeInvalid = endMillis <= startMillis
    val isOngoing = endMillis > nowMillis // 진행중 정의: 종료가 현재 이후
    val targetDaysInt = targetDays.toIntOrNull() ?: 0
    val isTargetValid = targetDays.isNotBlank() && targetDaysInt > 0

    // 실제 기간 및 완료 여부 계산
    val actualDays = remember(startMillis, endMillis) {
        (endMillis - startMillis).coerceAtLeast(0L) / (24 * 60 * 60 * 1000.0)
    }
    val isCompleted = !isOngoing && !isRangeInvalid && isTargetValid && actualDays >= targetDaysInt

    // 날짜/시간 선택기(날짜 먼저, 이후 시간)
    fun pickDateThenTime(initialDateMillis: Long, initialHour: Int, initialMinute: Int, onPicked: (dateMillis: Long, hour: Int, minute: Int) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val pickedCal = Calendar.getInstance().apply { set(y, m, d) }
                TimePickerDialog(
                    context,
                    { _, h, min -> onPicked(pickedCal.timeInMillis, h, min) },
                    initialHour,
                    initialMinute,
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("금주 기록 추가", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // 시작일 및 시간 (목록형)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable {
                        pickDateThenTime(startDate, startTime.first, startTime.second) { newDate, h, m ->
                            startDate = newDate
                            startTime = h to m
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("시작일 및 시간", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(
                    text = "${dateFormat.format(Date(startDate))} ${String.format(Locale.getDefault(), "%02d:%02d", startTime.first, startTime.second)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()

            // 종료일 및 시간 (항상 활성)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable {
                        pickDateThenTime(endDate, endTime.first, endTime.second) { newDate, h, m ->
                            endDate = newDate
                            endTime = h to m
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("종료일 및 시간", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(
                    text = "${dateFormat.format(Date(endDate))} ${String.format(Locale.getDefault(), "%02d:%02d", endTime.first, endTime.second)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 유효성 안내
            if (isRangeInvalid) {
                Text(
                    text = "시작 시간보다 종료 시간이 빠릅니다.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (isOngoing) {
                Text(
                    text = "종료 시간이 현재 시간 이후입니다. 진행 중 기록은 저장할 수 없어요.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))

            // 목표 일수 (정수)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("목표 일수", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                TextField(
                    value = targetDays,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*$"))) targetDays = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.widthIn(min = 72.dp).width(96.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            if (!isTargetValid) {
                Text(
                    text = "목표 일수는 1 이상의 정수만 입력 가능합니다.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HorizontalDivider()

            // 보조 텍스트: 실제 기간
            Spacer(Modifier.height(12.dp))
            Text(
                text = "실제 기간: ${String.format(Locale.getDefault(), "%.2f", actualDays)}일",
                color = if (isCompleted) Color(0xFF00B894) else Color(0xFFE17055),
                fontSize = 13.sp
            )

            // 하단 여백 확보
            Spacer(Modifier.height(24.dp))

            val canSave = !isRangeInvalid && !isOngoing && isTargetValid

            // 저장 버튼(전체 폭)
            Button(
                onClick = {
                    try {
                        val defaultGoalDays = 30.0
                        val computedPercentage = if (targetDaysInt > 0) {
                            com.example.alcoholictimer.utils.PercentUtils.roundPercent((actualDays / targetDaysInt) * 100.0)
                        } else {
                            com.example.alcoholictimer.utils.PercentUtils.roundPercent((actualDays / defaultGoalDays) * 100.0)
                        }
                        val isCompletedFinal = actualDays >= targetDaysInt
                        val statusFinal = if (isCompletedFinal) "성공" else "실패"
                        val record = SobrietyRecord(
                            id = "test_${System.currentTimeMillis()}",
                            startTime = startMillis,
                            endTime = endMillis,
                            targetDays = targetDaysInt,
                            actualDays = actualDays.toInt(),
                            isCompleted = isCompletedFinal,
                            status = statusFinal,
                            createdAt = System.currentTimeMillis(),
                            percentage = computedPercentage,
                            isTestRecord = true,
                            memo = null
                        )
                        onSave(record)
                    } catch (e: Exception) {
                        Log.e("AddTestRecord", "기록 생성 실패", e)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("저장", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Preview
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddTestRecordScreenPreview() {
    AlcoholicTimerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            AddTestRecordScreen(onSave = { }, onCancel = { })
        }
    }
}
