package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.QuizSessionDao
import com.example.data.db.VocabListDao
import com.example.data.db.WordDao
import com.example.data.model.QuizSessionState
import com.example.data.model.VocabList
import com.example.data.model.VocabListWithStats
import com.example.data.model.Word
import com.example.data.model.WordStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class VocabRepository(private val db: AppDatabase) {

    private val listDao: VocabListDao = db.vocabListDao()
    private val wordDao: WordDao = db.wordDao()
    private val quizDao: QuizSessionDao = db.quizSessionDao()

    val listsWithStats: Flow<List<VocabListWithStats>> = listDao.getListsWithStats()

    fun getListWithStatsById(listId: Long): Flow<VocabListWithStats?> = listDao.getListWithStatsById(listId)

    fun getListByIdFlow(listId: Long): Flow<VocabList?> = listDao.getListByIdFlow(listId)

    suspend fun getListById(listId: Long): VocabList? = withContext(Dispatchers.IO) {
        listDao.getListById(listId)
    }

    suspend fun createList(name: String, colorTag: Int = 0): Long = withContext(Dispatchers.IO) {
        listDao.insertList(VocabList(name = name.trim(), colorTag = colorTag))
    }

    suspend fun updateList(list: VocabList) = withContext(Dispatchers.IO) {
        listDao.updateList(list)
    }

    suspend fun deleteList(listId: Long) = withContext(Dispatchers.IO) {
        listDao.deleteListById(listId)
    }

    fun getWordsByListId(listId: Long): Flow<List<Word>> = wordDao.getWordsByListId(listId)

    fun getWordsByListIdAndStatus(listId: Long, status: String): Flow<List<Word>> =
        wordDao.getWordsByListIdAndStatus(listId, status)

    suspend fun getWordsDirect(listId: Long): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getWordsListDirect(listId)
    }

    suspend fun getWordsDirectByStatus(listId: Long, status: String): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getWordsListDirectByStatus(listId, status)
    }

    suspend fun getWordsByIds(ids: List<Long>): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getWordsByIds(ids)
    }

    suspend fun getWordById(id: Long): Word? = withContext(Dispatchers.IO) {
        wordDao.getWordById(id)
    }

    suspend fun isWordDuplicate(listId: Long, english: String): Boolean = withContext(Dispatchers.IO) {
        wordDao.findExistingWord(listId, english) != null
    }

    suspend fun addWord(word: Word): Long = withContext(Dispatchers.IO) {
        wordDao.insertWord(word)
    }

    suspend fun updateWord(word: Word) = withContext(Dispatchers.IO) {
        wordDao.updateWord(word)
    }

    suspend fun deleteWord(wordId: Long) = withContext(Dispatchers.IO) {
        wordDao.deleteWordById(wordId)
    }

    suspend fun classifyWord(wordId: Long, isKnown: Boolean) = withContext(Dispatchers.IO) {
        val newStatus = if (isKnown) WordStatus.SAVED else WordStatus.FORGOTTEN
        val knownDelta = if (isKnown) 1 else 0
        val forgottenDelta = if (!isKnown) 1 else 0
        wordDao.updateWordClassification(
            wordId = wordId,
            status = newStatus,
            timestamp = System.currentTimeMillis(),
            knownDelta = knownDelta,
            forgottenDelta = forgottenDelta
        )
    }

    suspend fun resetListProgress(listId: Long) = withContext(Dispatchers.IO) {
        wordDao.resetListProgress(listId)
        quizDao.deleteSessionsForList(listId)
    }

    suspend fun getActiveSession(listId: Long): QuizSessionState? = withContext(Dispatchers.IO) {
        quizDao.getActiveSession(listId)
    }

    fun getActiveSessionFlow(listId: Long): Flow<QuizSessionState?> = quizDao.getActiveSessionFlow(listId)

    suspend fun saveQuizSession(session: QuizSessionState): Long = withContext(Dispatchers.IO) {
        quizDao.insertSession(session)
    }

    suspend fun updateQuizSession(session: QuizSessionState) = withContext(Dispatchers.IO) {
        quizDao.updateSession(session)
    }

    suspend fun deleteQuizSession(listId: Long) = withContext(Dispatchers.IO) {
        quizDao.deleteSessionsForList(listId)
    }

    suspend fun batchInsertWords(
        listId: Long,
        words: List<Word>,
        skipDuplicates: Boolean = true
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val existingWords = wordDao.getWordsListDirect(listId).map { it.english.trim().lowercase() }.toSet()
        val toInsert = mutableListOf<Word>()
        var duplicatesCount = 0

        for (w in words) {
            val key = w.english.trim().lowercase()
            if (skipDuplicates && existingWords.contains(key)) {
                duplicatesCount++
            } else {
                toInsert.add(w.copy(listId = listId))
            }
        }

        if (toInsert.isNotEmpty()) {
            wordDao.insertWords(toInsert)
        }
        Pair(toInsert.size, duplicatesCount)
    }

    suspend fun exportListToCsv(listId: Long): String = withContext(Dispatchers.IO) {
        val words = wordDao.getWordsListDirect(listId)
        val sb = StringBuilder()
        // Include UTF-8 BOM so Excel and Arabic readers display properly
        sb.append('\uFEFF')
        sb.append("English,Arabic,Example EN,Example AR,Derived EN,Derived AR,Status,Known Count,Forgotten Count\n")
        for (w in words) {
            sb.append(escapeCsv(w.english)).append(",")
            sb.append(escapeCsv(w.arabic)).append(",")
            sb.append(escapeCsv(w.exampleEn)).append(",")
            sb.append(escapeCsv(w.exampleAr)).append(",")
            sb.append(escapeCsv(w.derivedEn)).append(",")
            sb.append(escapeCsv(w.derivedAr)).append(",")
            sb.append(escapeCsv(w.status)).append(",")
            sb.append(w.knownCount).append(",")
            sb.append(w.forgottenCount).append("\n")
        }
        sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "LexiVocab")

        val listsArray = JSONArray()
        val allLists = listDao.getAllListsDirect()
        val allWords = wordDao.getAllWordsDirect()

        val wordsByList = allWords.groupBy { it.listId }

        for (l in allLists) {
            val listObj = JSONObject()
            listObj.put("id", l.id)
            listObj.put("name", l.name)
            listObj.put("createdAt", l.createdAt)
            listObj.put("colorTag", l.colorTag)

            val wordsArray = JSONArray()
            val words = wordsByList[l.id] ?: emptyList()
            for (w in words) {
                val wObj = JSONObject()
                wObj.put("english", w.english)
                wObj.put("arabic", w.arabic)
                wObj.put("exampleEn", w.exampleEn)
                wObj.put("exampleAr", w.exampleAr)
                wObj.put("derivedEn", w.derivedEn)
                wObj.put("derivedAr", w.derivedAr)
                wObj.put("status", w.status)
                wObj.put("createdAt", w.createdAt)
                wObj.put("lastReviewedAt", w.lastReviewedAt ?: JSONObject.NULL)
                wObj.put("knownCount", w.knownCount)
                wObj.put("forgottenCount", w.forgottenCount)
                wordsArray.put(wObj)
            }
            listObj.put("words", wordsArray)
            listsArray.put(listObj)
        }

        root.put("lists", listsArray)
        root.toString(2)
    }

    suspend fun restoreFullBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val listsArray = root.getJSONArray("lists")

            for (i in 0 until listsArray.length()) {
                val listObj = listsArray.getJSONObject(i)
                val listName = listObj.getString("name")
                val colorTag = listObj.optInt("colorTag", 0)

                val newListId = listDao.insertList(
                    VocabList(
                        name = listName,
                        colorTag = colorTag,
                        createdAt = listObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )

                val wordsArray = listObj.optJSONArray("words")
                if (wordsArray != null) {
                    val wordsToInsert = mutableListOf<Word>()
                    for (j in 0 until wordsArray.length()) {
                        val wObj = wordsArray.getJSONObject(j)
                        val lastReviewed = if (wObj.isNull("lastReviewedAt")) null else wObj.optLong("lastReviewedAt")
                        wordsToInsert.add(
                            Word(
                                listId = newListId,
                                english = wObj.getString("english"),
                                arabic = wObj.getString("arabic"),
                                exampleEn = wObj.optString("exampleEn", ""),
                                exampleAr = wObj.optString("exampleAr", ""),
                                derivedEn = wObj.optString("derivedEn", ""),
                                derivedAr = wObj.optString("derivedAr", ""),
                                status = wObj.optString("status", WordStatus.UNCATEGORIZED),
                                createdAt = wObj.optLong("createdAt", System.currentTimeMillis()),
                                lastReviewedAt = lastReviewed,
                                knownCount = wObj.optInt("knownCount", 0),
                                forgottenCount = wObj.optInt("forgottenCount", 0)
                            )
                        )
                    }
                    if (wordsToInsert.isNotEmpty()) {
                        wordDao.insertWords(wordsToInsert)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
