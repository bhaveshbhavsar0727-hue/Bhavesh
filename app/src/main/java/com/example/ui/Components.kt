package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import com.example.ui.theme.*

@Composable
fun CalmingCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = Color.White
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = SoftBorder,
                shape = RoundedCornerShape(32.dp)
            ),
        shape = RoundedCornerShape(32.dp),
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = content
    )
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ProfBlue,
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = LightPillBg,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(6.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = InkDark
        )
    }
}

/**
 * Animated real-time visualizer showing user mic volume.
 */
@Composable
fun WaveformVisualizer(
    volumeFlow: StateFlow<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val volume by volumeFlow.collectAsState(initial = 0f)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, SoftBorder, RoundedCornerShape(16.dp))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val amp = if (isRecording) (volume * height * 0.45f).coerceAtLeast(4f) else 4f

        val path = Path()
        val path2 = Path()

        for (x in 0..width.toInt() step 5) {
            val progress = x.toFloat() / width
            val scale = sin(progress * PI.toFloat()) // taper at edges
            val y1 = centerY + amp * scale * sin(x * 0.03f + phase)
            val y2 = centerY - amp * scale * cos(x * 0.02f + phase + 1f)

            if (x == 0) {
                path.moveTo(0f, y1)
                path2.moveTo(0f, y2)
            } else {
                path.lineTo(x.toFloat(), y1)
                path2.lineTo(x.toFloat(), y2)
            }
        }

        drawPath(
            path = path,
            color = ProfBlue,
            style = Stroke(width = 3.dp.toPx())
        )
        drawPath(
            path = path2,
            color = ProfBlue.copy(alpha = 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Real-time frequency tracker plot with guidelines.
 */
@Composable
fun PitchChart(
    pitchHistoryFlow: StateFlow<List<Float>>,
    livePitchFlow: StateFlow<Float>,
    modifier: Modifier = Modifier
) {
    val history by pitchHistoryFlow.collectAsState(initial = emptyList())
    val livePitch by livePitchFlow.collectAsState(initial = 0f)

    val normalMaleMin = 85f
    val normalFemaleMax = 255f
    val historySize = history.size

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Frequency Tracker",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = ProfBlue
            )
            Text(
                text = if (livePitch > 0) "${livePitch.toInt()} Hz" else "--- Hz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (livePitch > normalFemaleMax) AnxietyRed else ProfBlue
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, SoftBorder, RoundedCornerShape(16.dp))
        ) {
            val width = size.width
            val height = size.height

            // Pitch boundaries we map: 50Hz to 400Hz
            val minMapHz = 50f
            val maxMapHz = 400f
            val range = maxMapHz - minMapHz

            fun hzToY(hz: Float): Float {
                val ratio = (hz - minMapHz) / range
                return height - (ratio * height).coerceIn(0f, height)
            }

            // Draw safe conversation bounds (100 Hz to 250 Hz shaded region)
            val safeYStart = hzToY(normalFemaleMax)
            val safeYEnd = hzToY(normalMaleMin)
            drawRect(
                color = ProfBlue.copy(alpha = 0.08f),
                topLeft = Offset(0f, safeYStart),
                size = androidx.compose.ui.geometry.Size(width, safeYEnd - safeYStart)
            )

            // Draw high-pitch guide line
            drawLine(
                color = AnxietyRed.copy(alpha = 0.4f),
                start = Offset(0f, safeYStart),
                end = Offset(width, safeYStart),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            // Draw low-pitch guide line
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(0f, safeYEnd),
                end = Offset(width, safeYEnd),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            // Draw historical data line
            if (historySize > 1) {
                val stepX = width / 50f
                val path = Path()
                history.forEachIndexed { index, pitch ->
                    val x = index * stepX
                    val y = hzToY(pitch)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = ProfBlue,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Deep Range (Male ~100Hz)", style = MaterialTheme.typography.labelSmall, color = InkDark.copy(alpha = 0.6f))
            Text("Anxious Spike (>250Hz)", style = MaterialTheme.typography.labelSmall, color = AnxietyRed.copy(alpha = 0.6f))
        }
    }
}

/**
 * Predefined replies & helper prompts for financial fear and conversational blocks.
 */
@Composable
fun SocialSkillPrompt(
    title: String,
    situation: String,
    suggestedText: String,
    onUseDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CalmingCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ProfBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = situation,
                style = MaterialTheme.typography.bodySmall,
                color = InkDark.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = AccentGrayCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = suggestedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                    color = InkDark
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onUseDraft(suggestedText) },
                colors = ButtonDefaults.buttonColors(containerColor = ProfBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Use Template", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
