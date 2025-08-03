package com.example.alcoholictimer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.alcoholictimer.utils.Constants
import com.google.android.material.navigation.NavigationView
import java.util.Date

/**
 * 모든 액티비티의 베이스 클래스
 * 공통된 햄버거 메뉴와 네비게이션 기능을 제공합니다.
 */
abstract class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

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
            // 드로어를 열기 전에 최신 상태로 업데이트
            updateNavigationDrawer()
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 내비게이션 메뉴 아이템 클릭 이벤트
        navigationView.setNavigationItemSelectedListener(this)

        // 특정 화면에 필요한 컨텐츠 뷰 설정
        setupContentView()
    }

    /**
     * 금주 상태에 따라 내비게이션 메뉴 상태와 헤더 정보를 업데이트합니다.
     */
    private fun updateNavigationDrawer() {
        // 네비게이션 메뉴 상태 업데이트
        updateNavigationMenuState()

        // 네비게이션 헤더 정보 업데이트
        updateNavigationHeader()
    }

    /**
     * 금주 상태에 따라 내비게이션 메뉴 상태를 업데이트합니다.
     * 금주 메뉴는 항상 활성화하고, 금주 중인 경우 텍스트를 '금주 상태'로 변경합니다.
     */
    private fun updateNavigationMenuState() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val hasStarted = sharedPref.contains("start_time")

        // 금주 메뉴는 항상 활성화하고, 금주 중인 경우 텍스트 변경
        val soberMenuItem = navigationView.menu.findItem(R.id.nav_sober)
        soberMenuItem.isEnabled = true
        if (hasStarted) {
            soberMenuItem.title = "금주 상태"
        } else {
            soberMenuItem.title = "금주"
        }

        // 활동 보기 메뉴는 항상 활성화
        val recordsMenuItem = navigationView.menu.findItem(R.id.nav_records)
        recordsMenuItem.isEnabled = true
    }

    /**
     * 네비게이션 헤더의 사용자 정보를 업데이트합니다.
     */
    private fun updateNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        if (headerView != null) {
            val tvUserNickname = headerView.findViewById<TextView>(R.id.tvUserNickname)
            val tvUserLevelDays = headerView.findViewById<TextView>(R.id.tvUserLevelDays)

            // 사용자 이름은 기본값으로 "알중이" 사용
            tvUserNickname.text = "알중이"

            // 금주 상태에 따른 레벨 및 일수 정보 업데이트
            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            if (sharedPref.contains("start_time")) {
                val startTime = sharedPref.getLong("start_time", Date().time)
                val timePassed = ((System.currentTimeMillis() - startTime) / Constants.TIME_UNIT_MILLIS).toInt()

                // 레벨 계산 (시간 단위에 따라 적절한 마일스톤 사용)
                val adjustedMilestones = when {
                    Constants.isSecondTestMode -> listOf(0, 7, 14, 30, 60, 120, 240, 365)
                    Constants.isMinuteTestMode -> listOf(0, 1, 2, 5, 10, 15, 20, 30)
                    else -> listOf(0, 7, 14, 30, 60, 120, 240, 365)
                }

                var currentLevel = 1
                for (i in adjustedMilestones.indices) {
                    if (timePassed >= adjustedMilestones[i]) {
                        currentLevel = i + 1
                    } else {
                        break
                    }
                }

                tvUserLevelDays.text = "Level $currentLevel · ${timePassed}${Constants.TIME_UNIT_TEXT} 금주 중"
            } else {
                tvUserLevelDays.text = "금주를 시작해보세요!"
            }
        }
    }

    /**
     * 각 액티비티에서 구현할 추상 메소드
     * 각자의 레이아웃을 contentFrame에 추가하는 작업을 수행
     */
    protected abstract fun setupContentView()

    /**
     * 액티비티가 새 인텐트로 재사용될 때 수행할 작업을 정의합니다.
     * 자식 클래스에서 오버라이드할 수 있습니다.
     */
    open fun handleNewIntent(intent: Intent?) {
        // 기본 구현은 아무 작업도 수행하지 않습니다
    }

    /**
     * 효과 없이 액티비티 전환
     */
    protected fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(0, 0) // 전환 효과 제거
    }

    /**
     * 액티비티 종료 시 효과 없이 전환
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0) // 전환 효과 제거
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_records -> {
                if (this !is RecordsActivity) {
                    val intent = Intent(this, RecordsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)  // 화면 전환 효과 제거
                }
            }
            R.id.nav_challenge -> {
                if (this !is ChallengeActivity) {
                    val intent = Intent(this, ChallengeActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            R.id.nav_message -> {
                if (this !is MessageActivity) {
                    val intent = Intent(this, MessageActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            R.id.nav_settings -> {
                if (this !is SettingsActivity) {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            R.id.nav_notifications -> {
                if (this !is NotificationsActivity) {
                    val intent = Intent(this, NotificationsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            R.id.nav_sober -> {
                // 금주 중일 때만 금주 화면으로 이동 가능
                val prefs = getSharedPreferences("user_settings", MODE_PRIVATE)
                val isAbstaining = prefs.contains("start_time")

                if (isAbstaining && this !is StatusActivity) {
                    val intent = Intent(this, StatusActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                } else if (!isAbstaining && this !is StartActivity) {
                    val intent = Intent(this, StartActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
