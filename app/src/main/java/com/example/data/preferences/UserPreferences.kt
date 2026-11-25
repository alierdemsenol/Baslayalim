package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("yks_odak_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_TRACK = "key_user_track" // "Sayısal", "Eşit Ağırlık", "Sözel", "Dil", "LGS Genel", "KPSS Genel"
        private const val KEY_EXAM_TARGET = "key_exam_target" // "YKS", "LGS", "KPSS"
        private const val KEY_AMOLED_MODE = "key_amoled_mode"
        private const val KEY_IS_ONBOARDED = "key_is_onboarded"
        private const val KEY_FIRST_USAGE_TIME = "key_first_usage_time"
        private const val KEY_TYT_EXAM_DATE = "key_tyt_exam_date"
        private const val KEY_AYT_EXAM_DATE = "key_ayt_exam_date"
        private const val KEY_LGS_EXAM_DATE = "key_lgs_exam_date"
        private const val KEY_KPSS_EXAM_DATE = "key_kpss_exam_date"
        private const val KEY_MSU_EXAM_DATE = "key_msu_exam_date"
        private const val KEY_THEME_MODE = "key_theme_mode" // "SYSTEM", "LIGHT", "DARK"
        private const val KEY_ACCENT_COLOR = "key_accent_color"
        private const val KEY_SYNC_KEY = "key_sync_key"
        private const val KEY_YDT_LANGUAGE = "key_ydt_language"
        private const val KEY_USER_ROLE = "key_user_role" // "STUDENT" or "PARENT"
        private const val KEY_NETWORK_USERNAME = "key_network_username"
        private const val KEY_PARENT_PAIRING_ENABLED = "key_parent_pairing_enabled"
        private const val KEY_SAVED_TRANSFER_DEVICES = "key_saved_transfer_devices"
        private const val KEY_SAVED_PARENT_DEVICES = "key_saved_parent_devices"
        private const val KEY_SAVED_BOOKS = "key_saved_books"
        private const val KEY_PARENT_SYNCED_PAYLOAD = "key_parent_synced_payload"
        private const val KEY_PARENT_LAST_SYNC_AT = "key_parent_last_sync_at"
        private const val KEY_DEVICE_UNIQUE_CODE = "key_device_unique_code"
        private const val KEY_PITCH_BLACK_DARK = "key_pitch_black_dark"
        private const val KEY_SELECTED_EXAM_INDEX = "key_selected_exam_index"
        private const val KEY_SELECTED_EXAM_TITLE = "key_selected_exam_title"
        private const val KEY_SELECTED_TIMER_HUB_TAB = "key_selected_timer_hub_tab"
        private const val KEY_SELECTED_MAIN_TAB = "key_selected_main_tab"

        const val ROLE_STUDENT = "STUDENT"
        const val ROLE_PARENT = "PARENT"

        fun generate5DigitDeviceCode(): String = (10000..99999).random().toString()

        fun resolveSystemLanguage(): String {
            return if (Locale.getDefault().language.equals("tr", ignoreCase = true)) "TR" else "EN"
        }

        fun defaultTytExamMillis(): Long = Calendar.getInstance().apply {
            set(2027, Calendar.JUNE, 19, 10, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun defaultAytExamMillis(): Long = Calendar.getInstance().apply {
            set(2027, Calendar.JUNE, 20, 10, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun defaultMsuExamMillis(): Long = Calendar.getInstance().apply {
            set(2027, Calendar.MARCH, 20, 10, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun defaultLgsExamMillis(): Long = Calendar.getInstance().apply {
            set(2027, Calendar.JUNE, 6, 9, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun defaultKpssExamMillis(): Long = Calendar.getInstance().apply {
            set(2027, Calendar.JULY, 18, 10, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Automatically resolves from device system locale: TR for Turkish devices, EN for all others
    var userLanguage: String
        get() = resolveSystemLanguage()
        set(_) { /* Strictly bound to device system language (TR or EN) */ }

    var userRole: String
        get() {
            val raw = prefs.getString(KEY_USER_ROLE, ROLE_STUDENT) ?: ROLE_STUDENT
            return if (raw == ROLE_PARENT) ROLE_PARENT else ROLE_STUDENT
        }
        set(value) {
            val clean = if (value == ROLE_PARENT) ROLE_PARENT else ROLE_STUDENT
            prefs.edit().putString(KEY_USER_ROLE, clean).commit()
        }

    var networkUsername: String
        get() = prefs.getString(KEY_USER_NAME, "")?.trim().orEmpty()
        set(value) = prefs.edit().putString(KEY_NETWORK_USERNAME, value.trim()).apply()

    var isParentPairingEnabled: Boolean
        get() = prefs.getBoolean(KEY_PARENT_PAIRING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PARENT_PAIRING_ENABLED, value).apply()

    var savedTransferDevicesJson: String
        get() = prefs.getString(KEY_SAVED_TRANSFER_DEVICES, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_SAVED_TRANSFER_DEVICES, value).apply()

    var savedParentDevicesJson: String
        get() = prefs.getString(KEY_SAVED_PARENT_DEVICES, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_SAVED_PARENT_DEVICES, value).apply()

    var savedBooksJson: String
        get() = prefs.getString(KEY_SAVED_BOOKS, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_SAVED_BOOKS, value).apply()

    var parentSyncedPayloadJson: String
        get() = prefs.getString(KEY_PARENT_SYNCED_PAYLOAD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PARENT_SYNCED_PAYLOAD, value).apply()

    var parentLastSyncAt: Long
        get() = prefs.getLong(KEY_PARENT_LAST_SYNC_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_PARENT_LAST_SYNC_AT, value).apply()

    var appThemeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).commit()
        }

    var appAccentColorHex: String
        get() = prefs.getString(KEY_ACCENT_COLOR, "#FF2D75") ?: "#FF2D75"
        set(value) {
            prefs.edit().putString(KEY_ACCENT_COLOR, value).commit()
        }

    var syncKey: String
        get() {
            var key = prefs.getString(KEY_SYNC_KEY, "") ?: ""
            if (key.length != 4 || !key.all { it.isDigit() }) {
                key = (1000..9999).random().toString()
                prefs.edit().putString(KEY_SYNC_KEY, key).apply()
            }
            return key
        }
        set(value) = prefs.edit().putString(KEY_SYNC_KEY, value).apply()

    var customTytExamDate: Long
        get() = prefs.getLong(KEY_TYT_EXAM_DATE, defaultTytExamMillis())
        set(value) = prefs.edit().putLong(KEY_TYT_EXAM_DATE, value).apply()

    var customAytExamDate: Long
        get() = prefs.getLong(KEY_AYT_EXAM_DATE, defaultAytExamMillis())
        set(value) = prefs.edit().putLong(KEY_AYT_EXAM_DATE, value).apply()

    var customLgsExamDate: Long
        get() = prefs.getLong(KEY_LGS_EXAM_DATE, defaultLgsExamMillis())
        set(value) = prefs.edit().putLong(KEY_LGS_EXAM_DATE, value).apply()

    var customKpssExamDate: Long
        get() = prefs.getLong(KEY_KPSS_EXAM_DATE, defaultKpssExamMillis())
        set(value) = prefs.edit().putLong(KEY_KPSS_EXAM_DATE, value).apply()

    var customMsuExamDate: Long
        get() = prefs.getLong(KEY_MSU_EXAM_DATE, defaultMsuExamMillis())
        set(value) = prefs.edit().putLong(KEY_MSU_EXAM_DATE, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userTrack: String
        get() = prefs.getString(KEY_USER_TRACK, "Sayısal") ?: "Sayısal"
        set(value) = prefs.edit().putString(KEY_USER_TRACK, value).apply()

    var examTarget: String
        get() = prefs.getString(KEY_EXAM_TARGET, "YKS") ?: "YKS"
        set(value) = prefs.edit().putString(KEY_EXAM_TARGET, value).apply()

    var ydtLanguage: String
        get() = prefs.getString(KEY_YDT_LANGUAGE, "İngilizce") ?: "İngilizce"
        set(value) = prefs.edit().putString(KEY_YDT_LANGUAGE, value).apply()

    var deviceUniqueCode: String
        get() {
            var code = prefs.getString(KEY_DEVICE_UNIQUE_CODE, "") ?: ""
            if (code.length != 5 || !code.all { it.isDigit() }) {
                code = generate5DigitDeviceCode()
                prefs.edit().putString(KEY_DEVICE_UNIQUE_CODE, code).apply()
            }
            return code
        }
        set(value) {
            val clean = value.trim()
            if (clean.length == 5 && clean.all { it.isDigit() }) {
                prefs.edit().putString(KEY_DEVICE_UNIQUE_CODE, clean).apply()
            }
        }

    var isPitchBlackDarkEnabled: Boolean
        get() = prefs.getBoolean(KEY_PITCH_BLACK_DARK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PITCH_BLACK_DARK, value).commit()
        }

    var selectedDashboardExamIndex: Int
        get() = prefs.getInt(KEY_SELECTED_EXAM_INDEX, 0).coerceAtLeast(0)
        set(value) {
            prefs.edit().putInt(KEY_SELECTED_EXAM_INDEX, value.coerceAtLeast(0)).commit()
        }

    var selectedDashboardExamTitle: String
        get() = prefs.getString(KEY_SELECTED_EXAM_TITLE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SELECTED_EXAM_TITLE, value).commit()
        }

    var selectedTimerHubTab: Int
        get() = prefs.getInt(KEY_SELECTED_TIMER_HUB_TAB, 0).coerceIn(0, 1)
        set(value) {
            prefs.edit().putInt(KEY_SELECTED_TIMER_HUB_TAB, value.coerceIn(0, 1)).commit()
        }

    var selectedMainTab: Int
        get() = prefs.getInt(KEY_SELECTED_MAIN_TAB, 0).coerceIn(0, 2)
        set(value) {
            prefs.edit().putInt(KEY_SELECTED_MAIN_TAB, value.coerceIn(0, 2)).commit()
        }

    var isAmoledModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_AMOLED_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_AMOLED_MODE, value).apply()

    var isOnboarded: Boolean
        get() = prefs.getBoolean(KEY_IS_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ONBOARDED, value).apply()

    var firstUsageTimestamp: Long
        get() {
            var time = prefs.getLong(KEY_FIRST_USAGE_TIME, 0L)
            if (time == 0L) {
                time = System.currentTimeMillis()
                prefs.edit().putLong(KEY_FIRST_USAGE_TIME, time).apply()
            }
            return time
        }
        set(value) = prefs.edit().putLong(KEY_FIRST_USAGE_TIME, value).apply()

    val usageDaysCount: Int
        get() {
            val startTime = firstUsageTimestamp
            val startCal = Calendar.getInstance().apply {
                timeInMillis = startTime
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nowCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMs = (nowCal.timeInMillis - startCal.timeInMillis).coerceAtLeast(0L)
            val days = (diffMs / (1000 * 60 * 60 * 24L)).toInt() + 1
            return days.coerceAtLeast(1)
        }

    fun clear() {
        prefs.edit().clear().commit()
    }
}
