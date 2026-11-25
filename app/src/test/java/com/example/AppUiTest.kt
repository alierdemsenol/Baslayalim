package com.example

import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Looper
import android.widget.Toast
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.AgendaSubjectBullet
import com.example.data.model.DailyNote
import com.example.data.model.SavedBookEntry
import com.example.data.model.SavedBookHelper
import com.example.data.model.StudySession
import com.example.data.model.StudyStatsCalculator
import com.example.data.preferences.UserPreferences
import com.example.data.repository.StudyRepository
import com.example.service.DailyReminderReceiver
import com.example.ui.viewmodel.PomodoroState
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.ActiveTimerManager
import com.example.util.AppActionLogger
import com.example.util.BackupHelper
import com.example.util.LocalSyncHelper
import com.example.util.NotificationHelper
import com.example.util.TranslationHelper
import com.example.widget.AllExamsWidgetProvider
import com.example.widget.ExamCountdownWidgetProvider
import com.example.widget.ExamWidgetUpdater
import com.example.widget.StopwatchWidgetProvider
import com.example.widget.StudyStatsWidgetProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun completeOnboarding(name: String = "Ali", track: String = "Sayısal", target: String = "YKS") {
        composeTestRule.runOnUiThread {
            val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
            vm.clearFocusLock()
            vm.selectMainTab(0)
            vm.selectTimerHubTab(0)
            vm.saveUserProfile(name, track, target, role = UserPreferences.ROLE_STUDENT)
            vm.syncSystemLanguage()
        }
        composeTestRule.waitForIdle()
        if (composeTestRule.onAllNodes(hasTestTag("nav_tab_main")).fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("nav_tab_main").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun scrollVerticalListToTag(tag: String) {
        val scrollMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        val nodes = composeTestRule.onAllNodes(scrollMatcher).fetchSemanticsNodes()
        if (nodes.size <= 1) {
            composeTestRule.onNode(scrollMatcher).performScrollToNode(hasTestTag(tag))
        } else {
            val targetIdx = nodes.indexOfFirst {
                it.boundsInRoot.left in -10f..40f && it.boundsInRoot.width > 100f
            }.coerceAtLeast(0)
            composeTestRule.onAllNodes(scrollMatcher)[targetIdx].performScrollToNode(hasTestTag(tag))
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Başlayalım", appName)
    }

    @Test
    fun `main activity launches onboarding without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        UserPreferences(context).clear()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `countdown formatter uses safak only for msu and gun kaldi for all other exams`() {
        assertEquals("Şafak 119", ExamWidgetUpdater.formatCountdownText("MSÜ 2027", 119L, isTr = true))
        assertEquals("211 gün kaldı", ExamWidgetUpdater.formatCountdownText("YKS (TYT) 2027", 211L, isTr = true))
        assertEquals("212 gün kaldı", ExamWidgetUpdater.formatCountdownText("YKS (AYT) 2027", 212L, isTr = true))
        assertEquals("198 gün kaldı", ExamWidgetUpdater.formatCountdownText("LGS 2027", 198L, isTr = true))
        assertEquals("240 gün kaldı", ExamWidgetUpdater.formatCountdownText("KPSS Lisans 2027", 240L, isTr = true))
        assertEquals("30 gün kaldı", ExamWidgetUpdater.formatCountdownText("Deneme", 30L, isTr = true))
        assertEquals("211 days left", ExamWidgetUpdater.formatCountdownText("YKS (TYT) 2027", 211L, isTr = false))
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `home tab is named Ana Sayfa and compact agenda and stats subtitle visibility behave properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            val db = AppDatabase.getDatabase(context)
            db.studySessionDao().clearAllSessions()
            db.dailyNoteDao().clearAllNotes()
        }
        completeOnboarding("Ali", "Sayısal", "YKS")

        assertEquals("Ana Sayfa", TranslationHelper.getTranslation("tab_main", "TR"))
        composeTestRule.onNodeWithTag("nav_tab_main").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ana Sayfa").assertIsDisplayed()

        scrollVerticalListToTag("dashboard_ajandam_card")
        composeTestRule.onNodeWithTag("dashboard_ajandam_card").assertIsDisplayed()
        val agendaCardText = composeTestRule.onNodeWithTag("dashboard_ajandam_card")
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString(" ")
        assertTrue(agendaCardText.contains("Ajandam"))
        assertFalse(agendaCardText.contains("Henüz ekleme yapılmadı"))
        assertFalse(agendaCardText.contains("Tarih seç", ignoreCase = true))
        assertFalse(agendaCardText.contains("Soru çözümü", ignoreCase = true))
        assertFalse(agendaCardText.contains("Tekrar", ignoreCase = true))

        scrollVerticalListToTag("dashboard_stats_card")
        composeTestRule.onNodeWithTag("dashboard_stats_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tüm zamanlayıcı ve ajanda kayıtlarından hesaplanır").assertIsDisplayed()

        // Open Agenda and verify Kayıtlı Günler is collapsed behind toggle arrow until clicked
        scrollVerticalListToTag("dashboard_ajandam_card")
        composeTestRule.onNodeWithTag("dashboard_ajandam_card").performClick()
        composeTestRule.waitForIdle()
        scrollVerticalListToTag("agenda_recent_toggle")
        composeTestRule.onNodeWithTag("agenda_recent_toggle").assertIsDisplayed()
        val collapsedEmptyNodes = composeTestRule.onAllNodesWithText("Henüz kayıtlı bir ajanda günü bulunmuyor.").fetchSemanticsNodes().size
        assertEquals(0, collapsedEmptyNodes)

        composeTestRule.onNodeWithTag("agenda_recent_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Henüz kayıtlı bir ajanda günü bulunmuyor.").assertIsDisplayed()
    }

    @Test
    fun `paused stopwatch renders visible delete trash button next to save`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        completeOnboarding("Test", "Sayısal", "YKS")

        ActiveTimerManager.startStopwatch(context, "Matematik")
        ActiveTimerManager.pauseStopwatch(context)

        composeTestRule.onNodeWithTag("nav_tab_stopwatch").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stopwatch_save_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stopwatch_delete_btn").assertIsDisplayed()

        ActiveTimerManager.resetStopwatch(context)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `pomodoro starts directly without silent mode warning dialog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ActiveTimerManager.resetPomodoro(context)
        completeOnboarding("Zeynep", "Eşit Ağırlık", "YKS")

        composeTestRule.onNodeWithTag("nav_tab_timer_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timer_hub_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pomo_start_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodesWithText("sessiz", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("silent", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        assertEquals(PomodoroState.WORKING, vm.pomodoroState.value)

        ActiveTimerManager.resetPomodoro(context)
    }

    @Test
    fun `repository insertSession auto-records into agenda without overwriting existing note or double counting`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = StudyRepository(db.studySessionDao(), db.customTimerDao(), db.dailyNoteDao())
            val dateStr = "2026-10-15"

            repo.saveAgendaEntry(
                dateStr = dateStr,
                studyDurationMinutes = 10,
                subjectBullets = listOf(
                    AgendaSubjectBullet(
                        subject = "Matematik",
                        topic = "Türev",
                        studyType = AgendaSubjectBullet.STUDY_TYPE_PRACTICE,
                        testCount = 4
                    )
                ),
                noteText = "Bugün verimli geçti.",
                mood = "😀"
            )

            val sessionTs = 1760526000000L
            repo.insertSession(
                StudySession(
                    subject = "Fizik",
                    durationSeconds = 1500L,
                    sessionType = "POMODORO",
                    blocksCompleted = 1,
                    timestamp = sessionTs,
                    dateFormatted = dateStr
                )
            )

            val note = db.dailyNoteDao().getNoteForDateOnce(dateStr)
            assertNotNull(note)
            assertEquals("Bugün verimli geçti.", note!!.noteText)
            assertEquals(25, note.studyDurationMinutes)
            val bullets = note.getSubjectBullets()
            assertEquals(2, bullets.size)
            assertEquals(AgendaSubjectBullet.STUDY_TYPE_PRACTICE, bullets[0].studyType)
            assertEquals(4, bullets[0].testCount)
            assertEquals(AgendaSubjectBullet.STUDY_TYPE_TIMER, bullets[1].studyType)
            assertEquals("Fizik", bullets[1].subject)

            StudyRepository.appendSessionToAgenda(
                db.studySessionDao(),
                db.dailyNoteDao(),
                StudySession(
                    subject = "Fizik",
                    durationSeconds = 1500L,
                    sessionType = "POMODORO",
                    blocksCompleted = 1,
                    timestamp = sessionTs,
                    dateFormatted = dateStr
                )
            )
            val afterDup = db.dailyNoteDao().getNoteForDateOnce(dateStr)!!
            assertEquals(2, afterDup.getSubjectBullets().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `legacy backup restores cleanly and backfills agenda while v5 roundtrip preserves practice testCount`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        db.studySessionDao().clearAllSessions()
        db.customTimerDao().clearAllTimers()
        db.dailyNoteDao().clearAllNotes()
        UserPreferences(context).clear()

        val legacyJson = """
            {
              "version": 1,
              "userName": "AyseLegacy",
              "userTrack": "Sayısal",
              "sessions": [
                {
                  "subject": "Kimya",
                  "durationSeconds": 1800,
                  "sessionType": "KRONOMETRE",
                  "timestamp": 1760526000000
                }
              ],
              "dailyNotes": [
                {
                  "dateStr": "2026-10-14",
                  "noteText": "Eski not",
                  "studyDurationMinutes": 45,
                  "subjectBulletsJson": "[{\"subject\":\"Biyoloji\",\"note\":\"Hücre Bölünmesi\"}]"
                }
              ]
            }
        """.trimIndent()

        val legacyOk = BackupHelper.restoreFromBackupJson(context, legacyJson)
        assertTrue(legacyOk)
        assertEquals("AyseLegacy", UserPreferences(context).userName)

        val restoredSessions = db.studySessionDao().getAllSessionsOnce()
        assertEquals(1, restoredSessions.size)
        assertTrue(restoredSessions[0].dateFormatted.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        val restoredLegacyNote = db.dailyNoteDao().getNoteForDateOnce("2026-10-14")
        assertNotNull(restoredLegacyNote)
        val legacyBullets = restoredLegacyNote!!.getSubjectBullets()
        assertEquals(1, legacyBullets.size)
        assertEquals("Hücre Bölünmesi", legacyBullets[0].topic)
        assertEquals(AgendaSubjectBullet.STUDY_TYPE_LEGACY, legacyBullets[0].studyType)

        val backfilledNote = db.dailyNoteDao().getNoteForDateOnce(restoredSessions[0].dateFormatted)
        assertNotNull(backfilledNote)
        assertEquals(30, backfilledNote!!.studyDurationMinutes)
        assertTrue(backfilledNote.getSubjectBullets().any { it.subject == "Kimya" && it.studyType == AgendaSubjectBullet.STUDY_TYPE_TIMER })

        val v5Note = DailyNote(
            dateStr = "2026-10-16",
            noteText = "v5 kontrolü",
            mood = "🔥",
            studyDurationMinutes = 90,
            subjectBulletsJson = DailyNote.encodeBullets(
                listOf(
                    AgendaSubjectBullet("Matematik", "İntegral", AgendaSubjectBullet.STUDY_TYPE_PRACTICE, testCount = 5),
                    AgendaSubjectBullet("Türkçe", "Paragraf", AgendaSubjectBullet.STUDY_TYPE_REVIEW, testCount = 0)
                )
            )
        )
        db.dailyNoteDao().insertOrUpdateNote(v5Note)

        val exportedJson = BackupHelper.createBackupJson(
            context,
            db.studySessionDao().getAllSessionsOnce(),
            db.customTimerDao().getAllTimersOnce(),
            db.dailyNoteDao().getAllNotesOnce()
        )

        db.studySessionDao().clearAllSessions()
        db.customTimerDao().clearAllTimers()
        db.dailyNoteDao().clearAllNotes()

        assertTrue(BackupHelper.restoreFromBackupJson(context, exportedJson))
        val reimportedV5 = db.dailyNoteDao().getNoteForDateOnce("2026-10-16")
        assertNotNull(reimportedV5)
        val reBullets = reimportedV5!!.getSubjectBullets()
        assertEquals(2, reBullets.size)
        assertEquals("İntegral", reBullets[0].topic)
        assertEquals(AgendaSubjectBullet.STUDY_TYPE_PRACTICE, reBullets[0].studyType)
        assertEquals(5, reBullets[0].testCount)
        assertEquals(AgendaSubjectBullet.STUDY_TYPE_REVIEW, reBullets[1].studyType)

        val widgetStats = ExamWidgetUpdater.loadStudyStats(context)
        assertTrue(widgetStats.totalTestsSolved >= 5)
        assertTrue(widgetStats.totalAllTimeMinutes >= 90)
    }

    @Test
    fun `privacy safe bug report hides user data and internal architecture`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)
        prefs.userName = "GizliKullaniciAdi123"
        prefs.syncKey = "9876"

        AppActionLogger.logAction(context, "NAV", "agenda")
        AppActionLogger.logAction(context, "POMODORO", "start_work:Matematik")
        AppActionLogger.logError(context, "TIMER", "CustomSecretException")

        val report = AppActionLogger.buildPrivacySafeReport(context)
        assertTrue(report.contains("DIAG_TOKEN_V1"))
        assertFalse(report.contains("GizliKullaniciAdi123"))
        assertFalse(report.contains("9876"))
        assertFalse(report.contains("Matematik"))
        assertFalse(report.contains("AgendaScreen"))
        assertFalse(report.contains("PomodoroScreen"))
        assertFalse(report.contains("com.example"))
        assertFalse(report.contains("CustomSecretException"))
    }

    @Test
    fun `subjects separate paragraf problem and geometri across tracks`() {
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        listOf("Sayısal", "Eşit Ağırlık", "Sözel", "Dil").forEach { track ->
            val subjects = vm.getSubjectsForTrack(track, "YKS")
            assertTrue("Missing TYT Türkçe in $track", subjects.contains("TYT Türkçe"))
            assertTrue("Missing Paragraf in $track", subjects.contains("Paragraf"))
            assertTrue("Missing Matematik in $track", subjects.contains("Matematik"))
            assertTrue("Missing Problem in $track", subjects.contains("Problem"))
            assertTrue("Missing Geometri in $track", subjects.contains("Geometri"))
        }
    }

    @Test
    fun `practice automatically counts as reviewed and deneme calculates net accurately`() {
        val practice = AgendaSubjectBullet(
            subject = "Paragraf",
            topic = "Anlatım Bozuklukları",
            studyType = AgendaSubjectBullet.STUDY_TYPE_PRACTICE,
            testCount = 3,
            examStage = "TYT",
            sourceBook = "Bilgi Sarmal"
        )
        val deneme = AgendaSubjectBullet(
            subject = "Matematik",
            topic = "TYT Genel",
            studyType = AgendaSubjectBullet.STUDY_TYPE_DENEME,
            examStage = "TYT",
            trialType = "TYT",
            totalQuestions = 120,
            correctCount = 96,
            wrongCount = 12
        )
        assertEquals(12, deneme.blankCount)
        assertEquals(93.0, deneme.computeNet(), 0.001)

        val stats = com.example.data.model.StudyStatsCalculator.compute(
            sessions = emptyList(),
            notes = listOf(
                DailyNote(
                    dateStr = "2026-10-20",
                    noteText = "",
                    studyDurationMinutes = 120,
                    subjectBulletsJson = DailyNote.encodeBullets(listOf(practice, deneme))
                )
            )
        )
        assertEquals(3, stats.totalTestsSolved)
        assertEquals(1, stats.totalTopicsReviewed)
        assertEquals(1, stats.totalDenemesSolved)
    }

    @Test
    fun `editing and moving bullet or entire day merges cleanly without overwriting target date`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = StudyRepository(db.studySessionDao(), db.customTimerDao(), db.dailyNoteDao())
            val sourceDate = "2026-11-01"
            val targetDate = "2026-11-02"
            val sessionTs = 1762000000000L

            repo.saveAgendaEntry(
                dateStr = targetDate,
                studyDurationMinutes = 40,
                subjectBullets = listOf(
                    AgendaSubjectBullet(
                        subject = "Geometri",
                        topic = "Üçgenler",
                        studyType = AgendaSubjectBullet.STUDY_TYPE_REVIEW,
                        examStage = "TYT"
                    )
                ),
                noteText = "Hedef gün notu",
                mood = ""
            )

            repo.insertSession(
                StudySession(
                    subject = "Problem",
                    durationSeconds = 1800L,
                    sessionType = "KRONOMETRE",
                    blocksCompleted = 1,
                    timestamp = sessionTs,
                    dateFormatted = sourceDate,
                    examStage = "TYT"
                )
            )

            val sourceNoteBefore = db.dailyNoteDao().getNoteForDateOnce(sourceDate)!!
            val sourceBullets = sourceNoteBefore.getSubjectBullets()
            assertEquals(1, sourceBullets.size)

            // Edit source bullet in detail AND move it to targetDate
            val editedAndMoved = sourceBullets[0].copy(
                topic = "Yaş Problemleri",
                examStage = "AYT",
                sourceBook = "Acil Problemler"
            )
            repo.updateOrMoveBullet(
                sourceDateStr = sourceDate,
                bulletIndex = 0,
                updatedBullet = editedAndMoved,
                targetDateStr = targetDate
            )

            val mergedTarget = db.dailyNoteDao().getNoteForDateOnce(targetDate)!!
            assertEquals("Hedef gün notu", mergedTarget.noteText)
            val targetBullets = mergedTarget.getSubjectBullets()
            assertEquals(2, targetBullets.size)
            assertTrue(targetBullets.any { it.subject == "Geometri" && it.topic == "Üçgenler" })
            assertTrue(targetBullets.any { it.topic == "Yaş Problemleri" && it.examStage == "AYT" && it.sourceBook == "Acil Problemler" })

            val movedSession = db.studySessionDao().getAllSessionsOnce().first { it.timestamp == sessionTs }
            assertEquals(targetDate, movedSession.dateFormatted)
            assertEquals("AYT", movedSession.examStage)

            // Also test moving entire day cleanly merges into another existing day
            val dayA = "2026-11-05"
            val dayB = "2026-11-06"
            repo.saveAgendaEntry(
                dateStr = dayA,
                studyDurationMinutes = 50,
                subjectBullets = listOf(
                    AgendaSubjectBullet("Fizik", "Optik", AgendaSubjectBullet.STUDY_TYPE_PRACTICE, testCount = 2, examStage = "TYT")
                ),
                noteText = "Gün A notu",
                mood = ""
            )
            repo.saveAgendaEntry(
                dateStr = dayB,
                studyDurationMinutes = 30,
                subjectBullets = listOf(
                    AgendaSubjectBullet("Kimya", "Gazlar", AgendaSubjectBullet.STUDY_TYPE_REVIEW, examStage = "AYT")
                ),
                noteText = "Gün B notu",
                mood = ""
            )
            repo.moveEntireDayToDate(dayA, dayB)

            val afterDayMerge = db.dailyNoteDao().getNoteForDateOnce(dayB)!!
            assertEquals(80, afterDayMerge.studyDurationMinutes)
            assertTrue(afterDayMerge.noteText.contains("Gün B notu"))
            assertTrue(afterDayMerge.noteText.contains("Gün A notu"))
            assertEquals(2, afterDayMerge.getSubjectBullets().size)
            assertEquals(null, db.dailyNoteDao().getNoteForDateOnce(dayA))
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup v6 roundtrip preserves examStage sourceBook and deneme metrics`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        db.studySessionDao().clearAllSessions()
        db.customTimerDao().clearAllTimers()
        db.dailyNoteDao().clearAllNotes()

        db.dailyNoteDao().insertOrUpdateNote(
            DailyNote(
                dateStr = "2026-11-10",
                noteText = "Deneme ve kaynak testi",
                studyDurationMinutes = 165,
                subjectBulletsJson = DailyNote.encodeBullets(
                    listOf(
                        AgendaSubjectBullet(
                            subject = "Matematik",
                            topic = "TYT Denemesi",
                            studyType = AgendaSubjectBullet.STUDY_TYPE_DENEME,
                            examStage = "TYT",
                            trialType = "TYT",
                            totalQuestions = 120,
                            correctCount = 100,
                            wrongCount = 8
                        ),
                        AgendaSubjectBullet(
                            subject = "Geometri",
                            topic = "Çember",
                            studyType = AgendaSubjectBullet.STUDY_TYPE_PRACTICE,
                            testCount = 3,
                            examStage = "AYT",
                            sourceBook = "Orijinal Geometri"
                        )
                    )
                )
            )
        )

        val json = BackupHelper.createBackupJson(
            context,
            db.studySessionDao().getAllSessionsOnce(),
            db.customTimerDao().getAllTimersOnce(),
            db.dailyNoteDao().getAllNotesOnce()
        )
        db.dailyNoteDao().clearAllNotes()

        assertTrue(BackupHelper.restoreFromBackupJson(context, json))
        val restored = db.dailyNoteDao().getNoteForDateOnce("2026-11-10")!!
        val bullets = restored.getSubjectBullets()
        assertEquals(2, bullets.size)
        assertEquals("TYT", bullets[0].examStage)
        assertEquals(AgendaSubjectBullet.STUDY_TYPE_DENEME, bullets[0].studyType)
        assertEquals(120, bullets[0].totalQuestions)
        assertEquals(100, bullets[0].correctCount)
        assertEquals(8, bullets[0].wrongCount)
        assertEquals("AYT", bullets[1].examStage)
        assertEquals("Orijinal Geometri", bullets[1].sourceBook)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `manual corner lock enters fullscreen dark focus mode after 5s with zero buttons and settings removes amoled toggle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ActiveTimerManager.resetPomodoro(context)
        completeOnboarding("Elif", "Sayısal", "YKS")

        // Verify AMOLED focus setting is removed from Settings
        composeTestRule.onNodeWithTag("dashboard_more_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ayarlar").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("settings_amoled")).fetchSemanticsNodes().size)
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        // Start Pomodoro and lock via corner lock icon button
        composeTestRule.onNodeWithTag("nav_tab_timer_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timer_hub_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pomo_start_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pomo_focus_lock_btn").assertIsDisplayed()
        val lockBounds = composeTestRule.onNodeWithTag("pomo_focus_lock_btn").fetchSemanticsNode().boundsInRoot
        val pauseBounds = composeTestRule.onNodeWithTag("pomo_pause_btn").fetchSemanticsNode().boundsInRoot
        assertFalse(lockBounds.overlaps(pauseBounds))

        composeTestRule.onNodeWithTag("pomo_focus_lock_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pomo_focus_lock_armed").assertIsDisplayed()

        // Advance both Compose test clock and Robolectric main looper past 5s threshold
        composeTestRule.mainClock.advanceTimeBy(5_500L)
        org.robolectric.shadows.ShadowLooper.idleMainLooper(6, java.util.concurrent.TimeUnit.SECONDS)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dark_focus_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dark_focus_time_text").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Ana Sayfa").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("Acil Durum (Duraklat)").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("dark_focus_unlock_btn")).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("dark_focus_unlock_slider")).fetchSemanticsNodes().size)

        // First tap reveals the Lock button without exiting Focus Mode
        composeTestRule.onNodeWithTag("dark_focus_overlay").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dark_focus_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dark_focus_unlock_btn").assertIsDisplayed()

        // Tap Lock button to reveal slide-to-unlock bar
        composeTestRule.onNodeWithTag("dark_focus_unlock_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dark_focus_unlock_slider").assertIsDisplayed()

        // Slide across the thumb to unlock while keeping Pomodoro running
        composeTestRule.onNodeWithTag("dark_focus_unlock_slider").performTouchInput {
            swipeRight(startX = width * 0.08f, endX = width * 0.96f)
        }
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("dark_focus_overlay")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("nav_tab_main").assertIsDisplayed()
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        assertEquals(null, vm.focusLockedTarget.value)
        assertEquals(PomodoroState.WORKING, vm.pomodoroState.value)

        // Direct Pause shows Devam and Sonlandir without Acil Durum prompt
        composeTestRule.onNodeWithTag("pomo_pause_btn").assertTextEquals("Duraklat")
        composeTestRule.onNodeWithTag("pomo_pause_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithText("Acil Durum", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("pomo_resume_btn").assertTextEquals("Devam")
        composeTestRule.onNodeWithTag("pomo_end_btn").assertTextEquals("Sonlandır")

        ActiveTimerManager.resetPomodoro(context)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `agenda shows tyt ayt selector 3-way study types and hidden-arrow book section`() {
        completeOnboarding("Mert", "Sayısal", "YKS")
        scrollVerticalListToTag("dashboard_ajandam_card")
        composeTestRule.onNodeWithTag("dashboard_ajandam_card").performClick()
        composeTestRule.waitForIdle()

        scrollVerticalListToTag("agenda_stage_TYT")
        composeTestRule.onNodeWithTag("agenda_stage_TYT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agenda_stage_AYT").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("agenda_stage_GEN")).fetchSemanticsNodes().size)

        scrollVerticalListToTag("agenda_type_practice_btn")
        composeTestRule.onNodeWithTag("agenda_type_review_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agenda_type_practice_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agenda_type_deneme_btn").assertIsDisplayed()

        // Select Soru Çözümü -> Book input initially collapsed behind arrow toggle
        composeTestRule.onNodeWithTag("agenda_type_practice_btn").performClick()
        composeTestRule.waitForIdle()
        scrollVerticalListToTag("agenda_book_toggle")
        composeTestRule.onNodeWithTag("agenda_book_toggle").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("agenda_book_input")).fetchSemanticsNodes().size)

        // Expand book input via hidden arrow
        composeTestRule.onNodeWithTag("agenda_book_toggle").performClick()
        composeTestRule.waitForIdle()
        scrollVerticalListToTag("agenda_book_input")
        composeTestRule.onNodeWithTag("agenda_book_input").assertIsDisplayed()

        // Switch to Deneme -> shows TYT / AYT / Branş options + question count inputs
        scrollVerticalListToTag("agenda_type_deneme_btn")
        composeTestRule.onNodeWithTag("agenda_type_deneme_btn").performClick()
        composeTestRule.waitForIdle()
        scrollVerticalListToTag("agenda_deneme_questions_input")
        composeTestRule.onNodeWithTag("agenda_deneme_questions_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agenda_deneme_correct_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agenda_deneme_wrong_input").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `weekly stats exclude old entries while detailed statistics screen shows all time and back navigation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())
        val oldCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -21) }
        val oldDateStr = sdf.format(oldCal.time)

        val oldSession = StudySession(
            subject = "Fizik",
            durationSeconds = 3600L,
            sessionType = "POMODORO",
            blocksCompleted = 2,
            timestamp = oldCal.timeInMillis,
            dateFormatted = oldDateStr,
            examStage = "TYT"
        )
        val todaySession = StudySession(
            subject = "Matematik",
            durationSeconds = 1800L,
            sessionType = "KRONOMETRE",
            blocksCompleted = 1,
            timestamp = System.currentTimeMillis(),
            dateFormatted = todayStr,
            examStage = "TYT"
        )

        val weekly = StudyStatsCalculator.computeWeekly(listOf(oldSession, todaySession), emptyList())
        val allTime = StudyStatsCalculator.compute(listOf(oldSession, todaySession), emptyList())
        assertEquals(30, weekly.totalAllTimeMinutes)
        assertEquals(90, allTime.totalAllTimeMinutes)

        runBlocking {
            db.studySessionDao().clearAllSessions()
            db.dailyNoteDao().clearAllNotes()
            db.studySessionDao().insertSession(oldSession)
            db.studySessionDao().insertSession(todaySession)
        }

        completeOnboarding("Emir", "Sayısal", "YKS")

        scrollVerticalListToTag("dashboard_stats_detail_btn")
        composeTestRule.onNodeWithText("Bu Hafta").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_stats_detail_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("detailed_statistics_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detaylı İstatistikler").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tüm Zamanlar", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("stats_back_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_main").assertIsDisplayed()
    }

    @Test
    fun `strict TYT AYT stage filtering configures subjects per student track and YDT language`() {
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]

        val tytSay = vm.getSubjectsForTrackAndStage("Sayısal", "YKS", "TYT")
        assertTrue(tytSay.containsAll(listOf("TYT Türkçe", "Paragraf", "Matematik", "Problem", "Geometri", "Tarih", "Coğrafya", "Felsefe", "Din Kültürü", "Fizik", "Kimya", "Biyoloji")))

        val aytSay = vm.getSubjectsForTrackAndStage("Sayısal", "YKS", "AYT")
        assertEquals(listOf("Matematik", "Geometri", "Fizik", "Kimya", "Biyoloji"), aytSay)
        assertFalse(aytSay.contains("Paragraf"))
        assertFalse(aytSay.contains("Problem"))

        val aytEa = vm.getSubjectsForTrackAndStage("Eşit Ağırlık", "YKS", "AYT")
        assertEquals(listOf("Matematik", "Türk Dili ve Edebiyatı", "Tarih", "Coğrafya"), aytEa)
        assertFalse(aytEa.contains("Geometri"))
        assertFalse(aytEa.contains("Paragraf"))
        assertFalse(aytEa.contains("Problem"))

        val aytSoz = vm.getSubjectsForTrackAndStage("Sözel", "YKS", "AYT")
        assertFalse(aytSoz.contains("Geometri"))
        assertFalse(aytSoz.contains("Matematik"))
        assertFalse(aytSoz.contains("Paragraf"))
        assertTrue(aytSoz.containsAll(listOf("Türk Dili ve Edebiyatı", "Felsefe Grubu", "Psikoloji", "Sosyoloji", "Mantık", "Din Kültürü")))

        val ydtDe = vm.getSubjectsForTrackAndStage("Dil", "YKS", "YDT", "Almanca")
        assertTrue(ydtDe.contains("YDT Almanca"))
        assertTrue(ydtDe.contains("Kelime"))
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `timer screen replaces Deneme Sinavi with Brans Denemesi filtered by stage`() {
        completeOnboarding("Kaan", "Sayısal", "YKS")
        composeTestRule.onNodeWithTag("nav_tab_timer_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timer_hub_tab_0").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodesWithText("Deneme Sınavı", ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("timer_stage_GEN")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("timer_brans_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("timer_brans_chip").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("timer_brans_deneme_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("branch_subject_Paragraf").assertIsDisplayed()

        composeTestRule.onNodeWithTag("timer_stage_AYT").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("branch_subject_Paragraf")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("branch_subject_Fizik").performClick()
        composeTestRule.waitForIdle()

        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        assertEquals("Fizik", vm.branchMockSubject.value)
        assertEquals("AYT Branş Denemesi - Fizik", vm.denemeType.value)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `onboarding offers wifi transfer dialog and settings removes clear data with exact about order`() {
        composeTestRule.runOnUiThread {
            ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java].resetProfileAndOnboarding()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_sync_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_wifi_sync_dialog").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithText("Yerel Ağ Üzerinden İlerleme Aktarımı", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
        composeTestRule.onNodeWithTag("onboarding_wifi_legacy_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_legacy_menu_item").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_sync_ip_input").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kapat").performClick()
        composeTestRule.waitForIdle()

        completeOnboarding("Deniz", "Sayısal", "YKS")
        composeTestRule.onNodeWithTag("dashboard_more_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ayarlar").performClick()
        composeTestRule.waitForIdle()

        scrollVerticalListToTag("settings_about_card")
        composeTestRule.onNodeWithText("Sürüm 0.0.6").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Tüm Verileri Sil", ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("settings_clear_data")).fetchSemanticsNodes().size)

        scrollVerticalListToTag("developer_apps_btn")
        composeTestRule.onNodeWithTag("about_version_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("github_releases_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_apps_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_apps_btn").assertTextEquals("Geliştirici")
        composeTestRule.onNodeWithText("Ali Erdem ŞENOL", substring = true).assertIsDisplayed()

        // Bug Report button is hidden until version text is tapped 7 times
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("bug_report_btn")).fetchSemanticsNodes().size)
        repeat(6) {
            composeTestRule.onNodeWithTag("about_version_text").performClick()
            composeTestRule.waitForIdle()
        }
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("bug_report_btn")).fetchSemanticsNodes().size)

        // 7th tap unlocks developer options and reveals Bug Report in exact position
        composeTestRule.onNodeWithTag("about_version_text").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Geliştirici seçenekleri açıldı", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
        scrollVerticalListToTag("developer_apps_btn")
        composeTestRule.onNodeWithTag("bug_report_btn").assertIsDisplayed()

        val verY = composeTestRule.onNodeWithTag("about_version_text").fetchSemanticsNode().boundsInRoot.top
        val bugY = composeTestRule.onNodeWithTag("bug_report_btn").fetchSemanticsNode().boundsInRoot.top
        val ghY = composeTestRule.onNodeWithTag("github_releases_btn").fetchSemanticsNode().boundsInRoot.top
        val devY = composeTestRule.onNodeWithTag("developer_apps_btn").fetchSemanticsNode().boundsInRoot.top
        assertTrue(verY < bugY && bugY < ghY && ghY < devY)
    }

    @Test
    fun `daily motivation 06_00 and agenda reminder 22_45 compute future triggers and fire notifications`() {
        val now = System.currentTimeMillis()
        val motTrigger = NotificationHelper.computeNextDailyTriggerMillis(6, 0, now)
        val agendaTrigger = NotificationHelper.computeNextDailyTriggerMillis(22, 45, now)
        assertTrue(motTrigger > now)
        assertTrue(agendaTrigger > now)

        val calMot = Calendar.getInstance().apply { timeInMillis = motTrigger }
        assertEquals(6, calMot.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calMot.get(Calendar.MINUTE))

        val calAg = Calendar.getInstance().apply { timeInMillis = agendaTrigger }
        assertEquals(22, calAg.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, calAg.get(Calendar.MINUTE))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = DailyReminderReceiver()
        receiver.onReceive(context, Intent(NotificationHelper.ACTION_DAILY_MOTIVATION))
        receiver.onReceive(context, Intent(NotificationHelper.ACTION_DAILY_AGENDA_REMINDER))

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = Shadows.shadowOf(nm).allNotifications
        assertTrue(posted.size >= 2)
    }

    @Test
    fun `savedBookHelper strictly isolates TYT and AYT books for same subject and backfills from notes`() {
        var books = emptyList<SavedBookEntry>()
        books = SavedBookHelper.upsertBook(books, "345 TYT Fizik", "TYT", "Fizik", 100L)
        books = SavedBookHelper.upsertBook(books, "Aydın AYT Fizik", "AYT", "Fizik", 200L)
        books = SavedBookHelper.upsertBook(books, "Orbital TYT Kimya", "TYT", "Kimya", 300L)

        val tytPhysics = SavedBookHelper.filterSuggestions(books, "TYT", "Fizik")
        assertEquals(listOf("345 TYT Fizik"), tytPhysics)

        val aytPhysics = SavedBookHelper.filterSuggestions(books, "AYT", "Fizik")
        assertEquals(listOf("Aydın AYT Fizik"), aytPhysics)

        val note = DailyNote(
            dateStr = "2026-11-18",
            noteText = "",
            subjectBulletsJson = DailyNote.encodeBullets(
                listOf(
                    AgendaSubjectBullet("Fizik", "Optik", AgendaSubjectBullet.STUDY_TYPE_PRACTICE, testCount = 2, examStage = "TYT", sourceBook = "Palme TYT Fizik")
                )
            )
        )
        val backfilled = SavedBookHelper.backfillFromNotes(books, listOf(note), "YKS")
        assertTrue(SavedBookHelper.filterSuggestions(backfilled, "TYT", "Fizik").contains("Palme TYT Fizik"))
        assertFalse(SavedBookHelper.filterSuggestions(backfilled, "AYT", "Fizik").contains("Palme TYT Fizik"))
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `brans denemesi enforces max 90 minutes in viewModel and timer presets`() {
        completeOnboarding("Kerem", "Sayısal", "YKS")
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        composeTestRule.runOnUiThread {
            vm.setCustomTimerDuration(150)
            vm.selectTimerSubject("Branş Denemesi")
            vm.setCustomTimerDuration(180)
        }
        composeTestRule.waitForIdle()
        assertEquals(90, vm.timerDurationMinutes.value)

        composeTestRule.onNodeWithTag("nav_tab_timer_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timer_hub_tab_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("timer_brans_preset_90").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("timer_brans_preset_120")).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("timer_brans_preset_180")).fetchSemanticsNodes().size)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `statistics screen renders TYT AYT stage breakdown card and filters by stage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        runBlocking {
            db.studySessionDao().clearAllSessions()
            db.dailyNoteDao().clearAllNotes()
            db.studySessionDao().insertSession(
                StudySession(
                    subject = "Fizik",
                    durationSeconds = 1800L,
                    sessionType = "POMODORO",
                    blocksCompleted = 1,
                    timestamp = System.currentTimeMillis(),
                    dateFormatted = todayStr,
                    examStage = "TYT"
                )
            )
            db.studySessionDao().insertSession(
                StudySession(
                    subject = "Kimya",
                    durationSeconds = 2400L,
                    sessionType = "KRONOMETRE",
                    blocksCompleted = 1,
                    timestamp = System.currentTimeMillis(),
                    dateFormatted = todayStr,
                    examStage = "AYT"
                )
            )
        }
        completeOnboarding("Sude", "Sayısal", "YKS")
        scrollVerticalListToTag("dashboard_stats_detail_btn")
        composeTestRule.onNodeWithTag("dashboard_stats_detail_btn").performClick()
        composeTestRule.waitForIdle()

        scrollVerticalListToTag("stats_stage_breakdown_card")
        composeTestRule.onNodeWithTag("stats_stage_breakdown_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stats_stage_filter_TYT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stats_stage_filter_AYT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stats_stage_filter_AYT").performClick()
        composeTestRule.waitForIdle()
        scrollVerticalListToTag("stats_subject_time_card")
        composeTestRule.onNodeWithText("Kimya").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Fizik").fetchSemanticsNodes().size)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `parent pairing menu is hidden when disabled and appears between calculator and settings when enabled`() {
        completeOnboarding("Batu", "Sayısal", "YKS")
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        composeTestRule.runOnUiThread { vm.setParentPairingEnabled(false) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_more_menu_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("menu_parent_pairing")).fetchSemanticsNodes().size)

        // Enable switch inside Settings -> Tab 0 (Genel, directly below Profile)
        composeTestRule.onNodeWithText("Ayarlar").performClick()
        composeTestRule.waitForIdle()
        scrollVerticalListToTag("settings_parent_pairing_switch")
        composeTestRule.onNodeWithTag("settings_parent_pairing_switch").performClick()
        composeTestRule.waitForIdle()

        // Navigate back to Home and verify Veli Eşitlemesi sits between Hesap Makinesi and Ayarlar
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_more_menu_btn").performClick()
        composeTestRule.waitForIdle()

        val calcTop = composeTestRule.onNodeWithText("Hesap Makinesi").fetchSemanticsNode().boundsInRoot.top
        val pairTop = composeTestRule.onNodeWithTag("menu_parent_pairing").fetchSemanticsNode().boundsInRoot.top
        val setTop = composeTestRule.onNodeWithText("Ayarlar").fetchSemanticsNode().boundsInRoot.top
        assertTrue(calcTop < pairTop && pairTop < setTop)
    }

    @Test
    fun `localSyncHelper sanitizes brand and model with 5 digit unique code without banned terms`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Pixel", LocalSyncHelper.sanitizeBrand("Google", "google"))
        assertEquals("Samsung", LocalSyncHelper.sanitizeBrand("samsung"))
        assertEquals("Galaxy S5", LocalSyncHelper.sanitizeModel("Samsung Galaxy S5", "Samsung"))
        assertEquals("", LocalSyncHelper.sanitizeModel("Google SDK", "Pixel"))

        val pkt1 = LocalSyncHelper.encodeDiscoveryPacket("Samsung", "", UserPreferences.ROLE_STUDENT, "TRANSFER", 8888, model = "Galaxy S5", deviceCode = "10001")
        val pkt2 = LocalSyncHelper.encodeDiscoveryPacket("Samsung", "", UserPreferences.ROLE_STUDENT, "TRANSFER", 8888, model = "Galaxy S5", deviceCode = "10002")
        assertFalse(pkt1.contains("Google", ignoreCase = true))
        val parsed1 = LocalSyncHelper.parseDiscoveryPacket(pkt1, "192.168.1.44")
        val parsed2 = LocalSyncHelper.parseDiscoveryPacket(pkt2, "192.168.1.45")
        assertNotNull(parsed1)
        assertNotNull(parsed2)
        assertEquals("Samsung", parsed1!!.brand)
        assertEquals("Galaxy S5", parsed1.model)
        assertEquals("10001", parsed1.deviceCode)
        assertTrue(parsed1.compactSummary().contains("#10001"))
        assertTrue(parsed1.stableId != parsed2!!.stableId)

        val prefs = UserPreferences(context)
        assertEquals(5, prefs.deviceUniqueCode.length)
        assertTrue(prefs.deviceUniqueCode.all { it.isDigit() })
        prefs.userRole = UserPreferences.ROLE_PARENT
        prefs.userName = "AnneBaba"

        val snapJson = BackupHelper.createStudentSnapshotJson(context, "Samsung", "Ayse")
        val saved = BackupHelper.saveParentStudentSnapshot(context, snapJson)
        assertNotNull(saved)
        assertEquals(UserPreferences.ROLE_PARENT, prefs.userRole)
        assertEquals("AnneBaba", prefs.userName)
    }

    @Test
    fun `localSyncHelper status callbacks run on main looper and safely show Toast`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callbackThreads = mutableListOf<Boolean>()
        var completed = false

        composeTestRule.activity.lifecycleScope.launch {
            LocalSyncHelper.connectAndSyncAutoPeer(
                context = context,
                hostIp = "127.0.0.1",
                port = 59991
            ) { status ->
                callbackThreads.add(Looper.myLooper() == Looper.getMainLooper())
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            }

            LocalSyncHelper.pushStudentSnapshotToParent(
                context = context,
                hostIp = "127.0.0.1",
                port = 59992
            ) { status ->
                callbackThreads.add(Looper.myLooper() == Looper.getMainLooper())
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            }
            completed = true
        }

        composeTestRule.waitUntil(timeoutMillis = 15_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            completed
        }
        assertTrue(callbackThreads.size >= 4)
        assertTrue(callbackThreads.all { it })
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `parent role onboarding hides exam targets and shows read only parent dashboard`() {
        composeTestRule.runOnUiThread {
            ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java].resetProfileAndOnboarding()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_next_step_1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_role_parent").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("parent_onboarding_info_card").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Hedef Sınavın").fetchSemanticsNodes().size)

        composeTestRule.runOnUiThread {
            ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
                .saveUserProfile("Mehmet", "Sayısal", "YKS", role = UserPreferences.ROLE_PARENT)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("parent_dashboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_countdown_card").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("nav_tab_timer_hub")).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("nav_tab_stopwatch")).fetchSemanticsNodes().size)
    }

    @Test
    fun `backupHelper mutual merge lets higher activity device win conflicts while filling blank gaps`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val prefs = UserPreferences(context)
        db.studySessionDao().clearAllSessions()
        db.dailyNoteDao().clearAllNotes()
        db.customTimerDao().clearAllTimers()

        // Local device has low activity (1 session, empty userName, empty savedBooks, no note on 2026-11-10)
        prefs.userName = ""
        prefs.userTrack = "Sözel"
        val origCode = prefs.deviceUniqueCode
        db.studySessionDao().insertSession(
            StudySession(
                subject = "Tarih",
                durationSeconds = 900L,
                sessionType = "POMODORO",
                blocksCompleted = 1,
                timestamp = 100000L,
                dateFormatted = "2026-11-09",
                examStage = "TYT"
            )
        )
        db.dailyNoteDao().insertOrUpdateNote(
            DailyNote(
                dateStr = "2026-11-09",
                noteText = "Yerel not",
                subjectBulletsJson = DailyNote.encodeBullets(
                    listOf(AgendaSubjectBullet("Tarih", "İlk Çağ", AgendaSubjectBullet.STUDY_TYPE_REVIEW, examStage = "TYT"))
                ),
                studyDurationMinutes = 15,
                timestamp = 100000L
            )
        )

        // Incoming backup has higher activity (2 sessions, filled userName = "Elif", track = "Sayısal", pitchBlack = true)
        val incomingJson = """
            {
              "version": 8,
              "userName": "Elif",
              "userTrack": "Sayısal",
              "examTarget": "YKS",
              "ydtLanguage": "İngilizce",
              "themeMode": "DARK",
              "isPitchBlackDarkEnabled": true,
              "savedBooks": [{"bookName":"345 TYT Fizik","examStage":"TYT","subject":"Fizik","lastUsedAt":500}],
              "sessions": [
                {"subject":"Fizik","durationSeconds":1800,"sessionType":"POMODORO","blocksCompleted":1,"timestamp":200000,"dateFormatted":"2026-11-10","examStage":"TYT"},
                {"subject":"Kimya","durationSeconds":1500,"sessionType":"KRONOMETRE","blocksCompleted":1,"timestamp":300000,"dateFormatted":"2026-11-11","examStage":"AYT"}
              ],
              "dailyNotes": [
                {
                  "dateStr": "2026-11-09",
                  "noteText": "",
                  "subjectBulletsJson": "[{\"subject\":\"Matematik\",\"topic\":\"Türev\",\"studyType\":\"SORU\",\"testCount\":2,\"examStage\":\"AYT\"}]",
                  "studyDurationMinutes": 45,
                  "timestamp": 200000
                },
                {
                  "dateStr": "2026-11-10",
                  "noteText": "Gelen boşluk doldurma",
                  "subjectBulletsJson": "[]",
                  "studyDurationMinutes": 30,
                  "timestamp": 250000
                }
              ],
              "customTimers": []
            }
        """.trimIndent()

        val ok = BackupHelper.restoreFromBackupJson(context, incomingJson)
        assertTrue(ok)

        // Higher-activity incoming won scalar track & pitchBlack, local empty name filled, local identity preserved
        assertEquals("Elif", prefs.userName)
        assertEquals("Sayısal", prefs.userTrack)
        assertTrue(prefs.isPitchBlackDarkEnabled)
        assertEquals(origCode, prefs.deviceUniqueCode)

        // Sessions unioned (1 local + 2 incoming = 3)
        val allSessions = db.studySessionDao().getAllSessionsOnce()
        assertEquals(3, allSessions.size)

        // Day 2026-11-09 merged bullets from both devices without overwriting non-blank noteText
        val mergedDay = db.dailyNoteDao().getNoteForDateOnce("2026-11-09")
        assertNotNull(mergedDay)
        assertEquals("Yerel not", mergedDay!!.noteText)
        assertEquals(2, mergedDay.getSubjectBullets().size)

        // Day 2026-11-10 gap filled from incoming
        val filledDay = db.dailyNoteDao().getNoteForDateOnce("2026-11-10")
        assertNotNull(filledDay)
        assertEquals("Gelen boşluk doldurma", filledDay!!.noteText)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h1600dp")
    fun `settings uses Ayarlar Yedekleme Uygulama Bicemi pitch black switch and exam dates above custom timers`() {
        completeOnboarding("Zeynep", "Sayısal", "YKS")
        composeTestRule.onNodeWithTag("dashboard_more_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ayarlar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ayarlar").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Profil & Ayarlar").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("settings_tab_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_tab_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yedekleme").assertIsDisplayed()

        // Veli Eşitlemesi switch and Rol switch are in Tab 0 directly below Profil
        scrollVerticalListToTag("settings_parent_pairing_switch")
        composeTestRule.onNodeWithTag("settings_parent_pairing_switch").assertIsDisplayed()
        scrollVerticalListToTag("settings_switch_role_btn")
        composeTestRule.onNodeWithTag("settings_switch_role_btn").assertIsDisplayed()

        // Uygulama Biçemi + Koyu temayı siyahlaştır switch
        scrollVerticalListToTag("settings_pitch_black_switch")
        composeTestRule.onNodeWithText("Uygulama Biçemi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Koyu temayı siyahlaştır").assertIsDisplayed()
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        assertFalse(vm.isPitchBlackDarkEnabled.value)
        composeTestRule.onNodeWithTag("settings_pitch_black_switch").performClick()
        composeTestRule.waitForIdle()
        assertTrue(vm.isPitchBlackDarkEnabled.value)

        // Sınav Tarihlerini Düzenle sits above Özel Geri Sayım Sayaçları (both visible at same scroll offset)
        scrollVerticalListToTag("settings_custom_timers_card")
        scrollVerticalListToTag("settings_exam_dates_card")
        composeTestRule.onNodeWithText("ÖSYM ve MEB tarafından yapılan sınavların tarihlerini güncelleyebilirsin.").assertIsDisplayed()
        val examTop = composeTestRule.onNodeWithTag("settings_exam_dates_card").fetchSemanticsNode().boundsInRoot.top
        val timersTop = composeTestRule.onNodeWithTag("settings_custom_timers_card").fetchSemanticsNode().boundsInRoot.top
        assertTrue(examTop < timersTop)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h1400dp")
    fun `agenda saved days renders only a single edit pencil per day even with multiple bullets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.studySessionDao().clearAllSessions()
            db.dailyNoteDao().clearAllNotes()
            db.dailyNoteDao().insertOrUpdateNote(
                DailyNote(
                    dateStr = "2026-11-15",
                    noteText = "İki kayıtlı gün",
                    subjectBulletsJson = DailyNote.encodeBullets(
                        listOf(
                            AgendaSubjectBullet("Fizik", "Optik", AgendaSubjectBullet.STUDY_TYPE_PRACTICE, testCount = 2, examStage = "TYT", sourceBook = "345 TYT Fizik"),
                            AgendaSubjectBullet("Matematik", "Türev", AgendaSubjectBullet.STUDY_TYPE_REVIEW, examStage = "AYT")
                        )
                    ),
                    studyDurationMinutes = 90,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        completeOnboarding("Efe", "Sayısal", "YKS")
        scrollVerticalListToTag("dashboard_ajandam_card")
        composeTestRule.onNodeWithTag("dashboard_ajandam_card").performClick()
        composeTestRule.waitForIdle()

        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            vm.allNotes.value.any { it.dateStr == "2026-11-15" }
        }

        scrollVerticalListToTag("agenda_recent_toggle")
        composeTestRule.onNodeWithTag("agenda_recent_toggle").performClick()
        composeTestRule.waitForIdle()
        scrollVerticalListToTag("agenda_edit_day_btn_2026-11-15")
        composeTestRule.onNodeWithTag("agenda_edit_day_btn_2026-11-15").assertIsDisplayed()
        composeTestRule.onNodeWithTag("agenda_past_bullet_2026-11-15_0", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("agenda_past_bullet_2026-11-15_1", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Kaydı Düzenle")).fetchSemanticsNodes().size)

        // Clicking the single day pencil opens dialog showing bullets for fine-grained edit
        composeTestRule.onNodeWithTag("agenda_edit_day_btn_2026-11-15").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit_day_dialog_bullet_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_day_dialog_bullet_1").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `timer hub uses Geri Sayim label and pomodoro and stopwatch support Karisik stage`() {
        completeOnboarding("Mert", "Sayısal", "YKS")
        composeTestRule.onNodeWithTag("nav_tab_timer_hub").performClick()
        composeTestRule.waitForIdle()

        // Tab 0 is now named Geri Sayım instead of Sayaç & Branş
        composeTestRule.onNodeWithTag("timer_hub_tab_0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Geri Sayım").assertIsDisplayed()

        // Switch to Pomodoro tab and verify Karışık stage pill works and hides subject list
        composeTestRule.onNodeWithTag("timer_hub_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pomo_stage_KARISIK").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pomo_stage_KARISIK").performClick()
        composeTestRule.waitForIdle()
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        assertEquals("KARISIK", vm.pomoExamStage.value)
        assertEquals("Karışık", vm.selectedSubject.value)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("pomo_subject_list")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("pomo_karisik_info").assertIsDisplayed()
        val mixedPomoSubjects = vm.getSubjectsForTrackAndStage(stage = vm.pomoExamStage.value)
        assertTrue(mixedPomoSubjects.contains("TYT Türkçe"))
        assertTrue(mixedPomoSubjects.contains("Fizik"))

        // Switch to Stopwatch tab and verify Karışık stage pill works and hides subject list
        composeTestRule.onNodeWithTag("nav_tab_stopwatch").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stopwatch_stage_KARISIK").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stopwatch_stage_KARISIK").performClick()
        composeTestRule.waitForIdle()
        assertEquals("KARISIK", vm.stopwatchExamStage.value)
        assertEquals("Karışık", vm.stopwatchSubject.value)
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("stopwatch_subject_list")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("stopwatch_karisik_info").assertIsDisplayed()
        val mixedSwSubjects = vm.getSubjectsForTrackAndStage(stage = vm.stopwatchExamStage.value)
        assertTrue(mixedSwSubjects.contains("Paragraf"))
        assertTrue(mixedSwSubjects.contains("Geometri"))
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `widgets reflect light accent dark neutral border and pitch black accent edge border in real time`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        val vmContext = vm.getApplication<android.app.Application>()
        val tealHex = "#FF00BFA5"
        val tealColor = Color.parseColor(tealHex)

        // 1. Light mode with accent
        composeTestRule.runOnUiThread {
            vm.updateAccentColorHex(tealHex)
            vm.updateThemeMode("LIGHT")
            vm.setPitchBlackDarkEnabled(false)
        }
        composeTestRule.waitForIdle()
        assertEquals(tealHex, UserPreferences(vmContext).appAccentColorHex)
        val lightPalette = ExamWidgetUpdater.resolveWidgetTheme(vmContext)
        assertFalse(lightPalette.isDark)
        assertFalse(lightPalette.isPitchBlack)
        assertEquals(0xFFFEF7FF.toInt(), lightPalette.backgroundColor)
        assertEquals(0xFFE7E0EC.toInt(), lightPalette.borderColor)
        assertEquals(tealColor, lightPalette.primaryColor)

        // 2. Dark mode without pitch-black: dark surface & neutral dark border (not accent border)
        composeTestRule.runOnUiThread {
            vm.updateThemeMode("DARK")
            vm.setPitchBlackDarkEnabled(false)
        }
        composeTestRule.waitForIdle()
        val darkPalette = ExamWidgetUpdater.resolveWidgetTheme(vmContext)
        assertTrue(darkPalette.isDark)
        assertFalse(darkPalette.isPitchBlack)
        assertEquals(0xFF1C1B22.toInt(), darkPalette.backgroundColor)
        assertEquals(0xFF33303C.toInt(), darkPalette.borderColor)

        // 3. Dark mode + pitch-black enabled: pure #000000 background + accent-colored outer edge border
        composeTestRule.runOnUiThread {
            vm.setPitchBlackDarkEnabled(true)
        }
        composeTestRule.waitForIdle()
        val pitchBlackPalette = ExamWidgetUpdater.resolveWidgetTheme(vmContext)
        assertTrue(pitchBlackPalette.isDark)
        assertTrue(pitchBlackPalette.isPitchBlack)
        assertEquals(Color.BLACK, pitchBlackPalette.backgroundColor)
        assertTrue(pitchBlackPalette.borderColor != 0xFF33303C.toInt())
        assertTrue(Color.green(pitchBlackPalette.borderColor) > 120) // Teal green channel reflected on border

        // Sync context prefs & verify all 4 widget layouts inflate & apply dynamic RemoteViews without error
        UserPreferences(context).apply {
            appAccentColorHex = tealHex
            appThemeMode = "DARK"
            isPitchBlackDarkEnabled = true
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowMgr = Shadows.shadowOf(appWidgetManager)
        val singleId = shadowMgr.createWidget(ExamCountdownWidgetProvider::class.java, R.layout.widget_exam_countdown)
        val allId = shadowMgr.createWidget(AllExamsWidgetProvider::class.java, R.layout.widget_all_exams)
        val statsId = shadowMgr.createWidget(StudyStatsWidgetProvider::class.java, R.layout.widget_study_stats)
        val swId = shadowMgr.createWidget(StopwatchWidgetProvider::class.java, R.layout.widget_stopwatch)

        ExamCountdownWidgetProvider().updateSingleWidget(
            context,
            appWidgetManager,
            singleId,
            ExamWidgetUpdater.loadExamItems(context)
        )
        AllExamsWidgetProvider().updateWidgets(context, appWidgetManager, intArrayOf(allId))
        StudyStatsWidgetProvider().updateWidgets(context, appWidgetManager, intArrayOf(statsId))
        StopwatchWidgetProvider().updateWidgets(context, appWidgetManager, intArrayOf(swId))

        assertNotNull(shadowMgr.getViewFor(singleId))
        assertNotNull(shadowMgr.getViewFor(allId))
        assertNotNull(shadowMgr.getViewFor(statsId))
        assertNotNull(shadowMgr.getViewFor(swId))

        // Restore default theme prefs for test isolation
        UserPreferences(context).clear()
        UserPreferences(vmContext).clear()
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `stopwatch widget controls start pause save and reset synchronized with ActiveTimerManager and ViewModel`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        db.studySessionDao().clearAllSessions()
        db.dailyNoteDao().clearAllNotes()
        ActiveTimerManager.resetStopwatch(context)

        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        val provider = StopwatchWidgetProvider()

        // 1. Start from widget broadcast
        composeTestRule.runOnUiThread {
            provider.onReceive(context, Intent(StopwatchWidgetProvider.ACTION_SW_START))
        }
        composeTestRule.waitForIdle()
        assertTrue(ActiveTimerManager.snapshot.value.stopwatch.isRunning)
        assertTrue(vm.stopwatchIsRunning.value)

        // 2. Pause from widget broadcast
        Thread.sleep(50L)
        composeTestRule.runOnUiThread {
            provider.onReceive(context, Intent(StopwatchWidgetProvider.ACTION_SW_PAUSE))
        }
        composeTestRule.waitForIdle()
        assertFalse(ActiveTimerManager.snapshot.value.stopwatch.isRunning)
        assertFalse(vm.stopwatchIsRunning.value)
        assertTrue(ActiveTimerManager.snapshot.value.stopwatch.active)

        // 3. Reset from widget broadcast
        composeTestRule.runOnUiThread {
            provider.onReceive(context, Intent(StopwatchWidgetProvider.ACTION_SW_RESET))
        }
        composeTestRule.waitForIdle()
        assertFalse(ActiveTimerManager.snapshot.value.stopwatch.active)
        assertEquals(0L, ActiveTimerManager.snapshot.value.stopwatch.accumulatedMs)

        // 4. Save from widget with accumulated time -> saves KRONOMETRE session and resets
        val prefs = context.getSharedPreferences("baslayalim_timer_state", Context.MODE_PRIVATE)
        prefs.edit().putLong("sw_accumulated_ms", 125_000L).putBoolean("sw_active", true).commit()
        // Force reload snapshot in test by resetting field? Instead start+pause adds real elapsed or call save directly after manipulating elapsed:
        ActiveTimerManager.pauseStopwatch(context)
        // Simulate 125s via startEpoch in SharedPreferences doesn't bypass in-memory snap; instead verify via direct start -> shift snapshot via pause isn't needed if we start with accumulated:
        ActiveTimerManager.startStopwatch(context, "Matematik")
        val snapField = ActiveTimerManager::class.java.getDeclaredField("_snapshot")
        snapField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val snapFlow = snapField.get(ActiveTimerManager) as kotlinx.coroutines.flow.MutableStateFlow<com.example.util.ActiveTimerSnapshot>
        snapFlow.value = snapFlow.value.copy(
            stopwatch = snapFlow.value.stopwatch.copy(
                active = true,
                isRunning = false,
                startEpochMs = 0L,
                accumulatedMs = 125_000L,
                subject = "Fizik",
                examStage = "AYT"
            )
        )
        provider.saveCurrentStopwatchSession(context)
        val savedSessions = db.studySessionDao().getAllSessionsOnce()
        assertTrue(savedSessions.any { it.sessionType == "KRONOMETRE" && it.subject == "Fizik" && it.durationSeconds >= 120L })
        assertFalse(ActiveTimerManager.snapshot.value.stopwatch.active)
    }

    @Test
    fun `lan transfer automatically copies current progress json backup to clipboard before connecting`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        db.studySessionDao().clearAllSessions()
        db.studySessionDao().insertSession(
            StudySession(
                subject = "Matematik",
                durationSeconds = 1800L,
                sessionType = "POMODORO",
                blocksCompleted = 1,
                examStage = "TYT"
            )
        )
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        composeTestRule.runOnUiThread {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("empty", ""))
        }
        composeTestRule.waitForIdle()

        var finished = false
        composeTestRule.activity.lifecycleScope.launch {
            LocalSyncHelper.connectAndSyncAutoPeer(
                context = context,
                hostIp = "127.0.0.1",
                port = 59994
            ) { status ->
                if (status.contains("Hata") || status.contains("Error")) {
                    finished = true
                }
            }
            finished = true
        }
        composeTestRule.waitUntil(timeoutMillis = 15_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            finished
        }
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        assertTrue(clipText.contains("\"sessions\""))
        assertTrue(clipText.contains("Matematik"))
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `onboarding and settings remove anonymous names and show developer button as Gelistirici`() {
        composeTestRule.runOnUiThread {
            ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java].resetProfileAndOnboarding()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_next_step_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("onboarding_refresh_username_btn")).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("onboarding_network_identity").assertIsDisplayed()

        completeOnboarding("Kaan", "Sayısal", "YKS")
        composeTestRule.onNodeWithTag("dashboard_more_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ayarlar").performClick()
        composeTestRule.waitForIdle()

        scrollVerticalListToTag("developer_apps_btn")
        composeTestRule.onNodeWithTag("developer_apps_btn").assertTextEquals("Geliştirici")

        scrollVerticalListToTag("settings_tab_1")
        composeTestRule.onNodeWithTag("settings_tab_1").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodes(hasTestTag("settings_refresh_username_btn")).fetchSemanticsNodes().size)
    }

    @Test
    fun `daily average counts unlogged days as zero since first app usage`() {
        val now = System.currentTimeMillis()
        val nineDaysAgo = now - (9L * 24L * 3600L * 1000L)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val stats = StudyStatsCalculator.compute(
            sessions = emptyList(),
            notes = listOf(
                DailyNote(
                    dateStr = todayStr,
                    noteText = "Tek gün kayıt",
                    studyDurationMinutes = 100,
                    timestamp = now
                )
            ),
            firstUsageTimestampMillis = nineDaysAgo,
            nowMillis = now
        )
        // 10 inclusive calendar days (9 days ago .. today) -> 100 / 10 = 10 min/day
        assertEquals(100, stats.totalAllTimeMinutes)
        assertEquals(10, stats.averageDailyMinutes)
        assertEquals(1, stats.activeDaysCount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `statistics paydos notes start collapsed and countdown labels show TYT AYT without YKS`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.studySessionDao().clearAllSessions()
            db.dailyNoteDao().clearAllNotes()
            db.dailyNoteDao().insertOrUpdateNote(
                DailyNote(
                    dateStr = "2026-11-20",
                    noteText = "Bugün çok verimli paydos yaptım",
                    mood = "Motive & Özgüvenli",
                    studyDurationMinutes = 60,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        completeOnboarding("Arda", "Sayısal", "YKS")
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]
        assertEquals("TYT 2027", vm.tytCountdown.value?.title)
        assertEquals("AYT 2027", vm.aytCountdown.value?.title)
        assertEquals("TYT 2027", ExamWidgetUpdater.loadExamItems(context).first().title)

        scrollVerticalListToTag("dashboard_stats_detail_btn")
        composeTestRule.onNodeWithTag("dashboard_stats_detail_btn").performClick()
        composeTestRule.waitForIdle()

        scrollVerticalListToTag("stats_notes_toggle")
        composeTestRule.onNodeWithTag("stats_notes_toggle").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Bugün çok verimli paydos yaptım").fetchSemanticsNodes().size)

        composeTestRule.onNodeWithTag("stats_notes_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(androidx.compose.ui.test.hasText("Bugün çok verimli paydos yaptım"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bugün çok verimli paydos yaptım").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `selected exam counter persists across dashboard and small widget and stopwatch reset shows zero`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        UserPreferences(context).clear()
        completeOnboarding("Ece", "Sayısal", "YKS")
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]

        // Switch dashboard exam ticker to AYT 2027 and verify persisted preference
        composeTestRule.runOnUiThread {
            vm.selectDashboardExam(1, "AYT 2027")
        }
        composeTestRule.waitForIdle()
        val vmCtx = vm.getApplication<android.app.Application>()
        assertEquals(1, UserPreferences(vmCtx).selectedDashboardExamIndex)
        assertEquals("AYT 2027", UserPreferences(vmCtx).selectedDashboardExamTitle)

        // Verify pitch-black widget uses full opacity border and black-to-accent radial gradient bitmap
        UserPreferences(context).apply {
            appAccentColorHex = "#FF00BFA5"
            appThemeMode = "DARK"
            isPitchBlackDarkEnabled = true
        }
        val pitchPalette = ExamWidgetUpdater.resolveWidgetTheme(context)
        assertEquals(255, Color.alpha(pitchPalette.borderColor))
        assertTrue(Color.green(pitchPalette.borderColor) > 120)
        val gradientBmp = ExamWidgetUpdater.getOrCreatePitchBlackGradientBitmap(pitchPalette.accentColor)
        assertEquals(Color.BLACK, gradientBmp.getPixel(gradientBmp.width / 2, gradientBmp.height / 2))
        val edgePixel = gradientBmp.getPixel(gradientBmp.width - 10, gradientBmp.height / 2)
        assertTrue(Color.green(edgePixel) > 80)

        // Verify stopwatch widget hides Chronometer & displays 00:00 after reset
        ActiveTimerManager.startStopwatch(context, "Matematik")
        ActiveTimerManager.resetStopwatch(context)
        assertEquals(0L, vm.stopwatchElapsedMillis.value)
        assertFalse(ActiveTimerManager.snapshot.value.stopwatch.active)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowMgr = Shadows.shadowOf(appWidgetManager)
        val swId = shadowMgr.createWidget(StopwatchWidgetProvider::class.java, R.layout.widget_stopwatch)
        StopwatchWidgetProvider().updateWidgets(context, appWidgetManager, intArrayOf(swId))
        val swView = shadowMgr.getViewFor(swId)
        val staticTv = swView.findViewById<android.widget.TextView>(R.id.widget_sw_static_time)
        val chrono = swView.findViewById<android.widget.Chronometer>(R.id.widget_sw_chronometer)
        assertEquals(android.view.View.VISIBLE, staticTv.visibility)
        assertEquals("00:00", staticTv.text.toString())
        assertEquals(android.view.View.GONE, chrono.visibility)
        val swBgImg = swView.findViewById<android.widget.ImageView>(R.id.widget_bg_fill_layer)
        assertTrue(swBgImg.drawable is android.graphics.drawable.BitmapDrawable)

        // Verify small widget index & title persistence
        val singleId = shadowMgr.createWidget(ExamCountdownWidgetProvider::class.java, R.layout.widget_exam_countdown)
        val provider = ExamCountdownWidgetProvider()
        val items = ExamWidgetUpdater.loadExamItems(context)
        provider.onReceive(
            context,
            Intent(ExamCountdownWidgetProvider.ACTION_NEXT_EXAM).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, singleId)
        )
        val widgetPrefs = context.getSharedPreferences("baslayalim_widget_prefs", Context.MODE_PRIVATE)
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            widgetPrefs.getInt("idx_$singleId", -1) == 1
        }
        assertEquals(1, widgetPrefs.getInt("idx_$singleId", -1))
        assertEquals(items[1].title, widgetPrefs.getString("title_$singleId", ""))

        UserPreferences(context).clear()
        UserPreferences(vmCtx).clear()
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `applicationId is com alierdemsenol baslayalim and standardized labels have no emojis firar or dnd`() {
        assertEquals("com.alierdemsenol.baslayalim", BuildConfig.APPLICATION_ID)
        assertEquals("0.0.6", BuildConfig.VERSION_NAME)

        val forbidden = listOf("firar", "acil durum", "aynen devam", "rahatsız etmeyin", "rahatsiz etmeyin")
        val allTextKeys = listOf(
            "btn_pause", "btn_continue", "btn_end", "pomo_finish_session",
            "stopwatch_pause", "stopwatch_resume", "stopwatch_stop_save",
            "timer_footer_note", "app_name", "version_label", "dev_options_unlocked",
            "focus_unlock_tap_btn", "focus_slide_to_unlock"
        )
        for (lang in listOf("TR", "EN")) {
            for (key in allTextKeys) {
                val txt = TranslationHelper.getTranslation(key, lang)
                val lower = txt.lowercase(Locale.ROOT)
                forbidden.forEach { term ->
                    assertFalse("Forbidden term '$term' in $key ($lang): $txt", lower.contains(term))
                }
                assertFalse("Emoji found in $key ($lang): $txt", txt.any { ch ->
                    Character.isSurrogate(ch) || ch.code in 0x2600..0x27BF
                })
            }
        }

        completeOnboarding("Zeynep", "Sayısal", "YKS")
        composeTestRule.onNodeWithTag("nav_tab_timer_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timer_hub_tab_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("timer_start_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timer_focus_lock_btn").assertIsDisplayed()
        assertFalse(
            composeTestRule.onNodeWithTag("timer_focus_lock_btn").fetchSemanticsNode().boundsInRoot
                .overlaps(composeTestRule.onNodeWithTag("timer_pause_btn").fetchSemanticsNode().boundsInRoot)
        )
        composeTestRule.onNodeWithTag("timer_pause_btn").assertTextEquals("Duraklat")
        composeTestRule.onNodeWithTag("timer_pause_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timer_resume_btn").assertTextEquals("Devam")
        composeTestRule.onNodeWithTag("timer_end_btn").assertTextEquals("Sonlandır")
        composeTestRule.onNodeWithTag("timer_end_btn").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    @Config(sdk = [36], qualifiers = "tr-rTR-w412dp-h915dp")
    fun `stopwatch karisik laps offer optional before and after lap subjects without requiring selection`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        db.studySessionDao().clearAllSessions()
        ActiveTimerManager.resetStopwatch(context)

        completeOnboarding("Efe", "Sayısal", "YKS")
        val vm = ViewModelProvider(composeTestRule.activity)[StudyViewModel::class.java]

        composeTestRule.onNodeWithTag("nav_tab_stopwatch").performClick()
        composeTestRule.waitForIdle()

        // Switch stopwatch to Karisik
        composeTestRule.onNodeWithTag("stopwatch_stage_KARISIK").performClick()
        composeTestRule.waitForIdle()

        // Simulate 120s running -> add lap -> 180s total -> pause
        val snapField = ActiveTimerManager::class.java.getDeclaredField("_snapshot")
        snapField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val snapFlow = snapField.get(ActiveTimerManager) as kotlinx.coroutines.flow.MutableStateFlow<com.example.util.ActiveTimerSnapshot>

        composeTestRule.runOnUiThread {
            ActiveTimerManager.startStopwatch(context, "Karışık")
            snapFlow.value = snapFlow.value.copy(
                stopwatch = snapFlow.value.stopwatch.copy(
                    active = true,
                    isRunning = true,
                    startEpochMs = 0L,
                    accumulatedMs = 120_000L,
                    subject = "Karışık",
                    examStage = "KARISIK"
                )
            )
            vm.addStopwatchLap()
            snapFlow.value = snapFlow.value.copy(
                stopwatch = snapFlow.value.stopwatch.copy(
                    active = true,
                    isRunning = false,
                    startEpochMs = 0L,
                    accumulatedMs = 180_000L,
                    subject = "Karışık",
                    examStage = "KARISIK"
                )
            )
        }
        composeTestRule.waitForIdle()

        // Tap Sonlandir -> dialog shows optional before & after lap sections
        composeTestRule.onNodeWithTag("stopwatch_save_btn").assertTextEquals("Sonlandır")
        composeTestRule.onNodeWithTag("stopwatch_save_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stopwatch_save_stage_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stopwatch_karisik_before_lap_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stopwatch_karisik_after_lap_section").assertIsDisplayed()

        // 1. Confirm WITHOUT choosing optional subjects -> saves single "Karışık" session
        composeTestRule.onNodeWithTag("stopwatch_confirm_save_btn").performClick()
        composeTestRule.waitForIdle()
        val sessionsAfterDefault = db.studySessionDao().getAllSessionsOnce()
        assertEquals(1, sessionsAfterDefault.size)
        assertEquals("Karışık", sessionsAfterDefault.first().subject)
        assertEquals("KARISIK", sessionsAfterDefault.first().examStage)

        // 2. Run again with lap and pick Matematik before lap, Fizik after lap -> splits into 2 sessions
        db.studySessionDao().clearAllSessions()
        composeTestRule.runOnUiThread {
            val baseNow = System.currentTimeMillis()
            ActiveTimerManager.startStopwatch(context, "Karışık")
            snapFlow.value = snapFlow.value.copy(
                stopwatch = snapFlow.value.stopwatch.copy(
                    active = true,
                    isRunning = true,
                    startEpochMs = baseNow - 120_000L,
                    accumulatedMs = 0L,
                    subject = "Karışık",
                    examStage = "KARISIK"
                )
            )
            vm.addStopwatchLap()
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            val baseNow = System.currentTimeMillis()
            snapFlow.value = snapFlow.value.copy(
                stopwatch = snapFlow.value.stopwatch.copy(
                    startEpochMs = baseNow - 180_000L
                )
            )
            vm.pauseStopwatch()
        }
        composeTestRule.waitForIdle()
        assertTrue(vm.stopwatchElapsedMillis.value >= 180_000L)
        assertEquals(1, vm.stopwatchLaps.value.size)

        composeTestRule.onNodeWithTag("stopwatch_save_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sw_before_lap_Matematik").performClick()
        composeTestRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
                .and(androidx.compose.ui.test.hasAnyAncestor(hasTestTag("stopwatch_karisik_after_lap_section")))
        ).performScrollToNode(hasTestTag("sw_after_lap_Fizik"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sw_after_lap_Fizik").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stopwatch_confirm_save_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking { db.studySessionDao().getAllSessionsOnce().size == 2 }
        }
        val splitSessions = db.studySessionDao().getAllSessionsOnce()
        assertEquals(2, splitSessions.size)
        assertTrue(splitSessions.any { it.subject == "Matematik" && it.durationSeconds in 118L..125L && it.examStage == "KARISIK" })
        assertTrue(splitSessions.any { it.subject == "Fizik" && it.durationSeconds in 55L..65L && it.examStage == "KARISIK" })
    }
}
