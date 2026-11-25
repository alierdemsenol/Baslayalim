package com.example.data.model

import com.example.util.TranslationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class SavedPeerDevice(
    val id: String,
    val brand: String,
    val model: String = "",
    val deviceCode: String = "",
    val username: String = "",
    val ip: String,
    val port: Int = 8888,
    val role: String = "STUDENT",
    val lastSeenAt: Long = System.currentTimeMillis()
) {
    fun hardwareLabel(): String {
        val b = brand.trim()
        val m = model.trim()
        return when {
            b.isNotEmpty() && m.isNotEmpty() && !m.equals(b, ignoreCase = true) -> "$b $m"
            m.isNotEmpty() -> m
            b.isNotEmpty() -> b
            else -> "Android"
        }
    }

    fun compactSummary(): String {
        val hw = hardwareLabel()
        return if (deviceCode.isNotBlank()) "$hw • #$deviceCode" else hw
    }

    companion object {
        fun encodeList(items: List<SavedPeerDevice>): String {
            val arr = JSONArray()
            items.forEach { d ->
                arr.put(JSONObject().apply {
                    put("id", d.id)
                    put("brand", d.brand)
                    put("model", d.model)
                    put("deviceCode", d.deviceCode)
                    put("username", d.username)
                    put("ip", d.ip)
                    put("port", d.port)
                    put("role", d.role)
                    put("lastSeenAt", d.lastSeenAt)
                })
            }
            return arr.toString()
        }

        fun decodeList(raw: String): List<SavedPeerDevice> {
            if (raw.isBlank() || raw == "[]") return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val id = o.optString("id", "").trim()
                        val brand = o.optString("brand", "").trim()
                        val model = o.optString("model", "").trim()
                        val deviceCode = o.optString("deviceCode", "").trim()
                        val username = o.optString("username", "").trim()
                        val ip = o.optString("ip", "").trim()
                        if (ip.isNotEmpty() || deviceCode.isNotEmpty() || username.isNotEmpty()) {
                            add(
                                SavedPeerDevice(
                                    id = id.ifBlank { deviceCode.ifBlank { "${brand}_${model}_$ip" } },
                                    brand = brand.ifBlank { "Android" },
                                    model = model,
                                    deviceCode = deviceCode,
                                    username = username,
                                    ip = ip,
                                    port = o.optInt("port", 8888),
                                    role = o.optString("role", "STUDENT"),
                                    lastSeenAt = o.optLong("lastSeenAt", 0L)
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

data class DiscoveredPeer(
    val brand: String,
    val model: String = "",
    val deviceCode: String = "",
    val username: String = "",
    val ip: String,
    val port: Int,
    val role: String,   // "STUDENT" or "PARENT"
    val mode: String,   // "TRANSFER" or "PARENT_PAIRING"
    val lastSeenMs: Long = System.currentTimeMillis()
) {
    val stableId: String get() = deviceCode.takeIf { it.isNotBlank() } ?: "${brand.lowercase(Locale.US)}|${model.lowercase(Locale.US)}|$ip"

    fun hardwareLabel(): String {
        val b = brand.trim()
        val m = model.trim()
        return when {
            b.isNotEmpty() && m.isNotEmpty() && !m.equals(b, ignoreCase = true) -> "$b $m"
            m.isNotEmpty() -> m
            b.isNotEmpty() -> b
            else -> "Android"
        }
    }

    fun compactSummary(): String {
        val hw = hardwareLabel()
        return if (deviceCode.isNotBlank()) "$hw • #$deviceCode" else hw
    }
}

data class SavedBookEntry(
    val bookName: String,
    val examStage: String, // "TYT", "AYT", "YDT", or ""
    val subject: String,
    val lastUsedAt: Long = System.currentTimeMillis()
)

object SavedBookHelper {

    fun encodeList(items: List<SavedBookEntry>): String {
        val arr = JSONArray()
        items.forEach { b ->
            arr.put(JSONObject().apply {
                put("bookName", b.bookName)
                put("examStage", b.examStage)
                put("subject", b.subject)
                put("lastUsedAt", b.lastUsedAt)
            })
        }
        return arr.toString()
    }

    fun decodeList(raw: String): List<SavedBookEntry> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("bookName", "").trim()
                    val subj = o.optString("subject", "").trim()
                    val stage = o.optString("examStage", "").trim().uppercase(Locale.US)
                    if (name.isNotEmpty() && subj.isNotEmpty()) {
                        add(
                            SavedBookEntry(
                                bookName = name,
                                examStage = stage,
                                subject = subj,
                                lastUsedAt = o.optLong("lastUsedAt", 0L)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun normalizeSubject(subject: String): String {
        val trForm = TranslationHelper.canonicalSubject(subject).trim().lowercase(Locale.forLanguageTag("tr"))
        return trForm
    }

    private fun normalizeStage(stage: String): String {
        return stage.trim().uppercase(Locale.US)
    }

    fun upsertBook(
        existing: List<SavedBookEntry>,
        bookName: String,
        examStage: String,
        subject: String,
        timestamp: Long = System.currentTimeMillis()
    ): List<SavedBookEntry> {
        val cleanBook = bookName.trim()
        val cleanSubj = TranslationHelper.canonicalSubject(subject).trim()
        val cleanStage = normalizeStage(examStage)
        if (cleanBook.isEmpty() || cleanSubj.isEmpty()) return existing

        val normBook = cleanBook.lowercase(Locale.forLanguageTag("tr"))
        val normSubj = normalizeSubject(cleanSubj)

        val updated = existing.filterNot { entry ->
            entry.bookName.trim().lowercase(Locale.forLanguageTag("tr")) == normBook &&
                normalizeStage(entry.examStage) == cleanStage &&
                normalizeSubject(entry.subject) == normSubj
        }.toMutableList()

        updated.add(
            0,
            SavedBookEntry(
                bookName = cleanBook,
                examStage = cleanStage,
                subject = cleanSubj,
                lastUsedAt = maxOf(timestamp, existing.firstOrNull {
                    it.bookName.trim().lowercase(Locale.forLanguageTag("tr")) == normBook &&
                        normalizeStage(it.examStage) == cleanStage &&
                        normalizeSubject(it.subject) == normSubj
                }?.lastUsedAt ?: 0L)
            )
        )
        return updated.sortedByDescending { it.lastUsedAt }.take(150)
    }

    /**
     * Strictly matches BOTH examStage (e.g. TYT vs AYT) and subject (e.g. Fizik).
     * Selecting TYT + Fizik will NEVER return books saved for AYT + Fizik or TYT + Kimya.
     */
    fun filterSuggestions(
        savedBooks: List<SavedBookEntry>,
        examStage: String,
        subject: String,
        query: String = ""
    ): List<String> {
        val targetStage = normalizeStage(examStage)
        val targetSubj = normalizeSubject(subject)
        if (targetSubj.isEmpty()) return emptyList()

        val q = query.trim().lowercase(Locale.forLanguageTag("tr"))
        val matched = savedBooks.filter { entry ->
            normalizeStage(entry.examStage) == targetStage &&
                normalizeSubject(entry.subject) == targetSubj
        }
            .filter { entry ->
                q.isEmpty() || entry.bookName.lowercase(Locale.forLanguageTag("tr")).contains(q)
            }
            .sortedWith(
                compareByDescending<SavedBookEntry> {
                    if (q.isEmpty()) false
                    else it.bookName.lowercase(Locale.forLanguageTag("tr")).startsWith(q)
                }.thenByDescending { it.lastUsedAt }
            )

        val seen = HashSet<String>()
        return matched.mapNotNull { entry ->
            val key = entry.bookName.trim().lowercase(Locale.forLanguageTag("tr"))
            if (seen.add(key)) entry.bookName.trim() else null
        }
    }

    fun backfillFromNotes(
        existing: List<SavedBookEntry>,
        notes: List<DailyNote>,
        examTarget: String
    ): List<SavedBookEntry> {
        var current = existing
        notes.forEach { note ->
            note.getSubjectBullets().forEach { b ->
                val book = b.sourceBook.trim()
                if (book.isNotEmpty() && b.subject.isNotBlank()) {
                    val stage = if (examTarget == "YKS") b.examStage else ""
                    current = upsertBook(current, book, stage, b.subject, note.timestamp)
                }
            }
        }
        return current
    }
}

data class ParentStudentSnapshot(
    val studentName: String = "",
    val studentUsername: String = "",
    val studentBrand: String = "",
    val studentModel: String = "",
    val studentDeviceCode: String = "",
    val examTarget: String = "YKS",
    val userTrack: String = "Sayısal",
    val ydtLanguage: String = "İngilizce",
    val syncedAtMillis: Long = 0L,
    val customTytExamDate: Long = 0L,
    val customAytExamDate: Long = 0L,
    val customMsuExamDate: Long = 0L,
    val customLgsExamDate: Long = 0L,
    val customKpssExamDate: Long = 0L,
    val sessions: List<StudySession> = emptyList(),
    val customTimers: List<CustomTimer> = emptyList(),
    val dailyNotes: List<DailyNote> = emptyList()
) {
    fun compactDeviceInfo(): String {
        val hw = when {
            studentBrand.isNotBlank() && studentModel.isNotBlank() && !studentModel.equals(studentBrand, ignoreCase = true) ->
                "$studentBrand $studentModel"
            studentModel.isNotBlank() -> studentModel
            studentBrand.isNotBlank() -> studentBrand
            else -> "Android"
        }
        return if (studentDeviceCode.isNotBlank()) "$hw • #$studentDeviceCode" else hw
    }
}
