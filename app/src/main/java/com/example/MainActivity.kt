package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.DashboardScreen
import com.example.ui.DailyReportsScreen
import com.example.ui.MainViewModel
import com.example.ui.SpeechPracticeScreen
import com.example.ui.WritingWorkspaceScreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.LightPillBg
import com.example.ui.theme.DeepNavyText
import com.example.ui.theme.MyApplicationTheme

enum class BetterTalkTab(val title: String, val icon: ImageVector, val testTag: String) {
    Dashboard("Hub", Icons.Default.GridView, "tab_dashboard"),
    Writing("Drafts", Icons.Default.EditNote, "tab_writing"),
    Voice("Voice", Icons.Default.Mic, "tab_voice"),
    Reports("Reviews", Icons.Default.Analytics, "tab_reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            var currentTab by remember { mutableStateOf(BetterTalkTab.Dashboard) }

            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 0.dp,
                            modifier = Modifier.drawBehind {
                                drawLine(
                                    color = Color(0xFFF1F5F9),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        ) {
                            BetterTalkTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DeepNavyText,
                                        selectedTextColor = DeepNavyText,
                                        indicatorColor = LightPillBg,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        BetterTalkTab.Dashboard -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToWriting = { suggestedTemplate ->
                                    viewModel.updateDraftInput(suggestedTemplate)
                                    currentTab = BetterTalkTab.Writing
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        BetterTalkTab.Writing -> {
                            WritingWorkspaceScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        BetterTalkTab.Voice -> {
                            SpeechPracticeScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        BetterTalkTab.Reports -> {
                            DailyReportsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
