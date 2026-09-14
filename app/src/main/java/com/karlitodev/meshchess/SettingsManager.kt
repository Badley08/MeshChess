package com.karlitodev.meshchess

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("meshchess_prefs", Context.MODE_PRIVATE)

    fun saveLanguage(langCode: String) {
        prefs.edit().putString("language", langCode).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("language", "fr") ?: "fr"
    }
}
