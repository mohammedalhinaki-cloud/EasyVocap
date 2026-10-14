package com.example.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.QuizSessionState
import com.example.data.model.VocabListWithStats
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListDetailViewModel(
    application: Application,
    private val listId: Long
) : AndroidViewModel(application) {

    private val repository = VocabRepository(AppDatabase.getDatabase(application))

    val listWithStats: StateFlow<VocabListWithStats?> = repository.getListWithStatsById(listId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeSession: StateFlow<QuizSessionState?> = repository.getActiveSessionFlow(listId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _csvExportContent = MutableStateFlow<String?>(null)
    val csvExportContent: StateFlow<String?> = _csvExportContent.asStateFlow()

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetListProgress(listId)
        }
    }

    fun deleteActiveSession() {
        viewModelScope.launch {
            repository.deleteQuizSession(listId)
        }
    }

    fun prepareCsvExport(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportListToCsv(listId)
            _csvExportContent.value = csv
            onReady(csv)
        }
    }
}
