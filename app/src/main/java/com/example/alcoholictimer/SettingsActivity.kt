package com.example.alcoholictimer

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alcoholictimer.utils.Constants

class SettingsActivity : AppCompatActivity() {

    private lateinit var radioGroupTestMode: RadioGroup
    private lateinit var rbRealMode: RadioButton
    private lateinit var rbMinuteMode: RadioButton
    private lateinit var rbSecondMode: RadioButton
    private lateinit var btnSaveSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // UI 요소 초기화
        radioGroupTestMode = findViewById(R.id.radioGroupTestMode)
        rbRealMode = findViewById(R.id.rbRealMode)
        rbMinuteMode = findViewById(R.id.rbMinuteMode)
        rbSecondMode = findViewById(R.id.rbSecondMode)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        // 현재 설정 불러오기 및 UI에 반영
        loadCurrentSettings()

        // 저장 버튼 클릭 이벤트
        btnSaveSettings.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 현재 설정을 불러와 UI에 반영합니다.
     */
    private fun loadCurrentSettings() {
        val preferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val testMode = preferences.getInt(Constants.PREF_KEY_TEST_MODE, Constants.TEST_MODE_SECOND)

        // 라디오 버튼 선택 상태 설정
        when (testMode) {
            Constants.TEST_MODE_REAL -> rbRealMode.isChecked = true
            Constants.TEST_MODE_MINUTE -> rbMinuteMode.isChecked = true
            Constants.TEST_MODE_SECOND -> rbSecondMode.isChecked = true
        }
    }

    /**
     * 사용자 설정을 저장합니다.
     */
    private fun saveSettings() {
        val preferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()

        // 선택된 테스트 모드 저장
        val testMode = when (radioGroupTestMode.checkedRadioButtonId) {
            R.id.rbRealMode -> Constants.TEST_MODE_REAL
            R.id.rbMinuteMode -> Constants.TEST_MODE_MINUTE
            R.id.rbSecondMode -> Constants.TEST_MODE_SECOND
            else -> Constants.TEST_MODE_SECOND // 기본값
        }

        editor.putInt(Constants.PREF_KEY_TEST_MODE, testMode)
        editor.apply()

        // Constants 클래스의 동적 설정 업데이트
        Constants.updateTestMode(testMode)
    }
}
