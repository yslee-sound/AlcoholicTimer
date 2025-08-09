package com.example.alcoholictimer

import android.app.Application
import com.example.alcoholictimer.utils.Constants

class AlcoholicApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Constants 초기화 (테스트 모드 설정 복원)
        Constants.init(this)
    }
}
