package kr.sweetapps.alcoholictimer.ui.tab_04.screens

import android.content.Context
import kr.sweetapps.alcoholictimer.util.CurrencyManager

class Tab04ViewModel {
    fun getSelectedCurrencyCode(context: Context): String {
        return CurrencyManager.getSelectedCurrency(context).code
    }

    fun saveCurrency(context: Context, code: String) {
        CurrencyManager.saveCurrency(context, code)
    }
}
