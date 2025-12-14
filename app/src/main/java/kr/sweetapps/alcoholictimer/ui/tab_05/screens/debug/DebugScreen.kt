package kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug

import android.widget.Toast
import android.util.Log
import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.ui.components.BackTopBar
import androidx.compose.ui.platform.LocalContext
import kr.sweetapps.alcoholictimer.ui.ad.AppOpenAdManager
import kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel.DebugScreenViewModel
import kr.sweetapps.alcoholictimer.util.constants.Constants

// Helper: get Activity from Context
private fun ContextToActivity(context: android.content.Context): Activity? {
    var ctx: android.content.Context? = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun DebugScreen(
    viewModel: DebugScreenViewModel = viewModel(),
    tab05ViewModel: kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel.Tab05ViewModel = viewModel(), // [NEW] Tab05ViewModel 추가
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column {
        BackTopBar(
            title = stringResource(id = R.string.debug_menu_title),
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()) // [NEW] Enable scrolling
                .padding(16.dp)
        ) {
            // [REMOVED] 맞춤형 광고 재설정 - 유럽 지역 배포 제외로 인해 불필요

            DebugSwitch(title = "기능 1", checked = uiState.switch1, onCheckedChange = { viewModel.setSwitch(1, it) })
            DebugSwitch(title = "데모 모드", checked = uiState.demoMode, onCheckedChange = { viewModel.setSwitch(2, it) })

            // [NEW] Time acceleration settings (1x ~ 10,000x) - Linear scale
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "시간 배속 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val acceleration = remember {
                val currentFactor = Constants.getTimeAcceleration(context).toFloat()
                // [FIX] 선형 스케일 사용 (로그 스케일 제거)
                mutableStateOf(currentFactor.coerceIn(1f, 10000f))
            }

            // [FIX] 선형 값을 그대로 사용
            val actualFactor = acceleration.value.toInt().coerceIn(1, 10000)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("현재 배속:", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    text = "${actualFactor}x",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (actualFactor == 1) {
                        androidx.compose.ui.graphics.Color.Gray
                    } else {
                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                    }
                )
            }

            androidx.compose.material3.Slider(
                value = acceleration.value,
                onValueChange = { newValue ->
                    acceleration.value = newValue
                },
                onValueChangeFinished = {
                    val factor = acceleration.value.toInt().coerceIn(1, 10000)
                    Constants.setTimeAcceleration(context, factor)

                    val message = when {
                        factor == 1 -> "정상 속도 (1x)"
                        factor < 100 -> "시간 배속: ${factor}x"
                        factor < 1000 -> "고속: ${factor}x ⚡"
                        else -> "극한: ${factor}x 🚀"
                    }

                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                valueRange = 1f..10000f, // [FIX] 선형 범위 (1 ~ 10,000)
                steps = 9999, // [FIX] 1씩 증가하는 정밀한 제어
                modifier = Modifier.fillMaxWidth()
            )

            // Min/Max labels only (aligned to slider edges)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1x", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Gray)
                Text("10,000x", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }

            Text(
                text = "※ 슬라이더를 드래그하여 1배속 ~ 10,000배속 범위에서 조절 (선형)",
                fontSize = 11.sp,
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "※ 실제 시간은 변경되지 않으며, 경과 시간 계산만 배속됩니다.",
                fontSize = 11.sp,
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )

            // [SECURITY] 릴리즈 빌드 경고
            if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                Text(
                    text = "⚠️ 릴리즈 빌드에서는 배속 기능이 비활성화됩니다.",
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            // [NEW] 전면 광고 쿨타임 설정 (초 단위) - 한 줄 레이아웃 + 스위치 제어
            if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
                // 초기 상태 로드
                val coolDownValue = remember {
                    mutableStateOf(
                        viewModel.getDebugAdCoolDown(context).let {
                            if (it >= 0) it.toString() else "1"
                        }
                    )
                }

                // 스위치 상태 (SharedPreferences에서 로드)
                val isCoolDownEnabled = remember {
                    mutableStateOf(
                        context.getSharedPreferences("ad_policy_prefs", android.content.Context.MODE_PRIVATE)
                            .getBoolean("debug_cooldown_enabled", false)
                    )
                }

                // 한 줄 레이아웃 (Row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. 라벨 (좌측, 남은 공간 차지)
                    Text(
                        text = "전면 광고 쿨타임 (초)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // 2. 입력창 (좁은 너비, 중앙 정렬)
                    OutlinedTextField(
                        value = coolDownValue.value,
                        onValueChange = { newValue ->
                            // 숫자만 입력 가능
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                coolDownValue.value = newValue
                                // 스위치가 켜져 있고 값이 비어있지 않으면 즉시 저장
                                if (isCoolDownEnabled.value && newValue.isNotEmpty()) {
                                    val seconds = newValue.toLongOrNull() ?: 1L
                                    viewModel.setDebugAdCoolDown(context, seconds)
                                    Log.d("DebugScreen", "전면 광고 쿨타임 설정: $seconds 초")
                                }
                            }
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .padding(horizontal = 8.dp),
                        enabled = isCoolDownEnabled.value, // 스위치로 제어
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            disabledTextColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
                            disabledBorderColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)
                        )
                    )

                    // 3. 스위치 (우측 끝)
                    androidx.compose.material3.Switch(
                        checked = isCoolDownEnabled.value,
                        onCheckedChange = { isChecked ->
                            isCoolDownEnabled.value = isChecked

                            // 상태 저장
                            context.getSharedPreferences("ad_policy_prefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("debug_cooldown_enabled", isChecked)
                                .apply()

                            Log.d("DebugScreen", "전면 광고 쿨타임 스위치: ${if (isChecked) "ON (테스트 모드)" else "OFF (기본 모드)"}")

                            // 켤 때 현재 입력값 저장
                            if (isChecked && coolDownValue.value.isNotEmpty()) {
                                val seconds = coolDownValue.value.toLongOrNull() ?: 1L
                                viewModel.setDebugAdCoolDown(context, seconds)
                            }
                            // 끌 때는 기본값 복원 (제거)
                            else if (!isChecked) {
                                viewModel.setDebugAdCoolDown(context, -1L) // 기본값으로 복원
                            }
                        }
                    )
                }

                // 설명 텍스트 (별도 줄)
                Text(
                    text = if (isCoolDownEnabled.value) {
                        "ON: ${coolDownValue.value}초 쿨타임 적용 (테스트 모드)"
                    } else {
                        "OFF: 기본 쿨타임 적용 (디버그: 1분, 릴리즈: 30분)"
                    },
                    fontSize = 12.sp,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // [REMOVED] UMP EEA 강제 - 유럽 지역 배포 제외로 인해 불필요
            DebugSwitch(title = "Analytics 이벤트 전송", checked = uiState.switch3, onCheckedChange = {
                viewModel.setSwitch(3, it)
                // trigger analytics test event when toggled on
                if (it) {
                    viewModel.performAction(3)
                    Toast.makeText(context, "Analytics event sent (debug)", Toast.LENGTH_SHORT).show()
                }
            })
            DebugSwitch(title = "Crashlytics 비치명 보고", checked = uiState.switch4, onCheckedChange = {
                viewModel.setSwitch(4, it)
                if (it) {
                    viewModel.performAction(4)
                    Toast.makeText(context, "Crashlytics non-fatal sent (debug)", Toast.LENGTH_SHORT).show()
                }
            })
            DebugSwitch(title = "Performance trace 실행", checked = uiState.switch5, onCheckedChange = {
                viewModel.setSwitch(5, it)
                if (it) {
                    viewModel.performAction(5)
                    Toast.makeText(context, "Performance trace started (debug)", Toast.LENGTH_SHORT).show()
                }
            })

            // [NEW] 랜덤 데이터 생성 섹션
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "🎲 테스트 데이터 생성",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 랜덤 데이터 생성 버튼
            androidx.compose.material3.Button(
                onClick = {
                    tab05ViewModel.generateRandomMockData(context)
                    Toast.makeText(
                        context,
                        "4년치 랜덤 데이터 생성 완료!\n(기록 화면에서 확인)",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )
            ) {
                Text("🎲 랜덤 과거 데이터 생성 (4년치)", color = androidx.compose.ui.graphics.Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 모든 기록 삭제 버튼
            androidx.compose.material3.Button(
                onClick = {
                    tab05ViewModel.clearAllRecords(context)
                    Toast.makeText(
                        context,
                        "모든 기록 삭제 완료!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF44336)
                )
            ) {
                Text("🗑️ 모든 기록 삭제", color = androidx.compose.ui.graphics.Color.White)
            }

            Text(
                text = """
                    ※ 생성 데이터: 4년 전 ~ 1년 전까지 무작위 기록
                    ※ 연도당 2~3개, 지속 기간 3~50일 랜덤
                    ※ 성공률 70%, 완료/실패 상태 포함
                    
                    ⚠️ 데이터는 완전 랜덤으로 생성됩니다.
                    아래 예상 통계는 어디까지나 "대략적인 참고값"이며,
                    실제 생성된 값과는 크게 다를 수 있습니다.
                    
                    📊 예상 통계 범위 (저/주1회이하/짧음 기준):
                    • 총 금주 일수: 약 150~300일
                    • 줄인 칼로리: 약 4,300~8,500 kcal
                    • 참아낸 술: 약 21~43병
                    • 절약한 금액: 약 ₩210,000~₩430,000
                    • 절약한 시간: 약 32~64시간
                    • 기대 수명+: 약 5~10일
                    
                    → 탭2(기록)에서 실제 통계를 확인하세요.
                """.trimIndent(),
                fontSize = 11.sp,
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            // [NEW] Bottom spacer for breathing room
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun DebugSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
