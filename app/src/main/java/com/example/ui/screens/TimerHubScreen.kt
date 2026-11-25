package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import kotlinx.coroutines.launch

@Composable
fun TimerHubScreen(viewModel: StudyViewModel) {
    val lang by viewModel.userLanguage.collectAsState()
    val savedHubTab by viewModel.selectedTimerHubTab.collectAsState()
    val pagerState = rememberPagerState(initialPage = savedHubTab.coerceIn(0, 1), pageCount = { 2 })
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTimerHubTab(pagerState.currentPage)
    }

    androidx.compose.runtime.LaunchedEffect(savedHubTab) {
        val clamped = savedHubTab.coerceIn(0, 1)
        if (pagerState.currentPage != clamped) {
            pagerState.scrollToPage(clamped)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                viewModel.selectTimerHubTab(0)
                                scope.launch { pagerState.scrollToPage(0) }
                            }
                            .padding(vertical = 8.dp)
                            .testTag("timer_hub_tab_0"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("seg_timer_deneme", lang),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                viewModel.selectTimerHubTab(1)
                                scope.launch { pagerState.scrollToPage(1) }
                            }
                            .padding(vertical = 8.dp)
                            .testTag("timer_hub_tab_1"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("seg_pomodoro", lang),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 0,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            if (page == 0) {
                TimerScreen(viewModel = viewModel)
            } else {
                PomodoroScreen(viewModel = viewModel)
            }
        }
    }
}
