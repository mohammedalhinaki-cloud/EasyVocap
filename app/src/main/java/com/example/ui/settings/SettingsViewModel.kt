package com.example.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsManager
import com.example.data.db.AppDatabase
import com.example.data.pref.UserPreferences
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val backupJsonReady: String? = null,
    val restoreSuccess: Boolean? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = UserPreferences(application)
    private val repository = VocabRepository(AppDatabase.getDatabase(application))
    val ttsManager = TtsManager(application)

    val autoSpeak: StateFlow<Boolean> = preferences.autoSpeakEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val timerSeconds: StateFlow<Int> = preferences.timerSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val themeMode: StateFlow<String> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val ttsReady: StateFlow<Boolean> = ttsManager.isReady
    val hasUsVoice: StateFlow<Boolean> = ttsManager.hasUsVoice

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoSpeak(enabled)
        }
    }

    fun setTimerSeconds(seconds: Int) {
        viewModelScope.launch {
            preferences.setTimerSeconds(seconds)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun testTtsSpeech() {
        ttsManager.speak("LexiVocab is ready. You can learn and review words completely offline.")
    }

    fun openSystemTtsSettings() {
        ttsManager.openTtsSettings()
    }

    fun prepareBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportFullBackupJson()
            _uiState.value = _uiState.value.copy(backupJsonReady = json)
            onReady(json)
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                } ?: throw IllegalArgumentException("تعذر قراءة ملف النسخة الاحتياطية")

                val success = repository.restoreFullBackupJson(jsonString)
                _uiState.value = _uiState.value.copy(restoreSuccess = success, errorMessage = if (success) null else "صيغة الملف غير متوافقة")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(restoreSuccess = false, errorMessage = e.message ?: "فشلت الاستعادة")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(restoreSuccess = null, errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
