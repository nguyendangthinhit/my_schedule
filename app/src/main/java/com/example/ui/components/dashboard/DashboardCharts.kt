package com.example.ui.components.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.dashboard.DayBarData
import com.example.models.dashboard.DayStreakItem
import com.example.models.dashboard.FourWeekPoint
import com.example.models.dashboard.TimeSlotDistribution
import kotlin.math.min

/**
 * 1. Circular Donut Chart for Weekly Progress (Image 1 top card)
 */
@Composable
fun WeeklyProgressDonutChart(
    completionRate: Int,
    completed: Int,
    inProgress: Int,
    incomplete: Int,
    modifier: Modifier = Modifier
) {
    val total = (completed + inProgress + incomplete).coerceAtLeast(1)
    val compSweep = (completed.toFloat() / total) * 360f
    val inProgSweep = (inProgress.toFloat() / total) * 360f
    val incompSweep = (incomplete.toFloat() / total) * 360f

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(completionRate) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = min(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Draw base track
            drawArc(
                color = Color(0xFFF1F5F9),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val currentScale = animProgress.value
            var startAngle = -90f

            // Completed arc (Green / Vibrant Purple Gradient)
            if (completed > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF6366F1), Color(0xFF8B5CF6)),
                        center = Offset(size.width / 2, size.height / 2)
                    ),
                    startAngle = startAngle,
                    sweepAngle = compSweep * currentScale,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += compSweep * currentScale
            }

            // In progress arc (Yellow/Orange)
            if (inProgress > 0) {
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = startAngle,
                    sweepAngle = inProgSweep * currentScale,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += inProgSweep * currentScale
            }

            // Incomplete arc (Red/Coral)
            if (incomplete > 0) {
                drawArc(
                    color = Color(0xFFEF4444),
                    startAngle = startAngle,
                    sweepAngle = incompSweep * currentScale,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$completionRate%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1)
            )
            Text(
                text = "Hoàn thành",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 2. Donut chart for Planned vs Completed (Image 1 card 3)
 */
@Composable
fun PlannedVsCompletedDonutChart(
    completed: Int,
    inProgress: Int,
    incomplete: Int,
    modifier: Modifier = Modifier
) {
    val total = (completed + inProgress + incomplete).coerceAtLeast(1)
    val compPct = (completed.toFloat() / total)
    val inProgPct = (inProgress.toFloat() / total)
    val incompPct = (incomplete.toFloat() / total)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(completed, inProgress, incomplete) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = min(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            val currentScale = animProgress.value
            var start = -90f

            // Completed (Green)
            val compSweep = compPct * 360f * currentScale
            drawArc(
                color = Color(0xFF10B981),
                startAngle = start,
                sweepAngle = compSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            start += compSweep

            // In progress (Yellow)
            val progSweep = inProgPct * 360f * currentScale
            drawArc(
                color = Color(0xFFF59E0B),
                startAngle = start,
                sweepAngle = progSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            start += progSweep

            // Incomplete (Coral)
            val incompSweep = incompPct * 360f * currentScale
            drawArc(
                color = Color(0xFFEF4444),
                startAngle = start,
                sweepAngle = incompSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

/**
 * 3. Weekly Bar Chart for Category Detail (Image 2)
 */
@Composable
fun CategoryWeeklyBarChart(
    barData: List<DayBarData>,
    primaryColor: Color = Color(0xFF10B981),
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(barData) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("100%", "75%", "50%", "25%", "0%").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Bars and Gridlines Canvas
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val chartBottom = size.height - 24.dp.toPx()
                val chartHeight = chartBottom
                val chartWidth = size.width

                // Draw 5 horizontal light gridlines
                for (i in 0..4) {
                    val y = (chartHeight / 4) * i
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                val count = barData.size.coerceAtLeast(1)
                val totalSlotWidth = chartWidth / count
                val barWidth = 14.dp.toPx()

                barData.forEachIndexed { index, item ->
                    val centerX = totalSlotWidth * index + (totalSlotWidth / 2)
                    val barHeight = (item.percentage / 100f) * chartHeight * animProgress.value
                    val topY = chartBottom - barHeight

                    val barColor = if (item.isHighlighted && item.percentage > 0) {
                        primaryColor
                    } else if (item.percentage > 0) {
                        primaryColor.copy(alpha = 0.7f)
                    } else {
                        Color(0xFFE2E8F0)
                    }

                    // Rounded top pill bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2, topY),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }
        }

        // X-Axis Day Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            barData.forEach { item ->
                Text(
                    text = item.dayLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 4. Time of Day Donut Chart (Image 2 card 3)
 */
@Composable
fun TimeDistributionDonutChart(
    distribution: List<TimeSlotDistribution>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(distribution) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = min(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            val currentScale = animProgress.value
            var start = -90f

            distribution.forEach { slot ->
                val sweep = (slot.percentage / 100f) * 360f * currentScale
                if (sweep > 0f) {
                    drawArc(
                        color = slot.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    start += sweep
                }
            }
        }
    }
}

/**
 * 5. 4-Week Trend Curved Line Chart (Image 2 bottom card)
 */
@Composable
fun FourWeekTrendLineChart(
    points: List<FourWeekPoint>,
    lineColor: Color = Color(0xFF6366F1),
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("100%", "75%", "50%", "25%", "0%").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val chartBottom = size.height - 18.dp.toPx()
                val chartHeight = chartBottom
                val chartWidth = size.width

                // Gridlines
                for (i in 0..4) {
                    val y = (chartHeight / 4) * i
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                if (points.size < 2) return@Canvas

                val count = points.size
                val stepX = chartWidth / (count - 1).coerceAtLeast(1)

                val nodeOffsets = points.mapIndexed { index, pt ->
                    val x = index * stepX
                    val y = chartBottom - ((pt.completionRate / 100f) * chartHeight * animProgress.value)
                    Offset(x, y)
                }

                // Construct smooth Bezier path
                val strokePath = Path().apply {
                    moveTo(nodeOffsets.first().x, nodeOffsets.first().y)
                    for (i in 1 until nodeOffsets.size) {
                        val prev = nodeOffsets[i - 1]
                        val curr = nodeOffsets[i]
                        val cx1 = (prev.x + curr.x) / 2
                        val cy1 = prev.y
                        val cx2 = (prev.x + curr.x) / 2
                        val cy2 = curr.y
                        cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                    }
                }

                // Fill area gradient under curve
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(nodeOffsets.last().x, chartBottom)
                    lineTo(nodeOffsets.first().x, chartBottom)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = chartBottom
                    )
                )

                // Draw line stroke
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw node dots
                nodeOffsets.forEach { node ->
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = node
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = node
                    )
                }
            }
        }

        // X-Axis Week Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.weekLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 6. Streak Row (Image 1 card 4)
 */
@Composable
fun StreakDaysRow(
    streakItems: List<DayStreakItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        streakItems.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isCompleted) Color(0xFF6366F1) else Color.Transparent
                        )
                        .then(
                            if (!item.isCompleted) {
                                Modifier.background(
                                    Color(0xFFE2E8F0).copy(alpha = 0.4f),
                                    CircleShape
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = item.dayLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.isToday) Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 7. Horizontal Progress Bar for Category summary
 */
@Composable
fun CategoryHorizontalProgressBar(
    progress: Float,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animProgress.snapTo(0f)
        animProgress.animateTo(progress.coerceIn(0f, 1f), animationSpec = tween(durationMillis = 700))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF1F5F9))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animProgress.value)
                .clip(RoundedCornerShape(4.dp))
                .background(primaryColor)
        )
    }
}
