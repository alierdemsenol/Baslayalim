package com.example.util

import android.content.Context
import android.os.Build
import android.util.Base64
import com.example.BuildConfig
import com.example.data.preferences.UserPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater

/**
 * Privacy & Security Preserving Diagnostic Logger.
 *
 * Exposes NEITHER user personal/study data (name, notes, subjects, agenda, IP, sync keys, targets)
 * NOR internal app identifiers, screen names, class paths, or architecture details in plaintext.
 * Stores compact event codes and generates a compressed/encoded diagnostic ticket (`DIAG_V1`)
 * that only the developer can decode using the event code table.
 */
object AppActionLogger {

    private const val LOG_FILE_NAME = "diag_events_ring.dat"
    private const val REPORTS_DIR = "reports"
    private const val MAX_LOG_LINES = 200
    private val lock = Any()

    @Volatile
    private var crashHandlerInstalled = false

    fun installCrashHandler(context: Context) {
        if (crashHandlerInstalled) return
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val topFrame = throwable.stackTrace.firstOrNull {
                    it.className.startsWith("com.example")
                } ?: throwable.stackTrace.firstOrNull()
                val frameTag = if (topFrame != null) {
                    val clsHash = Integer.toHexString(topFrame.className.hashCode()).takeLast(4)
                    "$clsHash:${topFrame.lineNumber}"
                } else {
                    "0:0"
                }
                val excHash = Integer.toHexString(throwable.javaClass.simpleName.hashCode()).takeLast(4)
                logError(appContext, "CRASH", "F_${excHash}_${frameTag}_${thread.name.take(8)}")
            } catch (_: Throwable) {
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
        crashHandlerInstalled = true
    }

    fun logAction(context: Context, category: String, detail: String) {
        val code = encodeOperation(category, detail, isError = false)
        appendCode(context.applicationContext, code)
    }

    fun logError(context: Context, category: String, detail: String) {
        val code = encodeOperation(category, detail, isError = true)
        appendCode(context.applicationContext, code)
    }

    private fun encodeOperation(category: String, detail: String, isError: Boolean): String {
        val cat = category.uppercase(Locale.US)
        val det = detail.lowercase(Locale.US)
        val opCode = when {
            isError && cat == "CRASH" -> "X99:${det.take(24)}"
            isError -> "X_${cat.take(3)}_${Integer.toHexString(det.hashCode()).takeLast(4)}"
            cat == "LIFECYCLE" -> "L01"
            cat == "NAV" && det.contains("settings") -> "N04"
            cat == "NAV" && det.contains("agenda") -> "N05"
            cat == "NAV" && det.contains("main") -> "N01"
            cat == "NAV" && det.contains("timer") -> "N02"
            cat == "NAV" && det.contains("stopwatch") -> "N03"
            cat == "POMODORO" && det.contains("start_work") -> "P01"
            cat == "POMODORO" && det.contains("pause") -> "P02"
            cat == "POMODORO" && det.contains("resume_work") -> "P03"
            cat == "POMODORO" && det.contains("start_break") -> "P04"
            cat == "POMODORO" && det.contains("resume_break") -> "P05"
            cat == "POMODORO" && det.contains("block_done") -> "P06"
            cat == "POMODORO" && det.contains("stop_save") -> "P07"
            cat == "POMODORO" && det.contains("firar") -> "P08"
            cat == "POMODORO" && det.contains("reset") -> "P09"
            cat == "TIMER" && det.contains("start") -> "T01"
            cat == "TIMER" && det.contains("pause") -> "T02"
            cat == "TIMER" && det.contains("completed") -> "T03"
            cat == "TIMER" && det.contains("firar") -> "T04"
            cat == "TIMER" && det.contains("reset") -> "T05"
            cat == "STOPWATCH" && det.contains("start") -> "S01"
            cat == "STOPWATCH" && det.contains("pause") -> "S02"
            cat == "STOPWATCH" && det.contains("lap") -> "S03"
            cat == "STOPWATCH" && det.contains("saved") -> "S04"
            cat == "STOPWATCH" && det.contains("reset") -> "S05"
            cat == "NOTIF" -> "F01"
            cat == "AGENDA" && det.contains("save") -> "A01"
            cat == "AGENDA" && det.contains("delete") -> "A02"
            cat == "BACKUP" && det.contains("export") -> "B01"
            cat == "BACKUP" && det.contains("restore") -> "B02"
            cat == "SYNC" -> "Y01"
            cat == "WIDGET" -> "W01"
            cat == "SETTINGS" -> "C01"
            cat == "PROFILE" -> "C02"
            cat == "DATA" -> "D01"
            cat == "REPORT" -> "R01"
            else -> "G_${Integer.toHexString((cat + det).hashCode()).takeLast(4)}"
        }
        val tsSec = System.currentTimeMillis() / 1000L
        return "$tsSec:$opCode"
    }

    private fun appendCode(context: Context, codedEntry: String) {
        synchronized(lock) {
            try {
                val file = File(context.filesDir, LOG_FILE_NAME)
                val existing = if (file.exists()) file.readLines() else emptyList()
                val trimmed = (existing + codedEntry).takeLast(MAX_LOG_LINES)
                file.writeText(trimmed.joinToString("\n") + "\n")
            } catch (_: Throwable) {
            }
        }
    }

    fun buildPrivacySafeReport(context: Context): String {
        val isTr = UserPreferences.resolveSystemLanguage() == "TR"
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val rawEvents = synchronized(lock) {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()
        }

        val errCount = rawEvents.count { it.contains(":X") }
        val payloadObj = JSONObject().apply {
            put("fmt", 1)
            put("ver", BuildConfig.VERSION_NAME)
            put("vc", BuildConfig.VERSION_CODE)
            put("sdk", Build.VERSION.SDK_INT)
            put("os", Build.VERSION.RELEASE)
            put("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
            put("ts", System.currentTimeMillis() / 1000L)
            put("errCount", errCount)
            val arr = JSONArray()
            rawEvents.takeLast(120).forEach { arr.put(it) }
            put("seq", arr)
        }

        val encodedToken = compressAndEncode(payloadObj.toString())

        val sb = StringBuilder()
        sb.appendLine("----------------------------------------------------")
        sb.appendLine(if (isTr) "TANILAMA HATA RAPORU" else "DIAGNOSTIC BUG REPORT")
        sb.appendLine("----------------------------------------------------")
        sb.appendLine(
            if (isTr) {
                "Gizlilik ve Guvenlik: Bu rapor kisisel verilerinizi, calisma gecmisinizi, ajanda kayitlarinizi ve uygulama ici yapiyi acik etmez. Yalnizca gelistiricinin cozebilecegi kodlanmis sistem tanilama paketi icerir."
            } else {
                "Privacy & Security: This report exposes neither your personal study/agenda data nor internal app architecture. It contains only a developer-decodable diagnostic packet."
            }
        )
        sb.appendLine()
        sb.appendLine("Tarih/Time: $nowStr | Ver: ${BuildConfig.VERSION_NAME} | SDK: ${Build.VERSION.SDK_INT} | Err: $errCount")
        sb.appendLine()
        sb.appendLine("-----BEGIN DIAG_TOKEN_V1-----")
        encodedToken.chunked(64).forEach { sb.appendLine(it) }
        sb.appendLine("-----END DIAG_TOKEN_V1-----")
        return sb.toString()
    }

    private fun compressAndEncode(plain: String): String {
        return try {
            val input = plain.toByteArray(StandardCharsets.UTF_8)
            val deflater = Deflater(Deflater.BEST_COMPRESSION)
            deflater.setInput(input)
            deflater.finish()
            val out = ByteArrayOutputStream()
            val buf = ByteArray(512)
            while (!deflater.finished()) {
                val count = deflater.deflate(buf)
                out.write(buf, 0, count)
            }
            deflater.end()
            val compressed = out.toByteArray()
            // Light XOR obfuscation key so casual readers cannot inspect plain JSON strings even if unzipped
            val mask = byteArrayOf(0x5A, 0x3C, 0x71, 0x19)
            for (i in compressed.indices) {
                compressed[i] = (compressed[i].toInt() xor mask[i % mask.size].toInt()).toByte()
            }
            Base64.encodeToString(compressed, Base64.NO_WRAP)
        } catch (_: Throwable) {
            Base64.encodeToString(plain.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }

    fun createReportFile(context: Context, reportContent: String): File? {
        return try {
            val dir = File(context.filesDir, REPORTS_DIR)
            if (!dir.exists()) dir.mkdirs()
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(4)?.forEach {
                try { it.delete() } catch (_: Exception) {}
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(dir, "diag_report_$stamp.txt")
            outFile.writeText(reportContent)
            outFile
        } catch (_: Throwable) {
            null
        }
    }
}
