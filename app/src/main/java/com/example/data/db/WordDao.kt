package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Word
import com.example.data.model.WordStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE listId = :listId ORDER BY createdAt DESC")
    fun getWordsByListId(listId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE listId = :listId AND status = :status ORDER BY createdAt DESC")
    fun getWordsByListIdAndStatus(listId: Long, status: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE listId = :listId ORDER BY createdAt DESC")
    suspend fun getWordsListDirect(listId: Long): List<Word>

    @Query("SELECT * FROM words WHERE listId = :listId AND status = :status ORDER BY createdAt DESC")
    suspend fun getWordsListDirectByStatus(listId: Long, status: String): List<Word>

    @Query("SELECT * FROM words WHERE id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: Long): Word?

    @Query("SELECT * FROM words WHERE id IN (:wordIds)")
    suspend fun getWordsByIds(wordIds: List<Long>): List<Word>

    @Query("SELECT * FROM words WHERE listId = :listId AND LOWER(TRIM(english)) = LOWER(TRIM(:english)) LIMIT 1")
    suspend fun findExistingWord(listId: Long, english: String): Word?

    @Query("""
        SELECT * FROM words 
        WHERE listId = :listId 
        AND (LOWER(english) LIKE '%' || LOWER(:query) || '%' OR arabic LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchWords(listId: Long, query: String): Flow<List<Word>>

    @Query("""
        SELECT * FROM words 
        WHERE listId = :listId AND status = :status
        AND (LOWER(english) LIKE '%' || LOWER(:query) || '%' OR arabic LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchWordsByStatus(listId: Long, status: String, query: String): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<Word>): List<Long>

    @Update
    suspend fun updateWord(word: Word)

    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteWordById(wordId: Long)

    @Query("""
        UPDATE words 
        SET status = :status,
            lastReviewedAt = :timestamp,
            knownCount = knownCount + :knownDelta,
            forgottenCount = forgottenCount + :forgottenDelta
        WHERE id = :wordId
    """)
    suspend fun updateWordClassification(
        wordId: Long,
        status: String,
        timestamp: Long,
        knownDelta: Int,
        forgottenDelta: Int
    )

    @Query("""
        UPDATE words
        SET status = 'UNCATEGORIZED',
            lastReviewedAt = NULL,
            knownCount = 0,
            forgottenCount = 0
        WHERE listId = :listId
    """)
    suspend fun resetListProgress(listId: Long)

    @Query("SELECT COUNT(*) FROM words WHERE listId = :listId")
    suspend fun getWordCount(listId: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE listId = :listId AND status = :status")
    suspend fun getWordCountByStatus(listId: Long, status: String): Int

    @Query("SELECT * FROM words")
    suspend fun getAllWordsDirect(): List<Word>
}
