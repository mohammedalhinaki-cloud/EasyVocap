package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_sessions",
    foreignKeys = [
        ForeignKey(
            entity = VocabList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class QuizSessionState(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val filterType: String, // "ALL", "FORGOTTEN", "UNCATEGORIZED", "SAVED"
    val wordIdsCsv: String, // comma-separated word IDs in order
    val currentIndex: Int = 0,
    val knewCount: Int = 0,
    val forgotCount: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
) {
    fun getWordIds(): List<Long> {
        if (wordIdsCsv.isBlank()) return emptyList()
        return wordIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
}
