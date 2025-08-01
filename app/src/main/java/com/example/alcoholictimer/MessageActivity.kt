package com.example.alcoholictimer

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout

class MessageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setupContentView() {
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
        LayoutInflater.from(this).inflate(R.layout.activity_message, contentFrame, true)
    }
}
