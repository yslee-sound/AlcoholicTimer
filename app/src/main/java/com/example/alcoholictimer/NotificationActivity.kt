package com.example.alcoholictimer

import android.os.Bundle

class NotificationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupContentView()
    }

    override fun setupContentView() {
        setContentView(R.layout.activity_notification)
    }
}
