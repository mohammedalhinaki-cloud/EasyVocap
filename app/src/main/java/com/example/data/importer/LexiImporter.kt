package com.example.data.importer

import android.content.Context
import android.net.Uri
import com.example.data.model.Word
import com.example.data.model.WordStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ParsedRow(
    val english: String,
    val arabic: String,
    val exampleEn: String = "",
    val exampleAr: String = "",
    val derivedEn: String = "",
    val derivedAr: String = ""
)

data class ImportPreviewData(
    val fileName: String,
    val rawRows: List<List<String>>,
    val headers: List<String>,
    val totalRowsFound: Int,
    val detectedDelimiter: Char,
    val isLxfPackage: Boolean = false,
    val lxfNote: String? = null
)

object LexiImporter {

    /**
     * Reads and parses a file from Uri into structured raw rows for preview and mapping.
     */
    suspend fun analyzeFile(context: Context, uri: Uri, fileName: String): ImportPreviewData = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("تعذر قراءة الملف المختار")

        // 1. Check if file is a ZIP archive (LXF or zipped data)
        if (isZipFile(bytes)) {
            return@withContext analyzeZipOrLxf(bytes, fileName)
        }

        // 2. Decode text bytes with charset detection (UTF-8 with BOM, UTF-8, UTF-16, Windows-1256)
        val text = decodeText(bytes)
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.isEmpty()) {
            throw IllegalArgumentException("الملف فارغ ولا يحتوي على أي أسطر")
        }

        // Detect delimiter based on top 10 lines
        val delimiter = detectDelimiter(lines.take(10))
        val parsedRows = lines.map { parseCsvLine(it, delimiter) }
            .filter { it.isNotEmpty() && it.any { cell -> cell.isNotBlank() } }

        val hasHeader = checkIfFirstRowIsHeader(parsedRows.firstOrNull())
        val headers = if (hasHeader && parsedRows.isNotEmpty()) {
            parsedRows.first()
        } else {
            val maxCols = parsedRows.take(10).maxOfOrNull { it.size } ?: 2
            (1..maxCols).map { "العمود $it" }
        }

        val dataRows = if (hasHeader) parsedRows.drop(1) else parsedRows

        ImportPreviewData(
            fileName = fileName,
            rawRows = dataRows.take(15),
            headers = headers,
            totalRowsFound = dataRows.size,
            detectedDelimiter = delimiter,
            isLxfPackage = false,
            lxfNote = null
        )
    }

    private fun isZipFile(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    private fun analyzeZipOrLxf(bytes: ByteArray, fileName: String): ImportPreviewData {
        val entryNames = mutableListOf<String>()
        var csvContent: String? = null
        var foundDb = false

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                val lower = entry.name.lowercase()
                if (lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt")) {
                    val reader = BufferedReader(InputStreamReader(zis, StandardCharsets.UTF_8))
                    csvContent = reader.readText()
                } else if (lower.endsWith(".db") || lower.endsWith(".sqlite")) {
                    foundDb = true
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (csvContent != null) {
            val lines = csvContent!!.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            val delimiter = detectDelimiter(lines.take(10))
            val parsedRows = lines.map { parseCsvLine(it, delimiter) }
            val hasHeader = checkIfFirstRowIsHeader(parsedRows.firstOrNull())
            val headers = if (hasHeader && parsedRows.isNotEmpty()) parsedRows.first() else listOf("العمود 1", "العمود 2")
            val dataRows = if (hasHeader) parsedRows.drop(1) else parsedRows

            return ImportPreviewData(
                fileName = fileName,
                rawRows = dataRows.take(15),
                headers = headers,
                totalRowsFound = dataRows.size,
                detectedDelimiter = delimiter,
                isLxfPackage = true,
                lxfNote = "تم العثور على محتوى CSV متوافق داخل حزمة LXF: (${entryNames.joinToString()})"
            )
        }

        // If it's a binary SQLite LXF package or proprietary
        return ImportPreviewData(
            fileName = fileName,
            rawRows = emptyList(),
            headers = listOf("English", "Arabic"),
            totalRowsFound = 0,
            detectedDelimiter = ',',
            isLxfPackage = true,
            lxfNote = "الملف حزمة Lexilize (LXF) تحتوي على ملفات: [${entryNames.joinToString()}]. يُفضّل تصدير الكلمات من Lexilize بصيغة CSV أو مشاركة ملف LXF عينة غير مشفر لتحليله مباشرة."
        )
    }

    private fun decodeText(bytes: ByteArray): String {
        // Handle UTF-8 BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        // Handle UTF-16 LE BOM
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }
        // Handle UTF-16 BE BOM
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }

        // Try UTF-8 first
        val utf8String = String(bytes, StandardCharsets.UTF_8)
        if (!utf8String.contains("\uFFFD")) {
            return utf8String
        }

        // Fallback to Windows-1256 (common for legacy Arabic text files)
        return try {
            String(bytes, Charset.forName("windows-1256"))
        } catch (e: Exception) {
            utf8String
        }
    }

    private fun detectDelimiter(sampleLines: List<String>): Char {
        val delimiters = listOf(',', '\t', ';', '|')
        val counts = mutableMapOf<Char, Int>()
        for (d in delimiters) {
            val occurrences = sampleLines.sumOf { line -> line.count { it == d } }
            counts[d] = occurrences
        }
        val best = counts.maxByOrNull { it.value }
        return if (best != null && best.value > 0) best.key else ','
    }

    private fun checkIfFirstRowIsHeader(row: List<String>?): Boolean {
        if (row == null || row.isEmpty()) return false
        val headerKeywords = listOf(
            "english", "word", "term", "front", "en", "الكلمة", "الإنجليزية",
            "arabic", "translation", "meaning", "back", "ar", "المعنى", "العربية",
            "example", "مثال", "derived", "مشتق"
        )
        return row.any { cell ->
            val clean = cell.lowercase().trim()
            headerKeywords.any { clean.contains(it) }
        }
    }

    fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    /**
     * Map rows into Word objects based on user-selected column indexes.
     */
    fun mapRowsToWords(
        rawRows: List<List<String>>,
        listId: Long,
        enColIndex: Int,
        arColIndex: Int,
        exampleEnIndex: Int = -1,
        exampleArIndex: Int = -1,
        derivedEnIndex: Int = -1,
        derivedArIndex: Int = -1
    ): List<Word> {
        val words = mutableListOf<Word>()
        for (row in rawRows) {
            val english = row.getOrNull(enColIndex)?.trim() ?: ""
            val arabic = row.getOrNull(arColIndex)?.trim() ?: ""

            if (english.isBlank() || arabic.isBlank()) continue

            val exEn = if (exampleEnIndex >= 0) row.getOrNull(exampleEnIndex)?.trim() ?: "" else ""
            val exAr = if (exampleArIndex >= 0) row.getOrNull(exampleArIndex)?.trim() ?: "" else ""
            val derEn = if (derivedEnIndex >= 0) row.getOrNull(derivedEnIndex)?.trim() ?: "" else ""
            val derAr = if (derivedArIndex >= 0) row.getOrNull(derivedArIndex)?.trim() ?: "" else ""

            words.add(
                Word(
                    listId = listId,
                    english = english,
                    arabic = arabic,
                    exampleEn = exEn,
                    exampleAr = exAr,
                    derivedEn = derEn,
                    derivedAr = derAr,
                    status = WordStatus.UNCATEGORIZED
                )
            )
        }
        return words
    }
}
