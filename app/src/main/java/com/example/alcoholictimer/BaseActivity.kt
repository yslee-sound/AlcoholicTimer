package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

/**
 * 모든 액티비티의 베이스 클래스
 * 공통된 햄버거 메뉴와 네비게이션 기능을 제공합니다.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 시스템 UI와의 겹침 처리를 위한 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.layout_base)

        // 공통 UI 요소 초기화
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)

        // 메뉴 버튼 클릭 시 드로어 열기
        btnMenu.setOnClickListener {
            // 드로어를 열기 전에 금주 상태를 확인하여 메뉴 아이템 활성화/비활성화 설정
            updateNavigationMenuState()
            drawerLayout.open()
        }

        // 내비게이션 메뉴 아이템 클릭 이벤트
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_start -> {
                    if (this !is StartActivity) {
                        startActivity(Intent(this, StartActivity::class.java))
                        if (this !is MainActivity) {
                            finish()
                        }
                    }
                }
                R.id.nav_status -> {
                    if (this !is StatusActivity) {
                        startActivity(Intent(this, StatusActivity::class.java))
                        if (this !is MainActivity) {
                            finish()
                        }
                    }
                }
            }
            drawerLayout.close()
            true
        }

        // 각 액티비티의 레이아웃을 contentFrame에 추가
        setupContentView()
    }

    /**
     * 금주 상태에 따라 내비게이션 메뉴 상태를 업데이트합니다.
     * 이미 금주를 시작했다면 금주 시작 메뉴를 비활성화합니다.
     */
    private fun updateNavigationMenuState() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val hasStarted = sharedPref.contains("start_time")

        // 금주가 이미 시작되었으면 시작 메뉴 비활성화
        val startMenuItem = navigationView.menu.findItem(R.id.nav_start)
        startMenuItem.isEnabled = !hasStarted

        // 금주가 시작되었을 때만 상태 메뉴 활성화
        val statusMenuItem = navigationView.menu.findItem(R.id.nav_status)
        statusMenuItem.isEnabled = hasStarted
    }

    /**
     * 각 액티비티에서 구현할 추상 메소드
     * 각자의 레이아웃을 contentFrame에 추가하는 작업을 수행
     */
    protected abstract fun setupContentView()
}
