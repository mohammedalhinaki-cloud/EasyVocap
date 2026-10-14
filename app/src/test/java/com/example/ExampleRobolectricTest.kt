package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.importer.LexiImporter
import com.example.data.model.Word
import com.example.data.model.WordStatus
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: VocabRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VocabRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LexiVocab", appName)
    }

    @Test
    fun testScenario1_100WordsClassificationAndPersistence() = runBlocking {
        // Scenario 1: Create list with 100 words, classify 80 as Saved, 20 as Forgotten.
        val listId = repository.createList("قائمة السيناريو الأول")
        val words = (1..100).map { i ->
            Word(
                listId = listId,
                english = "Word$i",
                arabic = "معنى$i",
                exampleEn = "Example sentence $i",
                exampleAr = "ترجمة المثال $i"
            )
        }
        val (inserted, _) = repository.batchInsertWords(listId, words)
        assertEquals(100, inserted)

        val directWords = repository.getWordsDirect(listId)
        assertEquals(100, directWords.size)

        // Classify 80 known, 20 forgotten
        for (i in 0 until 80) {
            repository.classifyWord(directWords[i].id, isKnown = true)
        }
        for (i in 80 until 100) {
            repository.classifyWord(directWords[i].id, isKnown = false)
        }

        // Verify stats
        val stats = repository.getListWithStatsById(listId).first()
        assertNotNull(stats)
        assertEquals(100, stats!!.totalCount)
        assertEquals(80, stats.savedCount)
        assertEquals(20, stats.forgottenCount)
        assertEquals(0, stats.uncategorizedCount)
        assertEquals(80f, stats.progressPercent, 0.01f)
    }

    @Test
    fun testScenario2_RetestForgottenWords() = runBlocking {
        // Scenario 2: Retest forgotten words, 10 become known -> forgotten becomes 10, saved becomes 90
        val listId = repository.createList("قائمة السيناريو الثاني")
        val words = (1..100).map { i ->
            Word(listId = listId, english = "Term$i", arabic = "معنى$i")
        }
        repository.batchInsertWords(listId, words)
        val allWords = repository.getWordsDirect(listId)

        // 80 saved, 20 forgotten
        allWords.take(80).forEach { repository.classifyWord(it.id, isKnown = true) }
        allWords.drop(80).forEach { repository.classifyWord(it.id, isKnown = false) }

        // Now test forgotten words only: reclassify 10 of them as known
        val forgottenBefore = repository.getWordsDirectByStatus(listId, WordStatus.FORGOTTEN)
        assertEquals(20, forgottenBefore.size)

        forgottenBefore.take(10).forEach { repository.classifyWord(it.id, isKnown = true) }

        val stats = repository.getListWithStatsById(listId).first()
        assertNotNull(stats)
        assertEquals(90, stats!!.savedCount)
        assertEquals(10, stats.forgottenCount)
    }

    @Test
    fun testScenario7_ResetProgress() = runBlocking {
        // Scenario 7: Reset list progress clears classification and stats without deleting words
        val listId1 = repository.createList("القائمة الأولى")
        val listId2 = repository.createList("القائمة الثانية المستقلة")

        repository.addWord(Word(listId = listId1, english = "Achieve", arabic = "يحقق"))
        repository.addWord(Word(listId = listId1, english = "Persist", arabic = "يواظب"))
        repository.addWord(Word(listId = listId2, english = "Apple", arabic = "تفاحة"))

        val words1 = repository.getWordsDirect(listId1)
        repository.classifyWord(words1[0].id, isKnown = true)
        repository.classifyWord(words1[1].id, isKnown = false)

        val words2 = repository.getWordsDirect(listId2)
        repository.classifyWord(words2[0].id, isKnown = true)

        // Reset progress of List 1
        repository.resetListProgress(listId1)

        val stats1 = repository.getListWithStatsById(listId1).first()
        assertEquals(2, stats1!!.totalCount)
        assertEquals(0, stats1.savedCount)
        assertEquals(0, stats1.forgottenCount)
        assertEquals(2, stats1.uncategorizedCount)

        // List 2 must be completely unaffected
        val stats2 = repository.getListWithStatsById(listId2).first()
        assertEquals(1, stats2!!.totalCount)
        assertEquals(1, stats2.savedCount)
    }

    @Test
    fun testScenario8_BackupAndRestore() = runBlocking {
        // Scenario 8: Full backup export and restore preserves lists and word states
        val listId = repository.createList("قائمة النسخ الاحتياطي")
        val w1 = repository.addWord(Word(listId = listId, english = "Explore", arabic = "يستكشف", exampleEn = "Explore new worlds", exampleAr = "استكشف عوالم جديدة"))
        repository.classifyWord(w1, isKnown = true)

        val backupJson = repository.exportFullBackupJson()
        assertTrue(backupJson.contains("Explore"))
        assertTrue(backupJson.contains("يستكشف"))

        // Create new clean in-memory database to simulate restore
        val context = ApplicationProvider.getApplicationContext<Context>()
        val newDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val newRepo = VocabRepository(newDb)

        val restored = newRepo.restoreFullBackupJson(backupJson)
        assertTrue(restored)

        val restoredLists = newRepo.listsWithStats.first()
        assertEquals(1, restoredLists.size)
        assertEquals("قائمة النسخ الاحتياطي", restoredLists[0].name)
        assertEquals(1, restoredLists[0].savedCount)
        newDb.close()
    }

    @Test
    fun testCsvParserAndMapper() {
        val csvLine = "\"Achieve\",\"يحقق\",\"You can achieve goals\",\"يمكنك تحقيق أهدافك\""
        val parsed = LexiImporter.parseCsvLine(csvLine, ',')
        assertEquals(4, parsed.size)
        assertEquals("Achieve", parsed[0])
        assertEquals("يحقق", parsed[1])
        assertEquals("You can achieve goals", parsed[2])
        assertEquals("يمكنك تحقيق أهدافك", parsed[3])
    }
}
