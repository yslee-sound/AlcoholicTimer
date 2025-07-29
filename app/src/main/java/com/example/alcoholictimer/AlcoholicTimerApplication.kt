package com.example.alcoholictimer

import android.app.Application
import com.example.alcoholictimer.utils.Constants

class AlcoholicTimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 앱 설정 초기화
        Constants.init(applicationContext)
    }
}
