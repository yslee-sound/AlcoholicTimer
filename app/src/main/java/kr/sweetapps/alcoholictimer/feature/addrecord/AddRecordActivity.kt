package kr.sweetapps.alcoholictimer.feature.addrecord

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.theme.AlcoholicTimerTheme
import kr.sweetapps.alcoholictimer.core.data.RecordsDataLoader
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.feature.addrecord.components.TargetDaysBottomSheet
import kr.sweetapps.alcoholictimer.constants.UiConstants
import kr.sweetapps.alcoholictimer.core.ui.predictAnchoredBannerHeightDp
import kr.sweetapps.alcoholictimer.core.ui.AppBorder
import androidx.core.view.WindowCompat

class AddRecordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No special window flags here; this Activity may be used as a legacy fallback.
        setContent {
            // 앱은 라이트 모드 고정 정책: 다크 모드 진입 방지
            AlcoholicTimerTheme(darkTheme = false) {
                // 전체 화면 배경을 흰색으로 유지
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    AddRecordScreen(
                        onSave = { record ->
                            val success = saveRecord(record)
                            if (success) {
                                // success: no toast per request
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                // conflict: no toast per request
                            }
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    // Activity lifecycle hooks not used for ad control any more

    private fun saveRecord(record: SobrietyRecord): Boolean {
        return try {
            val currentRecords = RecordsDataLoader.loadSobrietyRecords(this).toMutableList()
            // 시간 겹침 방지
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
            Log.e("AddRecord", "기록 저장 실패", e)
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordScreen(
    onSave: (SobrietyRecord) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var targetDays by remember { mutableStateOf("0") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableStateOf(Pair(9, 0)) }
    var endTime by remember { mutableStateOf(Pair(18, 0)) }

    // 목표 일수: 휠 피커 바텀시트 사용
    var showTargetSheet by remember { mutableStateOf(false) }
    var tempTarget by remember(targetDays) { mutableIntStateOf(targetDays.toIntOrNull()?.coerceIn(0, 999) ?: 0) }

    // 다국어 지원 날짜 포맷
    val dateFormat = remember {
        val locale = Locale.getDefault()
        when (locale.language) {
            "ko" -> SimpleDateFormat("yyyy년 MM월 dd일", locale)
            "ja" -> SimpleDateFormat("yyyy年MM月dd日", locale)
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        }
    }

    // 밀리초 계산(선택 날짜 + 시간)
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
    val isOngoing = endMillis > nowMillis
    val targetDaysInt = targetDays.toIntOrNull() ?: 0
    val isTargetValid = targetDays.isNotBlank() && targetDaysInt in 1..999

    // 실제 기간(일)과 완료 여부 계산
    val actualDays = remember(startMillis, endMillis) {
        ((endMillis - startMillis).coerceAtLeast(0L) / (24 * 60 * 60 * 1000L)).toInt()
    }
    val isCompleted = !isOngoing && !isRangeInvalid && isTargetValid && actualDays >= targetDaysInt

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
            kr.sweetapps.alcoholictimer.core.ui.BackTopBar(title = stringResource(R.string.add_record_title), onBack = onCancel)
        },
        // 전체 화면 배경을 흰색으로 고정
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // bottomBar에 배너를 넣어 스캐폴드가 공간을 자동으로 예약하게 함
        bottomBar = {
            // 예측 배너 높이만 사용 (padding은 modifier로 처리)
            val predictedBannerH = predictAnchoredBannerHeightDp()

            Column {
                Spacer(modifier = Modifier.height(UiConstants.BANNER_TOP_GAP))
                HorizontalDivider(thickness = AppBorder.Hairline, color = Color(0xFFE0E0E0))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .height(predictedBannerH),
                    contentAlignment = Alignment.Center
                ) {
                    // AdmobBanner() centralized in MainActivity BaseScaffold; keep placeholder
                }
            }
        },
     ) { innerPadding ->
        // 상단 스크롤 콘텐츠 영역
        Column(modifier = Modifier.fillMaxSize()) {
             // 상단 스크롤 콘텐츠 영역
             Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                 Column(
                     modifier = Modifier
                         .padding(innerPadding)
                         .fillMaxSize()
                         .verticalScroll(rememberScrollState())
                         // 화면 배경 흰색 유지
                         .background(Color.White)
                         .padding(horizontal = 16.dp)
                 ) {
                     Spacer(Modifier.height(8.dp))

                     // 시작일 및 시간
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
                         Text(stringResource(R.string.add_record_start_date_time), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                         Text(
                             text = "${dateFormat.format(Date(startDate))} - ${String.format(Locale.getDefault(), "%02d:%02d", startTime.first, startTime.second)}",
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }

                     HorizontalDivider()

                     // 종료일 및 시간
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
                         Text(stringResource(R.string.add_record_end_date_time), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                         Text(
                             text = "${dateFormat.format(Date(endDate))} - ${String.format(Locale.getDefault(), "%02d:%02d", endTime.first, endTime.second)}",
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }

                     HorizontalDivider()

                     // 목표 일수 (행 형식 + 바텀시트 휠 피커)
                     Row(
                         modifier = Modifier
                             .fillMaxWidth()
                             .heightIn(min = 56.dp)
                             .clickable {
                                 tempTarget = targetDays.toIntOrNull()?.coerceIn(0, 999) ?: 0
                                 showTargetSheet = true
                             },
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Text(stringResource(R.string.add_record_target_days), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                         Text(
                             text = "$targetDays${stringResource(R.string.add_record_days_unit)}",
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }

                     HorizontalDivider()

                     // 경고/안내
                     if (isRangeInvalid) {
                         Text(stringResource(R.string.add_record_error_invalid_range), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                     }
                     if (!isTargetValid) {
                         Text(stringResource(R.string.add_record_error_set_target), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                     }

                     Spacer(Modifier.height(16.dp))

                     // 저장/취소
                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                         OutlinedButton(
                             onClick = onCancel,
                             modifier = Modifier.weight(1f)
                         ) { Text(stringResource(R.string.add_record_cancel)) }

                         Button(
                             onClick = {
                                 if (!isRangeInvalid && !isOngoing && isTargetValid) {
                                     val id = "rec_${System.currentTimeMillis()}"
                                     val status = if (isCompleted) "성공" else "실패"
                                     val record = SobrietyRecord(
                                         id = id,
                                         startTime = startMillis,
                                         endTime = endMillis,
                                         targetDays = targetDaysInt,
                                         actualDays = actualDays,
                                         isCompleted = isCompleted,
                                         status = status,
                                         createdAt = System.currentTimeMillis()
                                     )
                                     onSave(record)
                                 }
                             },
                             enabled = !isRangeInvalid && !isOngoing && isTargetValid,
                             modifier = Modifier.weight(1f)
                         ) { Text(stringResource(R.string.add_record_save)) }
                     }

                     Spacer(Modifier.height(24.dp))
                 }

                 if (showTargetSheet) {
                     // 바텀시트는 핵심 대화형 요소이므로 흰색 Surface 유지
                     TargetDaysBottomSheet(
                         initialValue = tempTarget,
                         onConfirm = { picked: Int ->
                             targetDays = picked.toString()
                             showTargetSheet = false
                         },
                         onDismiss = { showTargetSheet = false }
                     )
                 }
             }
        }
  }
}
