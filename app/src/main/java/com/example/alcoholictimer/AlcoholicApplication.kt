package com.example.alcoholictimer

import android.app.Application
import com.example.alcoholictimer.utils.RecordManager

class AlcoholicApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // RecordManager 초기화
        RecordManager.init(this)
    }
}
