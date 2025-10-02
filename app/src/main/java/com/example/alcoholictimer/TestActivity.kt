package com.example.alcoholictimer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

/**
 * 개발자용 테스트 도구 화면.
 * 2025-10-02 이후 버전부터 햄버거 메뉴에서 제거되었으며, 일반 사용자 UI에서는 접근할 수 없습니다.
 */
@Deprecated("개발자 도구 화면. 메뉴에서 제거됨(2025-10-02). 필요 시 직접 실행만 허용.")
class TestActivity : BaseActivity() {

    override fun getScreenTitle(): String = "테스트"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                BaseScreen {
                    TestScreen()
                }
            }
        } catch (e: Exception) {
            // 예외 발생 시 로그 남기고 앱 종료 방지
            android.util.Log.e("TestActivity", "초기화 중 오류", e)
            finish()
        }
    }
}

@Composable
fun TestScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상단 제목 (테스트 화면 안내)
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
            Text(
                text = "테스트 도구",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // 구분선
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Black
        )

        // 기록 초기화 버튼
        var showDialog by remember { mutableStateOf(false) }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(
                onClick = {
                    showDialog = true
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                    Text("모든 기록 초기화")
                }
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("기록 초기화") },
                text = { Text("모든 기록을 초기화하시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = {
                        val sharedPref = context.getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                        val beforeRecords = sharedPref.getString("sobriety_records", "[]")
                        android.util.Log.d("TestActivity", "초기화 전 기록: $beforeRecords")
                        // KTX 확장 사용
                        sharedPref.edit {
                            clear()
                            remove("sobriety_records")
                        }
                        val afterRecords = sharedPref.getString("sobriety_records", "[]")
                        android.util.Log.d("TestActivity", "초기화 후 기록: $afterRecords")
                        Toast.makeText(context, "모든 기록이 초기화되었습니다", Toast.LENGTH_SHORT).show()
                        showDialog = false
                    }) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }
        // "모든 설정 초기화" 다이얼로그
        var showSettingsDialog by remember { mutableStateOf(false) }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            OutlinedButton(
                onClick = {
                    showSettingsDialog = true
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                )
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                    Text("모든 설정 초기화")
                }
            }
        }
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("모든 설정 초기화") },
                text = { Text("앱을 설치한 초기 상태로 모든 설정을 되돌리시겠습니까?\n(기록 포함 모든 데이터가 삭제됩니다)") },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            // 1. 모든 SharedPreferences 데이터 완전 삭제
                            val sharedPrefNames = listOf(
                                "user_settings",
                                "test_settings",
                                "app_preferences",
                                "sobriety_records"
                            )

                            // 각 SharedPreferences 파일의 내용을 완전히 삭제
                            sharedPrefNames.forEach { prefName ->
                                val prefs = context.getSharedPreferences(prefName, android.content.Context.MODE_PRIVATE)
                                prefs.edit { clear() }
                            }

                            // 2. SharedPreferences 파일 자체를 물리적으로 삭제
                            val prefsDir = context.filesDir.parentFile?.resolve("shared_prefs")
                            prefsDir?.listFiles()?.forEach { file ->
                                try {
                                    file.delete()
                                } catch (e: Exception) {
                                    android.util.Log.w("TestActivity", "공유 설정 파일 삭제 실패: ${file.name}", e)
                                }
                            }

                            // 3. 앱 내부 저장소의 모든 파일 삭제
                            context.filesDir.listFiles()?.forEach { file ->
                                try {
                                    if (file.isDirectory) {
                                        file.deleteRecursively()
                                    } else {
                                        file.delete()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("TestActivity", "내부 파일 삭제 실패: ${file.name}", e)
                                }
                            }

                            // 4. 캐시 디렉토리 삭제
                            context.cacheDir.listFiles()?.forEach { file ->
                                try {
                                    if (file.isDirectory) {
                                        file.deleteRecursively()
                                    } else {
                                        file.delete()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("TestActivity", "캐시 파일 삭제 실패: ${file.name}", e)
                                }
                            }

                            Toast.makeText(context, "모든 설정과 기록이 완전히 초기화되었습니다.\n앱을 재시작해주세요.", Toast.LENGTH_LONG).show()
                            showSettingsDialog = false

                            // 5. 앱 종료 (사용자가 수동으로 재시작하도록)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                val activity = context as? android.app.Activity
                                // minSdk 21 이상이므로 분기 없이 finishAffinity 사용
                                activity?.finishAffinity()
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 2000) // 2초 후 앱 종료

                        } catch (e: Exception) {
                            Toast.makeText(context, "초기화 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }

        // 하단 공간 확보
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTestScreen() {
    TestScreen()
}
