package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object WordStatus {
    const val UNCATEGORIZED = "UNCATEGORIZED"
    const val SAVED = "SAVED"
    const val FORGOTTEN = "FORGOTTEN"
}

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = VocabList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("listId"),
        Index("listId", "status"),
        Index("listId", "english")
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val english: String,
    val arabic: String,
    val exampleEn: String = "",
    val exampleAr: String = "",
    val derivedEn: String = "",
    val derivedAr: String = "",
    val status: String = WordStatus.UNCATEGORIZED,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val knownCount: Int = 0,
    val forgottenCount: Int = 0
)
