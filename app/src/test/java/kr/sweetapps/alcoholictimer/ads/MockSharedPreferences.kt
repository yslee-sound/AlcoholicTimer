package kr.sweetapps.alcoholictimer.ads

import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

class MockSharedPreferences : SharedPreferences {
    private val map = ConcurrentHashMap<String, Any>()

    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? MutableSet<String>) ?: defValues
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun getAll(): MutableMap<String, *> = map

    inner class Editor : SharedPreferences.Editor {
        private val changes = HashMap<String, Any?>()
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { if (key != null) changes[key] = value; return this }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { if (key != null && values != null) changes[key] = values; return this }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor { if (key != null) changes[key] = value; return this }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor { if (key != null) changes[key] = value; return this }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { if (key != null) changes[key] = value; return this }
        override fun putString(key: String?, value: String?): SharedPreferences.Editor { if (key != null && value != null) changes[key] = value; return this }
        override fun remove(key: String?): SharedPreferences.Editor { if (key != null) changes[key] = null; return this }
        override fun clear(): SharedPreferences.Editor { changes.clear(); changes["__clear__"] = true; return this }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (changes.containsKey("__clear__")) map.clear()
            for ((k, v) in changes) {
                if (k == "__clear__") continue
                if (v == null) map.remove(k) else map[k] = v
            }
            changes.clear()
        }
    }
}

