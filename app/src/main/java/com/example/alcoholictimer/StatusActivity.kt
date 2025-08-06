package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alcoholictimer.models.RecentActivity
import com.example.alcoholictimer.utils.Constants
import com.example.alcoholictimer.utils.RecordManager
import com.example.alcoholictimer.utils.RecentActivityManager
import com.example.alcoholictimer.utils.SobrietyRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector

class StatusActivity : BaseActivity() {
    // 이전에 목표 달성 여부를 확인했는지 체크하는 플래그
    private var goalAchievementChecked = false

    private val levelTitles = listOf(
        "새싹 도전자",
        "첫걸음 성공",
        "의지의 시작",
        "한달의 기적",
        "습관의 탄생",
        "의지의 달인",
        "금주의 마스터",
        "절제의 달인"
    )

    // UI 업데이트를 위한 타이머 및 핸들러
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    // 금주 시작 시간 저장 변수
    private var abstainStartTime: String = ""

    // Compose State 변수들
    private var timePassed by mutableIntStateOf(0)
    private var hoursDisplay by mutableStateOf("")
    private var timeDetail by mutableStateOf("")
    private var progressValue by mutableFloatStateOf(0f)
    private var statusMessage by mutableStateOf("")
    private var timeUnitText by mutableStateOf("일")

    override fun getScreenTitle(): String = "금주 상태"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen {
                StatusScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // SharedPreferences에서 현재 테스트 모드를 읽어와서 Constants 업데이트
        val sharedPref = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val currentTestMode = sharedPref.getInt(Constants.PREF_TEST_MODE, Constants.TEST_MODE_REAL)
        Constants.updateTestMode(currentTestMode)

        updateTimeModeDisplay()  // 모드 변경사항 업데이트

        // 금주 시작 시간 초기화 (abstainStartTime이 비어있는 경우를 대비)
        initAbstainStartTime()

        updateUI()
        startTimer()

        // 목표 달성 플래그 초기화
        goalAchievementChecked = false
    }

    override fun onPause() {
        super.onPause()
        // 화면이 보이지 않을 때 타이머 정지
        stopTimer()
    }

    // Activity 클래스의 onNewIntent를 오버라이드
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateTimeModeDisplay()  // 모드 변경사항 업데이트
        updateUI()  // UI 즉시 갱신
    }

    private fun startTimer() {
        stopTimer()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    updateTimeDisplay()
                }
            }
        }, 0, 100)  // 100ms 간격으로 업데이트
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun updateTimeDisplay() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val completionFlag = sharedPref.getBoolean("timer_completed", false)

        // 목표가 이미 달성되었으면 타이머 업데이트를 중단
        if (completionFlag) {
            stopTimer()
            return
        }

        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val currentTime = System.currentTimeMillis()
        val secondsPassed = (currentTime - startTime) / 1000L

        // 테스트 모드에 따른 시간 계산 - 대형 숫자 업데이트
        timePassed = when {
            Constants.isSecondTestMode -> secondsPassed.toInt() + 1  // 초 단위
            Constants.isMinuteTestMode -> (secondsPassed / 60).toInt() + 1  // 분 단위
            else -> ((currentTime - startTime) / Constants.TIME_UNIT_MILLIS).toInt() + 1  // 일 단위
        }

        // 시간 계산
        val hours = (secondsPassed / 3600) % 24

        // 시간 표시 업데이트 - Compose State 사용
        hoursDisplay = String.format(Locale.getDefault(), "%02d시간", hours)

        // 테스트 모드별 시간 표시 업데이트
        timeDetail = when {
            Constants.isSecondTestMode -> {
                val totalSeconds = secondsPassed
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            }
            Constants.isMinuteTestMode -> {
                val totalSeconds = secondsPassed
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            }
            else -> {
                val totalSeconds = secondsPassed
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            }
        }

        // 진행 상태 확인 및 완료 처리
        checkProgressStatus()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun StatusScreen() {
        var showStopDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "금주 진행 상황",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 대형 숫자 표시
            Text(
                text = timePassed.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 시간 단위 표시
            Text(
                text = timeUnitText,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 시간 표시
            Text(
                text = hoursDisplay,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 상세 시간 표시
            Text(
                text = timeDetail,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 진행률 표시
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(bottom = 16.dp)
            )

            // 상태 메시지
            Text(
                text = statusMessage,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 중지 버튼
            Button(
                onClick = { showStopDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("금주 중지", fontSize = 16.sp)
            }
        }

        // 중지 확인 다이얼로그
        if (showStopDialog) {
            AlertDialog(
                onDismissRequest = { showStopDialog = false },
                title = { Text("금주 중지", fontWeight = FontWeight.Bold) },
                text = { Text("정말 금주를 중지하시겠습니까?\n모든 금주 기록이 초기화됩니다.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showStopDialog = false
                            handleStopSobriety()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    Button(onClick = { showStopDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }
    }

    private fun handleMenuSelection(menuItem: String) {
        when (menuItem) {
            "금주" -> {
                // 현재 화면이므로 아무 작업 안함
            }
            "활동 보기" -> {
                val intent = Intent(this, RecordsActivity::class.java)
                startActivity(intent)
            }
            "설정" -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun handleStopSobriety() {
        // 중단된 활동 기록 저장
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val endTime = System.currentTimeMillis()

        // RecentActivityManager 초기화 및 중단된 활동 저장
        RecentActivityManager.init(this@StatusActivity)
        RecentActivityManager.saveStoppedActivity(startTime, endTime, Constants.currentTestMode)

        // SharedPreferences 초기화
        sharedPref.edit().clear().apply()

        // 시작 화면으로 이동
        val intent = Intent(this@StatusActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()

        // 토스트 메시지 표시
        Toast.makeText(this@StatusActivity, "금주가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * UI를 업데이트하는 메서드 - 타이머에 의해 주기적으로 호출됨
     */
    private fun updateUI() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val targetDays = sharedPref.getInt("target_days", 30)
        val completionFlag = sharedPref.getBoolean("timer_completed", false)

        if (completionFlag) {
            timePassed = targetDays
            statusMessage = "축하합니다! ${targetDays}${Constants.TIME_UNIT_TEXT} 목표를 달성했습니다!"
            progressValue = 1.0f
            return
        }

        val currentTime = System.currentTimeMillis()
        val secondsPassed = (currentTime - startTime) / 1000L

        // 테스트 모드에 따른 시간 계산
        val currentTimePassed = when {
            Constants.isSecondTestMode -> secondsPassed.toInt() + 1  // 초 단위
            Constants.isMinuteTestMode -> (secondsPassed / 60).toInt() + 1  // 분 단위
            else -> ((currentTime - startTime) / Constants.TIME_UNIT_MILLIS).toInt() + 1  // 일 단위
        }

        // 대형 숫자에 진행 중인 시간 표시
        timePassed = currentTimePassed

        // 진행률 계산
        val targetSeconds = when {
            Constants.isSecondTestMode -> targetDays.toLong()
            Constants.isMinuteTestMode -> targetDays.toLong() * 60
            else -> targetDays.toLong() * 24 * 60 * 60
        }

        val progress = ((secondsPassed.toFloat() / targetSeconds.toFloat()) * 100).toInt()
        progressValue = (progress.coerceIn(0, 100) / 100f)

        // 타이머 표시 업데이트
        updateTimeDisplay()

        // 완료 조건 확인 - 초단위 테스트 모드에서는 초 단위로 비교
        val isCompleted = when {
            Constants.isSecondTestMode -> secondsPassed >= targetDays
            Constants.isMinuteTestMode -> secondsPassed >= (targetDays * 60)
            else -> currentTimePassed > targetDays
        }

        if (isCompleted) {
            timePassed = targetDays
            progressValue = 1.0f
            handleGoalCompletion(targetDays)
        } else {
            // 남은 시간 계산 및 메시지 업데이트
            val remainingTime = targetDays - currentTimePassed + 1
            statusMessage = "남은 시간: ${remainingTime}${Constants.TIME_UNIT_TEXT}"
        }
    }

    private fun handleGoalCompletion(targetDays: Int) {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
        val endTime = System.currentTimeMillis()

        sharedPref.edit().putBoolean("timer_completed", true).apply()

        if (!goalAchievementChecked) {
            goalAchievementChecked = true
            // 타이머 즉시 중지
            stopTimer()

            // RecentActivityManager 초기화
            RecentActivityManager.init(this)

            // 완료된 활동을 RecentActivityManager에 저장
            RecentActivityManager.saveCompletedActivity(
                startTime,
                endTime,
                targetDays,
                Constants.currentTestMode
            )

            // 완료된 기록을 먼저 저장
            val recordId = saveCompletedRecord(startTime, endTime, targetDays, 1)

            // 결과 화면 전환 지연 후 기록 요약 화면으로 이동
            Handler(Looper.getMainLooper()).postDelayed({
                // 기록 요약 화면으로 이동 (기록 ID 전달)
                navigateToRecordSummary(recordId)
            }, Constants.RESULT_SCREEN_DELAY.toLong())
        }
    }

    /**
     * 완료된 금주 기록을 저장합니다
     * @return 저장된 기록의 ID
     */
    private fun saveCompletedRecord(startTime: Long, endTime: Long, targetDays: Int, level: Int): Long {
        Log.d("StatusActivity", "saveCompletedRecord 시작: startTime=$startTime, endTime=$endTime, targetDays=$targetDays")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startDate = dateFormat.format(Date(startTime))
        val endDate = dateFormat.format(Date(endTime))

        // 기록 ID 생성
        val recordId = System.currentTimeMillis()
        Log.d("StatusActivity", "생성된 recordId: $recordId")

        // 테스트 모드일 때는 실제 경과 시간 대신 설정한 목표 시간을 그대로 사용
        val achievedDays = if (Constants.isSecondTestMode || Constants.isMinuteTestMode) {
            // 테스트 모드에서는 설정한 목표 시간 그대로 표시
            targetDays
        } else {
            // 실제 모드에서만 실제 경과 시간 계산
            ((endTime - startTime) / Constants.TIME_UNIT_MILLIS).toInt()
        }

        Log.d("StatusActivity", "테스트 모드: ${Constants.isSecondTestMode}, 설정된 일수: $targetDays, 기록될 일수: $achievedDays")

        // 기록 객체 생성
        val record = SobrietyRecord(
            id = recordId,
            startDate = startDate,
            endDate = endDate,
            duration = targetDays,
            achievedDays = achievedDays, // 목표 시간을 그대로 사용
            achievedLevel = level,
            levelTitle = if (level > 0 && level <= levelTitles.size) levelTitles[level - 1] else "기본 레벨",
            isCompleted = true
        )

        Log.d("StatusActivity", "생성된 기록: $record")

        // 기존 기록 불러오기
        val sharedPref = getSharedPreferences("sobriety_records", MODE_PRIVATE)
        val recordsJson = sharedPref.getString("records", "[]")
        Log.d("StatusActivity", "기존 기록 JSON: $recordsJson")

        val records = SobrietyRecord.fromJsonArray(recordsJson ?: "[]").toMutableList()
        Log.d("StatusActivity", "기존 기록 개수: ${records.size}")

        // 새 기록 추가
        records.add(record)
        Log.d("StatusActivity", "새 기록 추가 후 총 개수: ${records.size}")

        // 기록 저장
        val newRecordsJson = SobrietyRecord.toJsonArray(records)
        Log.d("StatusActivity", "저장할 JSON: $newRecordsJson")

        sharedPref.edit().putString("records", newRecordsJson).commit()

        // 저장 확인
        val savedRecordsJson = sharedPref.getString("records", "[]")
        Log.d("StatusActivity", "저장 확인 JSON: $savedRecordsJson")

        // 현재 진행중인 금주 데이터 초기화
        getSharedPreferences("user_settings", MODE_PRIVATE).edit().clear().apply()

        Log.d("StatusActivity", "saveCompletedRecord 완료, 반환 ID: $recordId")
        return recordId
    }

    private fun saveActivity(isCompleted: Boolean) {
        try {
            val activity = RecentActivity(
                startDate = abstainStartTime,
                endDate = getCurrentDate(),
                duration = calculateDuration(),
                isSuccess = isCompleted
            )
            RecordManager.addActivity(activity)
        } catch (e: Exception) {
            // 날짜 파싱 오류 등이 발생하면 로그만 남기고 기본값 사용
            Log.e("StatusActivity", "Error saving activity: ${e.message}", e)
            try {
                val activity = RecentActivity(
                    startDate = getCurrentDate(),
                    endDate = getCurrentDate(),
                    duration = 1,
                    isSuccess = isCompleted
                )
                RecordManager.addActivity(activity)
            } catch (fallbackError: Exception) {
                Log.e("StatusActivity", "Fallback save also failed: ${fallbackError.message}", fallbackError)
            }
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun calculateDuration(): Int {
        return try {
            if (abstainStartTime.isBlank()) {
                // abstainStartTime이 비어있으면 SharedPreferences에서 시작 시간을 가져와서 계산
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                val startTimeMillis = sharedPref.getLong("start_time", System.currentTimeMillis())
                val currentTime = System.currentTimeMillis()
                val daysPassed = ((currentTime - startTimeMillis) / (1000 * 60 * 60 * 24)).toInt()
                return daysPassed + 1
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = dateFormat.parse(abstainStartTime)
            val currentDate = Date()

            if (startDate != null) {
                ((currentDate.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
            } else {
                1 // 기본값
            }
        } catch (e: Exception) {
            Log.e("StatusActivity", "Error calculating duration: ${e.message}", e)
            1 // 오류 발생 시 기본값 반환
        }
    }

    private fun navigateToRecordSummary(recordId: Long) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("record_id", recordId)
        startActivity(intent)
        finish()
    }

    private fun updateTimeModeDisplay() {
        timeUnitText = when {
            Constants.isSecondTestMode -> "초"
            Constants.isMinuteTestMode -> "분"
            else -> "일"
        }
    }

    /**
     * 진행 상태를 확인하고 필요한 경우 목표 완료 처리를 합니다.
     * 타이머에서 주기적으로 호출됩니다.
     */
    private fun checkProgressStatus() {
        try {
            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            val startTime = sharedPref.getLong("start_time", System.currentTimeMillis())
            val targetDays = sharedPref.getInt("target_days", 30)
            val completionFlag = sharedPref.getBoolean("timer_completed", false)

            if (completionFlag) {
                return  // 이미 완료된 상태면 처리하지 않음
            }

            val currentTime = System.currentTimeMillis()
            val secondsPassed = (currentTime - startTime) / 1000L

            // 진행률 계산
            val targetSeconds = when {
                Constants.isSecondTestMode -> targetDays.toLong()
                Constants.isMinuteTestMode -> targetDays.toLong() * 60
                else -> targetDays.toLong() * 24 * 60 * 60
            }

            // 진행률이 100을 초과하지 않도록 제한
            val progress = ((secondsPassed.toFloat() / targetSeconds.toFloat()) * 100).toInt()
            val safeProgress = progress.coerceIn(0, 100)

            // UI 업데이트는 runOnUiThread 내에서 수행
            runOnUiThread {
                progressValue = safeProgress / 100f

                // 완료 조건 확인
                val isCompleted = when {
                    Constants.isSecondTestMode -> secondsPassed >= targetDays
                    Constants.isMinuteTestMode -> secondsPassed >= (targetDays * 60)
                    else -> secondsPassed >= (targetDays * 24 * 60 * 60)
                }

                if (isCompleted && !goalAchievementChecked) {
                    timePassed = targetDays
                    progressValue = 1.0f
                    handleGoalCompletion(targetDays)
                } else if (!isCompleted) {
                    // 남은 시간 계산 및 메시지 업데이트
                    val currentTimePassed = when {
                        Constants.isSecondTestMode -> secondsPassed.toInt() + 1  // 초 단위
                        Constants.isMinuteTestMode -> (secondsPassed / 60).toInt() + 1  // 분 단위
                        else -> ((currentTime - startTime) / Constants.TIME_UNIT_MILLIS).toInt() + 1  // 일 단위
                    }
                    val remainingTime = targetDays - currentTimePassed + 1
                    if (remainingTime > 0) {
                        statusMessage = "남은 시간: ${remainingTime}${Constants.TIME_UNIT_TEXT}"
                    } else {
                        statusMessage = "목표 달성이 임박했습니다!"
                    }
                }
            }
        } catch (e: Exception) {
            // 에러 발생 시 로그만 남기고 앱은 계속 실행
            Log.e("StatusActivity", "Error in checkProgressStatus: ${e.message}", e)
        }
    }

    /**
     * 금주 시작 시간을 초기화합니다.
     */
    private fun initAbstainStartTime() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTimeMillis = sharedPref.getLong("start_time", System.currentTimeMillis())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        abstainStartTime = dateFormat.format(Date(startTimeMillis))
    }
}

// 프리뷰 코드
@Preview(showBackground = true)
@Composable
fun StatusScreenPreview() {
    MaterialTheme {
        StatusScreenContent(
            timePassed = 15,
            timeUnitText = "일",
            hoursDisplay = "08시간",
            timeDetail = "08:23:45",
            progressValue = 0.5f,
            statusMessage = "남은 시간: 15일",
            onStopClick = {},
            onMenuClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DrawerMenuPreview() {
    MaterialTheme {
        ModalDrawerSheet(
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                Text(
                    text = "메뉴",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                DrawerMenuItemPreview(
                    title = "금주",
                    icon = Icons.Default.Home,
                    onClick = {}
                )

                DrawerMenuItemPreview(
                    title = "활동 보기",
                    icon = Icons.Default.Menu,
                    onClick = {}
                )

                DrawerMenuItemPreview(
                    title = "설정",
                    icon = Icons.Default.Settings,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun DrawerMenuItemPreview(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = title,
            fontSize = 18.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreenContent(
    timePassed: Int,
    timeUnitText: String,
    hoursDisplay: String,
    timeDetail: String,
    progressValue: Float,
    statusMessage: String,
    onStopClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("금주 상태", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "메뉴 열기",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "금주 진행 상황",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 대형 숫자 표시
            Text(
                text = timePassed.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 시간 단위 표시
            Text(
                text = timeUnitText,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 시간 표시
            Text(
                text = hoursDisplay,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 상세 시간 표시
            Text(
                text = timeDetail,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 진행률 표시
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(bottom = 16.dp)
            )

            // 상태 메시지
            Text(
                text = statusMessage,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 중지 버튼
            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("금주 중지", fontSize = 16.sp)
            }
        }
    }
}
