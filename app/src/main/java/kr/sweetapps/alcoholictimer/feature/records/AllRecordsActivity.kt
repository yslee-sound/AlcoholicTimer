package kr.sweetapps.alcoholictimer.feature.records

import android.os.Bundle
import kr.sweetapps.alcoholictimer.core.ui.BaseActivity
import kr.sweetapps.alcoholictimer.R

class AllRecordsActivity : BaseActivity() {
    // Legacy stub: use Screen.AllRecords in NavHost instead.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun getScreenTitleResId(): Int = R.string.all_records_title
    @Suppress("DEPRECATION")
    override fun getScreenTitle(): String = getString(R.string.all_records_title)
}
