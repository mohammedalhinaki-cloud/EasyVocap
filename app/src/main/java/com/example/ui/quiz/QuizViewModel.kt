package com.example.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsManager
import com.example.data.db.AppDatabase
import com.example.data.model.QuizSessionState
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

data class QuizUiState(
    val isLoading: Boolean = true,
    val currentWord: Word? = null,
    val isRevealed: Boolean = false,
    val currentIndex: Int = 0,
    val totalWords: Int = 0,
    val remainingSeconds: Float = 3f,
    val timerProgress: Float = 1f,
    val timerDurationSeconds: Int = 3,
    val knewCount: Int = 0,
    val forgotCount: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val sessionStartTime: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

class QuizViewModel(
    application: Application,
    private val listId: Long,
    private val filterType: String
) : AndroidViewModel(application) {

    private val repository = VocabRepository(AppDatabase.getDatabase(application))
    private val preferences = UserPreferences(application)
    val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var wordsQueue: List<Word> = emptyList()
    private var timerJob: Job? = null
    private var autoSpeakEnabled = true
    private var configuredTimerSeconds = 3
    private var currentSessionId: Long? = null
    private var sessionStartTime = System.currentTimeMillis()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            autoSpeakEnabled = preferences.autoSpeakEnabled.first()
            configuredTimerSeconds = preferences.timerSeconds.first()

            val existingSession = repository.getActiveSession(listId)

            if (existingSession != null && existingSession.filterType == filterType && !existingSession.isCompleted) {
                // Resume existing session
                currentSessionId = existingSession.id
                sessionStartTime = existingSession.startedAt
                val wordIds = existingSession.getWordIds()
                val loadedWords = repository.getWordsByIds(wordIds)
                // preserve the randomized order from session
                val wordMap = loadedWords.associateBy { it.id }
                wordsQueue = wordIds.mapNotNull { wordMap[it] }

                val resumeIndex = existingSession.currentIndex.coerceAtMost(wordsQueue.size)
                if (resumeIndex < wordsQueue.size) {
                    _uiState.value = QuizUiState(
                        isLoading = false,
                        currentWord = wordsQueue[resumeIndex],
                        currentIndex = resumeIndex,
                        totalWords = wordsQueue.size,
                        knewCount = existingSession.knewCount,
                        forgotCount = existingSession.forgotCount,
                        timerDurationSeconds = configuredTimerSeconds,
                        remainingSeconds = configuredTimerSeconds.toFloat(),
                        sessionStartTime = sessionStartTime
                    )
                    startTimerForCurrentWord()
                } else {
                    finishQuiz()
                }
            } else {
                // Start fresh session with words matching filterType
                val rawWords = when (filterType) {
                    "FORGOTTEN" -> repository.getWordsDirectByStatus(listId, WordStatus.FORGOTTEN)
                    "SAVED" -> repository.getWordsDirectByStatus(listId, WordStatus.SAVED)
                    "UNCATEGORIZED" -> repository.getWordsDirectByStatus(listId, WordStatus.UNCATEGORIZED)
                    else -> repository.getWordsDirect(listId)
                }

                if (rawWords.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "لا توجد كلمات مطابقة للاختبار في هذا التصنيف"
                    )
                    return@launch
                }

                // Randomize without repetition
                wordsQueue = rawWords.shuffled()
                sessionStartTime = System.currentTimeMillis()

                val sessionState = QuizSessionState(
                    listId = listId,
                    filterType = filterType,
                    wordIdsCsv = wordsQueue.joinToString(",") { it.id.toString() },
                    currentIndex = 0,
                    knewCount = 0,
                    forgotCount = 0,
                    startedAt = sessionStartTime,
                    updatedAt = System.currentTimeMillis(),
                    isCompleted = false
                )
                currentSessionId = repository.saveQuizSession(sessionState)

                _uiState.value = QuizUiState(
                    isLoading = false,
                    currentWord = wordsQueue.firstOrNull(),
                    currentIndex = 0,
                    totalWords = wordsQueue.size,
                    knewCount = 0,
                    forgotCount = 0,
                    timerDurationSeconds = configuredTimerSeconds,
                    remainingSeconds = configuredTimerSeconds.toFloat(),
                    sessionStartTime = sessionStartTime
                )
                startTimerForCurrentWord()
            }
        }
    }

    private fun startTimerForCurrentWord() {
        timerJob?.cancel()
        val currentWord = _uiState.value.currentWord ?: return

        // Pronounce automatically if auto-speak is enabled
        if (autoSpeakEnabled) {
            ttsManager.speak(currentWord.english)
        }

        val totalMs = configuredTimerSeconds * 1000L
        val intervalMs = 50L

        timerJob = viewModelScope.launch {
            var elapsedMs = 0L
            while (elapsedMs < totalMs) {
                if (!_uiState.value.isPaused) {
                    delay(intervalMs)
                    elapsedMs += intervalMs
                    val remainingSec = ((totalMs - elapsedMs).toFloat() / 1000f).coerceAtLeast(0f)
                    val progress = (1f - (elapsedMs.toFloat() / totalMs.toFloat())).coerceIn(0f, 1f)
                    _uiState.value = _uiState.value.copy(
                        remainingSeconds = remainingSec,
                        timerProgress = progress
                    )
                } else {
                    delay(100L)
                }
            }
            // Auto reveal Arabic meaning and examples
            revealAnswer()
        }
    }

    fun revealAnswer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isRevealed = true,
            remainingSeconds = 0f,
            timerProgress = 0f
        )
    }

    fun onAnswer(isKnown: Boolean) {
        timerJob?.cancel()
        val currentWord = _uiState.value.currentWord ?: return
        val newKnewCount = _uiState.value.knewCount + if (isKnown) 1 else 0
        val newForgotCount = _uiState.value.forgotCount + if (!isKnown) 1 else 0
        val nextIndex = _uiState.value.currentIndex + 1

        viewModelScope.launch {
            // Immediately classify in Room DB
            repository.classifyWord(currentWord.id, isKnown)

            // Update persistent QuizSessionState
            currentSessionId?.let { sId ->
                val isCompleted = nextIndex >= wordsQueue.size
                repository.updateQuizSession(
                    QuizSessionState(
                        id = sId,
                        listId = listId,
                        filterType = filterType,
                        wordIdsCsv = wordsQueue.joinToString(",") { it.id.toString() },
                        currentIndex = nextIndex,
                        knewCount = newKnewCount,
                        forgotCount = newForgotCount,
                        startedAt = sessionStartTime,
                        updatedAt = System.currentTimeMillis(),
                        isCompleted = isCompleted
                    )
                )
            }

            if (nextIndex < wordsQueue.size) {
                _uiState.value = _uiState.value.copy(
                    currentWord = wordsQueue[nextIndex],
                    currentIndex = nextIndex,
                    isRevealed = false,
                    knewCount = newKnewCount,
                    forgotCount = newForgotCount,
                    remainingSeconds = configuredTimerSeconds.toFloat(),
                    timerProgress = 1f
                )
                startTimerForCurrentWord()
            } else {
                _uiState.value = _uiState.value.copy(
                    knewCount = newKnewCount,
                    forgotCount = newForgotCount,
                    isFinished = true
                )
                finishQuiz()
            }
        }
    }

    fun onPauseLifecycle() {
        _uiState.value = _uiState.value.copy(isPaused = true)
        ttsManager.stop()
    }

    fun onResumeLifecycle() {
        _uiState.value = _uiState.value.copy(isPaused = false)
    }

    fun speakCurrentWord() {
        val word = _uiState.value.currentWord ?: return
        ttsManager.speak(word.english)
    }

    private fun finishQuiz() {
        _uiState.value = _uiState.value.copy(isFinished = true)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        ttsManager.shutdown()
    }
}
