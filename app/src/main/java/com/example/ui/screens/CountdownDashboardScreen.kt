package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.YKSExamCountdown

@Composable
fun CountdownDashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToPomodoro: () -> Unit,
    onNavigateToStopwatch: () -> Unit
) {
    val tytCountdown by viewModel.tytCountdown.collectAsStateWithLifecycle()
    val aytCountdown by viewModel.aytCountdown.collectAsStateWithLifecycle()
    val lgsCountdown by viewModel.lgsCountdown.collectAsStateWithLifecycle()
    val examTarget by viewModel.examTarget.collectAsStateWithLifecycle()
    val usageDays by viewModel.usageDaysCount.collectAsStateWithLifecycle()
    val todayTotalSeconds by viewModel.todayTotalSeconds.collectAsStateWithLifecycle()
    val todayPomodoros by viewModel.todayTotalPomodoros.collectAsStateWithLifecycle()

    var editingExamTarget by remember { mutableStateOf<String?>(null) }

    var isDetailExpanded by remember { mutableStateOf(false) }

    val totalSec = todayTotalSeconds ?: 0L
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & Branding
        item {
            Column {
                Text(
                    text = if (examTarget == "LGS") "LGS ODAK" else "YKS ODAK",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Şafak Kaç?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ŞAFAK HERO BANNER / COLLAPSIBLE SECTION
        item {
            val primaryCountdown = if (examTarget == "LGS") lgsCountdown else tytCountdown
            val daysLeft = primaryCountdown?.daysRemaining ?: 0L

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { isDetailExpanded = !isDetailExpanded }
                    .testTag("safak_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Şafak Kaç?",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isDetailExpanded) "Gizle ▲" else "Göster ▼",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isDetailExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = com.example.widget.ExamWidgetUpdater.formatCountdownText(
                                if (examTarget == "LGS") "LGS" else "YKS",
                                daysLeft,
                                true
                            ),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${if (examTarget == "LGS") "LGS" else "YKS - TYT"} Sınavına $daysLeft Gün Kaldı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bu uygulamayı $usageDays gündür istikrarla kullanıyorsun",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sınav Sayaçlarını görmek için dokun",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (isDetailExpanded) {
            if (examTarget == "LGS") {
                // LGS Countdown Banner Card
                item {
                    lgsCountdown?.let { countdown ->
                        CountdownCard(
                            countdown = countdown,
                            gradientColors = listOf(Color(0xFF0D9488), Color(0xFF1E293B)),
                            badgeText = "LGS",
                            onEditDate = { editingExamTarget = "LGS" }
                        )
                    }
                }
            } else {
                // TYT Countdown Banner Card
                item {
                    tytCountdown?.let { countdown ->
                        CountdownCard(
                            countdown = countdown,
                            gradientColors = listOf(Color(0xFF3730A3), Color(0xFF1E293B)),
                            badgeText = "TYT",
                            onEditDate = { editingExamTarget = "TYT" }
                        )
                    }
                }

                // AYT Countdown Banner Card
                item {
                    aytCountdown?.let { countdown ->
                        CountdownCard(
                            countdown = countdown,
                            gradientColors = listOf(Color(0xFF854D0E), Color(0xFF1E293B)),
                            badgeText = "AYT",
                            onEditDate = { editingExamTarget = "AYT" }
                        )
                    }
                }
            }
        }

        // Today's Study Progress Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_progress_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bugünkü İlerlemen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Hedef: 4 Saat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Total Study Time Column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AvTimer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (hours > 0) "${hours}s ${minutes}dk" else "${minutes} dk",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Çalışma Süresi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Completed Pomodoros Column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${todayPomodoros ?: 0} Blok",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pomodoro",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Progress Bar
                    val targetSeconds = 4 * 3600L
                    val progressFloat = (totalSec.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Quick Start Actions Row
        item {
            Text(
                text = "Hızlı Başlat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pomodoro Start Button
                Button(
                    onClick = onNavigateToPomodoro,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("quick_pomodoro_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pomodoro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Stopwatch Start Button
                OutlinedButton(
                    onClick = onNavigateToStopwatch,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("quick_stopwatch_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kronometre",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Daily Motivational Quote Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "“Başarı, her gün tekrarlanan küçük disiplinli çabaların toplamıdır.”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Edit Exam Date Dialog
    editingExamTarget?.let { target ->
        var yearInput by remember { mutableStateOf("2027") }
        var monthInput by remember { mutableStateOf("6") }
        var dayInput by remember { mutableStateOf("20") }
        val context = androidx.compose.ui.platform.LocalContext.current

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingExamTarget = null },
            title = { Text("$target Sınav Tarihini Düzenle") },
            text = {
                Column {
                    Text(
                        text = "ÖSYM / MEB tarafından sınav tarihi ertelenir veya değiştirilirse buradan güncelleyebilirsiniz:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = dayInput,
                            onValueChange = { dayInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Gün") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = monthInput,
                            onValueChange = { monthInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Ay") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Yıl") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = dayInput.toIntOrNull() ?: 20
                        val m = (monthInput.toIntOrNull() ?: 6) - 1 // 0-based calendar month
                        val y = yearInput.toIntOrNull() ?: 2027

                        val cal = java.util.Calendar.getInstance().apply {
                            set(y, m, d, 10, 15, 0)
                        }
                        viewModel.updateExamDate(target, cal.timeInMillis)
                        editingExamTarget = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingExamTarget = null }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun CountdownCard(
    countdown: YKSExamCountdown,
    gradientColors: List<Color>,
    badgeText: String,
    onEditDate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countdown.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.IconButton(
                            onClick = onEditDate,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                contentDescription = "Sınav Tarihini Düzenle",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CountdownUnit(value = countdown.daysRemaining, label = "Gün")
                    CountdownUnit(value = countdown.hoursRemaining, label = "Saat")
                    CountdownUnit(value = countdown.minutesRemaining, label = "Dakika")
                    CountdownUnit(value = countdown.secondsRemaining, label = "Saniye")
                }
            }
        }
    }
}

@Composable
fun CountdownUnit(value: Long, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = String.format("%02d", value),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
