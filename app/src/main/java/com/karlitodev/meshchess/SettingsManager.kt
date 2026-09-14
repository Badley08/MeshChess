package com.karlitodev.meshchess

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("meshchess_prefs", Context.MODE_PRIVATE)

    fun saveLanguage(langCode: String) {
        prefs.edit().putString("language", langCode).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("language", "en") ?: "en"
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean("first_launch", true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean("first_launch", false).apply()
    }

    fun getGeminiApiKey(): String? {
        return prefs.getString("gemini_api_key", null)
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun getAIDifficulty(): String {
        return prefs.getString("ai_difficulty", "medium") ?: "medium"
    }

    fun saveAIDifficulty(difficulty: String) {
        prefs.edit().putString("ai_difficulty", difficulty).apply()
    }
}
