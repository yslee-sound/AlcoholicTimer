package kr.sweetapps.alcoholictimer.ui.tab_05.screens.policy

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class DocumentViewModel : ViewModel() {
    var content by mutableStateOf<String?>(null)
        private set

    // resName: raw 리소스 이름(확장자 제외)
    fun load(context: Context, resName: String) {
        try {
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId != 0) {
                val text = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
                content = text
            } else {
                content = "파일을 찾을 수 없습니다: $resName"
            }
        } catch (_: Exception) {
            content = "파일을 읽을 수 없습니다: $resName"
        }
    }
}
