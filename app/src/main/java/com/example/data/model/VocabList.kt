package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocab_lists")
data class VocabList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val colorTag: Int = 0
)

data class VocabListWithStats(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val colorTag: Int,
    val totalCount: Int,
    val savedCount: Int,
    val forgottenCount: Int,
    val uncategorizedCount: Int
) {
    val progressPercent: Float
        get() = if (totalCount > 0) (savedCount.toFloat() / totalCount.toFloat()) * 100f else 0f
}
