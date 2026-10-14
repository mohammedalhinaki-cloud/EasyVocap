package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.QuizSessionState
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizSessionDao {
    @Query("SELECT * FROM quiz_sessions WHERE listId = :listId AND isCompleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSession(listId: Long): QuizSessionState?

    @Query("SELECT * FROM quiz_sessions WHERE listId = :listId AND isCompleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    fun getActiveSessionFlow(listId: Long): Flow<QuizSessionState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: QuizSessionState): Long

    @Update
    suspend fun updateSession(session: QuizSessionState)

    @Query("DELETE FROM quiz_sessions WHERE listId = :listId")
    suspend fun deleteSessionsForList(listId: Long)

    @Query("DELETE FROM quiz_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}
