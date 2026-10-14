package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VocabList
import com.example.data.model.VocabListWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabListDao {
    @Query("SELECT * FROM vocab_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<VocabList>>

    @Query("""
        SELECT 
            l.id AS id,
            l.name AS name,
            l.createdAt AS createdAt,
            l.colorTag AS colorTag,
            COUNT(w.id) AS totalCount,
            SUM(CASE WHEN w.status = 'SAVED' THEN 1 ELSE 0 END) AS savedCount,
            SUM(CASE WHEN w.status = 'FORGOTTEN' THEN 1 ELSE 0 END) AS forgottenCount,
            SUM(CASE WHEN w.status = 'UNCATEGORIZED' THEN 1 ELSE 0 END) AS uncategorizedCount
        FROM vocab_lists l
        LEFT JOIN words w ON l.id = w.listId
        GROUP BY l.id
        ORDER BY l.createdAt DESC
    """)
    fun getListsWithStats(): Flow<List<VocabListWithStats>>

    @Query("""
        SELECT 
            l.id AS id,
            l.name AS name,
            l.createdAt AS createdAt,
            l.colorTag AS colorTag,
            COUNT(w.id) AS totalCount,
            SUM(CASE WHEN w.status = 'SAVED' THEN 1 ELSE 0 END) AS savedCount,
            SUM(CASE WHEN w.status = 'FORGOTTEN' THEN 1 ELSE 0 END) AS forgottenCount,
            SUM(CASE WHEN w.status = 'UNCATEGORIZED' THEN 1 ELSE 0 END) AS uncategorizedCount
        FROM vocab_lists l
        LEFT JOIN words w ON l.id = w.listId
        WHERE l.id = :listId
        GROUP BY l.id
    """)
    fun getListWithStatsById(listId: Long): Flow<VocabListWithStats?>

    @Query("SELECT * FROM vocab_lists WHERE id = :listId LIMIT 1")
    suspend fun getListById(listId: Long): VocabList?

    @Query("SELECT * FROM vocab_lists WHERE id = :listId LIMIT 1")
    fun getListByIdFlow(listId: Long): Flow<VocabList?>

    @Query("SELECT * FROM vocab_lists")
    suspend fun getAllListsDirect(): List<VocabList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: VocabList): Long

    @Update
    suspend fun updateList(list: VocabList)

    @Query("DELETE FROM vocab_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)
}
