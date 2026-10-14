package com.example.data.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak_enabled")
        val KEY_TIMER_SECONDS = intPreferencesKey("timer_seconds")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_QUICK_REVIEW_AUTO = booleanPreferencesKey("quick_review_auto")
        val KEY_QUICK_REVIEW_INTERVAL = intPreferencesKey("quick_review_interval")
    }

    val autoSpeakEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SPEAK] ?: true
    }

    val timerSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TIMER_SECONDS] ?: 3
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val quickReviewAuto: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUICK_REVIEW_AUTO] ?: false
    }

    val quickReviewInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUICK_REVIEW_INTERVAL] ?: 3
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SPEAK] = enabled
        }
    }

    suspend fun setTimerSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 15)
        context.dataStore.edit { prefs ->
            prefs[KEY_TIMER_SECONDS] = clamped
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setQuickReviewAuto(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_REVIEW_AUTO] = enabled
        }
    }

    suspend fun setQuickReviewInterval(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        context.dataStore.edit { prefs ->
            prefs[KEY_QUICK_REVIEW_INTERVAL] = clamped
        }
    }
}
