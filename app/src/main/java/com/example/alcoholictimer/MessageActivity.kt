package com.example.alcoholictimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup

class MessageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setupContentView() {
        // 응원 메시지 액티비티의 컨텐츠를 contentFrame에 추가
        val contentFrame = findViewById<ViewGroup>(R.id.contentFrame)
        LayoutInflater.from(this).inflate(R.layout.activity_message, contentFrame, true)
    }
}
