package com.example.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsManager
import com.example.data.db.AppDatabase
import com.example.data.model.Word
import com.example.data.model.WordStatus
import com.example.data.pref.UserPreferences
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class QuickReviewUiState(
    val isLoading: Boolean = true,
    val words: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val autoAdvance: Boolean = false,
    val advanceIntervalSeconds: Int = 3,
    val autoSpeak: Boolean = true,
    val errorMessage: String? = null
) {
    val currentWord: Word?
        get() = words.getOrNull(currentIndex)
}

class QuickReviewViewModel(
    application: Application,
    private val listId: Long,
    private val filterType: String
) : AndroidViewModel(application) {

    private val repository = VocabRepository(AppDatabase.getDatabase(application))
    private val preferences = UserPreferences(application)
    val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(QuickReviewUiState())
    val uiState: StateFlow<QuickReviewUiState> = _uiState.asStateFlow()

    private var autoAdvanceJob: Job? = null

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            val autoSp = preferences.autoSpeakEnabled.first()
            val autoAdv = preferences.quickReviewAuto.first()
            val interval = preferences.quickReviewInterval.first()

            val wordsList = when (filterType) {
                "FORGOTTEN" -> repository.getWordsDirectByStatus(listId, WordStatus.FORGOTTEN)
                "SAVED" -> repository.getWordsDirectByStatus(listId, WordStatus.SAVED)
                "UNCATEGORIZED" -> repository.getWordsDirectByStatus(listId, WordStatus.UNCATEGORIZED)
                else -> repository.getWordsDirect(listId)
            }

            if (wordsList.isEmpty()) {
                _uiState.value = QuickReviewUiState(
                    isLoading = false,
                    errorMessage = "لا توجد كلمات في هذا القسم للمراجعة"
                )
                return@launch
            }

            _uiState.value = QuickReviewUiState(
                isLoading = false,
                words = wordsList,
                currentIndex = 0,
                autoAdvance = autoAdv,
                advanceIntervalSeconds = interval,
                autoSpeak = autoSp
            )

            speakCurrentWord()
            if (autoAdv) {
                startAutoAdvanceLoop()
            }
        }
    }

    fun nextWord() {
        val total = _uiState.value.words.size
        if (total == 0) return
        val nextIdx = (_uiState.value.currentIndex + 1) % total
        setIndex(nextIdx)
    }

    fun previousWord() {
        val total = _uiState.value.words.size
        if (total == 0) return
        val prevIdx = if (_uiState.value.currentIndex - 1 < 0) total - 1 else _uiState.value.currentIndex - 1
        setIndex(prevIdx)
    }

    fun setIndex(index: Int) {
        val total = _uiState.value.words.size
        if (index in 0 until total) {
            _uiState.value = _uiState.value.copy(currentIndex = index)
            speakCurrentWord()
            if (_uiState.value.autoAdvance) {
                restartAutoAdvance()
            }
        }
    }

    fun toggleAutoSpeak() {
        val newVal = !_uiState.value.autoSpeak
        _uiState.value = _uiState.value.copy(autoSpeak = newVal)
        viewModelScope.launch {
            preferences.setAutoSpeak(newVal)
        }
    }

    fun toggleAutoAdvance() {
        val newVal = !_uiState.value.autoAdvance
        _uiState.value = _uiState.value.copy(autoAdvance = newVal)
        viewModelScope.launch {
            preferences.setQuickReviewAuto(newVal)
        }
        if (newVal) {
            startAutoAdvanceLoop()
        } else {
            autoAdvanceJob?.cancel()
        }
    }

    fun setIntervalSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        _uiState.value = _uiState.value.copy(advanceIntervalSeconds = clamped)
        viewModelScope.launch {
            preferences.setQuickReviewInterval(clamped)
        }
        if (_uiState.value.autoAdvance) {
            restartAutoAdvance()
        }
    }

    fun speakCurrentWord() {
        if (_uiState.value.autoSpeak) {
            _uiState.value.currentWord?.let {
                ttsManager.speak(it.english)
            }
        }
    }

    fun speakCurrentWordManual() {
        _uiState.value.currentWord?.let {
            ttsManager.speak(it.english)
        }
    }

    private fun restartAutoAdvance() {
        autoAdvanceJob?.cancel()
        startAutoAdvanceLoop()
    }

    private fun startAutoAdvanceLoop() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            while (true) {
                delay(_uiState.value.advanceIntervalSeconds * 1000L)
                nextWord()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
        ttsManager.shutdown()
    }
}
