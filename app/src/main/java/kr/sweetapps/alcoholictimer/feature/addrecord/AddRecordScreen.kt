package kr.sweetapps.alcoholictimer.feature.addrecord

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.data.RecordsDataLoader
import kr.sweetapps.alcoholictimer.core.model.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.layout.boundsInWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreenComposable(
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var targetDays by remember { mutableStateOf("0") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableStateOf(Pair(9, 0)) }
    var endTime by remember { mutableStateOf(Pair(18, 0)) }

    // 인라인 편집: 읽기 모드와 편집 모드 토글
    var isEditingTarget by remember { mutableStateOf(false) }
    var editableTargetText by rememberSaveable { mutableStateOf(targetDays) }
    val targetFocusRequester = remember { FocusRequester() }

    val dateFormat = remember {
        val locale = Locale.getDefault()
        when (locale.language) {
            "ko" -> SimpleDateFormat("yyyy년 MM월 dd일", locale)
            "ja" -> SimpleDateFormat("yyyy年MM月dd日", locale)
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        }
    }

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

    val isRangeInvalid = endMillis <= startMillis
    val isOngoing = endMillis > nowMillis
    val targetDaysInt = targetDays.toIntOrNull() ?: 0
    val isTargetValid = targetDays.isNotBlank() && targetDaysInt in 1..999

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

    // 저장 로직: SharedPreferences에 기존 로직과 동일하게 저장
    fun persistRecord(record: SobrietyRecord): Boolean {
        return try {
            val currentRecords = RecordsDataLoader.loadSobrietyRecords(context).toMutableList()
            val hasTimeConflict = currentRecords.any { existing ->
                val newStart = record.startTime
                val newEnd = record.endTime
                val existingStart = existing.startTime
                val existingEnd = existing.endTime
                (newStart < existingEnd && newEnd > existingStart)
            }
            if (hasTimeConflict) return false
            currentRecords.add(record)
            currentRecords.sortByDescending { it.endTime }
            val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
            val jsonString = SobrietyRecord.toJsonArray(currentRecords)
            sharedPref.edit { putString("sobriety_records", jsonString) }
            true
        } catch (e: Exception) {
            android.util.Log.e("AddRecordComposable", "기록 저장 실패", e)
            false
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // track bounds so we can detect taps outside the field
    var rootBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
    var fieldBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
    var editRequestedAt by remember { mutableStateOf(0L) }

    // when entering edit mode, request focus and show keyboard immediately (use LaunchedEffect to ensure focus is granted)
    LaunchedEffect(isEditingTarget) {
        if (isEditingTarget) {
            // small suspend to ensure the field is composed and focus can be granted
            delay(120)
            targetFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        topBar = {
            kr.sweetapps.alcoholictimer.core.ui.BackTopBar(title = stringResource(R.string.add_record_title), onBack = onCancel)
        },
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords -> rootBounds = coords.boundsInWindow() }
            .pointerInput(Unit) {
                detectTapGestures { localOffset ->
                    val tapGlobal = Offset(rootBounds.left + localOffset.x, rootBounds.top + localOffset.y)
                    val now = System.currentTimeMillis()
                    if (!fieldBounds.contains(tapGlobal) && (now - editRequestedAt >= 300L)) {
                        focusManager.clearFocus()
                        isEditingTarget = false
                    }
                }
            }
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(Color.White)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

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
                        Text(text = "${dateFormat.format(Date(startDate))} - ${String.format(Locale.getDefault(), "%02d:%02d", startTime.first, startTime.second)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()

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
                        Text(text = "${dateFormat.format(Date(endDate))} - ${String.format(Locale.getDefault(), "%02d:%02d", endTime.first, endTime.second)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.add_record_target_days), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))

                        // 인라인 편집 모드: 클릭하면 바로 편집 모드로 전환 (actual focus/keyboard handled in LaunchedEffect)
                        if (!isEditingTarget) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                editableTargetText = targetDays
                                isEditingTarget = true
                                editRequestedAt = System.currentTimeMillis()
                                // request focus synchronously to ensure keyboard appears on first tap
                                targetFocusRequester.requestFocus()
                                keyboardController?.show()
                            }) {
                                Text(text = "$targetDays${stringResource(R.string.add_record_days_unit)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    editableTargetText = targetDays
                                    isEditingTarget = true
                                    editRequestedAt = System.currentTimeMillis()
                                    targetFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.cd_navigate_back))
                                }
                            }
                        } else {
                            val keyboard = keyboardController
                            var targetFieldHadFocus by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = editableTargetText,
                                onValueChange = { new: String ->
                                    val filtered = new.filter { ch -> ch.isDigit() }.take(3)
                                    editableTargetText = filtered
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .width(96.dp)
                                    .focusRequester(targetFocusRequester)
                                    .onGloballyPositioned { coords -> fieldBounds = coords.boundsInWindow() }
                                    .onFocusChanged { state ->
                                        // mark when field actually received focus; only commit on loss if it had focus
                                        if (state.isFocused) {
                                            targetFieldHadFocus = true
                                        } else if (!state.isFocused && targetFieldHadFocus) {
                                            val parsed = editableTargetText.toIntOrNull()
                                            if (parsed != null && parsed in 1..999) {
                                                targetDays = parsed.toString()
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.add_record_error_set_target), Toast.LENGTH_SHORT).show()
                                            }
                                            targetFieldHadFocus = false
                                            isEditingTarget = false
                                            keyboard?.hide()
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    // 확인: 1..999만 허용
                                    val parsed = editableTargetText.toIntOrNull()
                                    if (parsed != null && parsed in 1..999) {
                                        targetDays = parsed.toString()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.add_record_error_set_target), Toast.LENGTH_SHORT).show()
                                    }
                                    isEditingTarget = false
                                    keyboard?.hide()
                                }),
                                // 간단한 스타일과 placeholder
                                shape = MaterialTheme.shapes.medium,
                                placeholder = { Text(text = "목표 일수 입력", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }

                    HorizontalDivider()

                    if (isRangeInvalid) Text(stringResource(R.string.add_record_error_invalid_range), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    if (!isTargetValid) Text(stringResource(R.string.add_record_error_set_target), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_record_cancel)) }

                        Button(
                            onClick = {
                                if (!isRangeInvalid && !isOngoing && isTargetValid) {
                                    val id = "rec_${System.currentTimeMillis()}"
                                    val status = if (!isOngoing && !isRangeInvalid && actualDays >= targetDaysInt) "성공" else "실패"
                                    val record = SobrietyRecord(
                                        id = id,
                                        startTime = startMillis,
                                        endTime = endMillis,
                                        targetDays = targetDaysInt,
                                        actualDays = actualDays,
                                        isCompleted = (!isOngoing && !isRangeInvalid && actualDays >= targetDaysInt),
                                        status = status,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    val ok = persistRecord(record)
                                    if (ok) {
                                        Toast.makeText(context, context.getString(R.string.add_record_toast_success), Toast.LENGTH_SHORT).show()
                                        onFinished()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.add_record_toast_conflict), Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isRangeInvalid && !isOngoing && isTargetValid,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.add_record_save)) }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
