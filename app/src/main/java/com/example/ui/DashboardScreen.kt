package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToWriting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.todayStats.collectAsState()
    val scrollState = rememberScrollState()

    // Breathing exercise states
    var breathingText by remember { mutableStateOf("Tap to Breathe") }
    var isBreathingActive by remember { mutableStateOf(false) }
    var breathingScale by remember { mutableStateOf(1f) }

    // Interactive Social Sync status
    var isSyncActive by remember { mutableStateOf(true) }

    // Pulsing animation for the active sync indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_active")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(isBreathingActive) {
        if (isBreathingActive) {
            while (isBreathingActive) {
                breathingText = "Breathe In..."
                animate(1f, 1.4f, animationSpec = tween(4000, easing = EaseInOutSine)) { value, _ ->
                    breathingScale = value
                }
                breathingText = "Hold..."
                delay(4000)
                breathingText = "Breathe Out..."
                animate(1.4f, 1f, animationSpec = tween(4000, easing = EaseInOutSine)) { value, _ ->
                    breathingScale = value
                }
                breathingText = "Rest..."
                delay(2000)
            }
        } else {
            breathingScale = 1f
            breathingText = "Tap to Breathe"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IceBlueBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming Header mimicking the Design HTML exactly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .border(1.dp, SoftBorder, shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BetterTalk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ProfBlue
                )
                Text(
                    text = "Guardian Mode: Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LightPillBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BB",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavyText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 1. Social Sync Active Status Card (Direct translation of HTML layout)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SoftBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSyncActive) Color(0xFF22C55E).copy(alpha = pulseAlpha)
                                    else Color.Gray
                                )
                        )
                        Text(
                            text = if (isSyncActive) "Social Sync Active" else "Social Sync Paused",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                    }
                    Text(
                        text = "Monitoring WhatsApp & Instagram",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { isSyncActive = !isSyncActive },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSyncActive) ProfBlue else Color.Gray,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isSyncActive) "Pause" else "Resume",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Today's Accomplishments Stats Block
        SectionHeader(title = "Today's Practice Meter", icon = Icons.Default.Timeline)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drafts stat
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SoftBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = ProfBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${stats.draftsCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = InkDark)
                    Text(text = "Drafts Checked", style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
            // Voice stat
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SoftBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = ProfBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${stats.sessionsCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = InkDark)
                    Text(text = "Speeches", style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
            // Mistakes log stat
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SoftBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ReassuringYellow)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${stats.totalMistakes}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = InkDark)
                    Text(text = "Mistakes Saved", style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }

        // 2. Twin Accent Metrics Grid (Confidence & Pitch Accuracy from HTML)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Confidence Accent Card (Light Blue bg)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = AccentBlueCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CONFIDENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyText.copy(alpha = 0.6f)
                    )
                    Column {
                        Text(
                            text = "84%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyText
                        )
                        Text(
                            text = "+5% from yesterday",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProfBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pitch Accuracy Accent Card (Light Gray bg)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGrayCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PITCH ACCURACY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkDark.copy(alpha = 0.6f)
                    )

                    // Waveform elements mimicking HTML div flex structure
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .background(Color.Gray.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(ProfBlue, RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .background(Color.Gray.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }

        // Calming Breathing Space
        CalmingCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Larynx Relaxation Spot",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProfBlue
                )
                Text(
                    text = "A tight throat spikes vocal pitch. Practice this 4-7-8 breathing before speaking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(130.dp * breathingScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ProfBlue.copy(alpha = 0.4f),
                                    ProfBlue.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .clickable { isBreathingActive = !isBreathingActive },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isBreathingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ProfBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = breathingText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ProfBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 3. Recent Correction Card (Direct translation of HTML layout)
        SectionHeader(title = "Recent Correction Feed", icon = Icons.Default.AutoAwesome)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SoftBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Correction",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Social App",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Stumbling/Anxious Input (Red left border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFFF87171),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 4.dp.toPx()
                                )
                            }
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "\"I am scary about money...\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFFB91C1C)
                        )
                    }

                    // Professional Polish Corrected Output (Green left border)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFF4ADE80),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 4.dp.toPx()
                                )
                            }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "\"I feel hesitant about financial tasks.\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "PERSONALIZED TONE: PROFESSIONAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Reassurance Zone - Financial & Overthinking
        SectionHeader(title = "Coping Corner", icon = Icons.Outlined.Lightbulb)

        CalmingCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ReassuringYellow)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Managing Finances For Others?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "It is completely normal to feel scared about money mistakes. Remember:\n1. Double-check transactions once, then let it go.\n2. Mistakes are just numbers on paper—they do not define your value as a trustworthy person.\n3. Ask questions early; communication builds real trust.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        CalmingCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ProfBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Calming The Overthinking Loop",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When you feel 'blank' about what to reply, or overthink simple texts: pause for 30 seconds. Draft your response in our Writing tab first. BetterTalk will analyze it in a safe, judgment-free space so you can hit send with 100% confidence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // 4. Voice Coach Tip Card (Direct translation of HTML layout, dark charcoal card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Transparent, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = DarkTipBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Voice Coach Tip",
                        tint = LightPillBg,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice Coach Tip",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF93C5FD), // Light blue text matching HTML text-blue-300
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your pitch rose during the 2PM call. Take 3 deep breaths before your next meeting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Interactive Ready-Made Social Templates
        SectionHeader(title = "Helpful Reply Prompts", icon = Icons.Default.ChatBubbleOutline)

        SocialSkillPrompt(
            title = "Prompt: Splitting bills with roommates",
            situation = "Use this template when you feel anxious about talking about finances and need to request shared utility splits.",
            suggestedText = "Hi everyone, I just calculated the electricity bill for this month. It is \$85 in total, which works out to \$28 per person. Let me know if you want me to share the receipt. No rush at all!",
            onUseDraft = {
                onNavigateToWriting(it)
            }
        )

        SocialSkillPrompt(
            title = "Prompt: Delaying a social event",
            situation = "Use this template when you are overthinking a reply and feel overwhelmed to meet today.",
            suggestedText = "Hey! I was really looking forward to hanging out today, but I am feeling a bit tired and need to rest this evening. Can we reschedule for tomorrow or later this week? Let me know what works for you!",
            onUseDraft = {
                onNavigateToWriting(it)
            }
        )

        SocialSkillPrompt(
            title = "Prompt: Declining extra budget tasks",
            situation = "Use this template if you feel scared about handling someone else's budget and want to decline politely.",
            suggestedText = "Thank you so much for trusting me with this budget. However, because of my current workload, I am worried I won't be able to give it the close focus it needs. I think it would be safest if someone else handles the spreadsheet this time.",
            onUseDraft = {
                onNavigateToWriting(it)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
