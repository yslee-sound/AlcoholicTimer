package kr.sweetapps.alcoholictimer.feature.records

import android.os.Bundle
import kr.sweetapps.alcoholictimer.R
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity

class RecordsActivity : BaseActivity() {
    // Legacy stub: Records 화면은 NavHost(Screen.Records)로 진입합니다.
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState) }
    override fun getScreenTitleResId(): Int = R.string.records_title
    @Suppress("DEPRECATION")
    override fun getScreenTitle(): String = getString(R.string.records_title)
}
