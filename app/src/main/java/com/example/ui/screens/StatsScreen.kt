package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import com.example.viewmodel.ScheduleViewModel

@Composable
fun StatsScreen(viewModel: ScheduleViewModel) {
    val events by viewModel.events.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val totalEvents = events.size
    val completedEvents = events.count { it.isCompleted }
    val completionRate = if (totalEvents > 0) ((completedEvents.toFloat() / totalEvents) * 100).toInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
            .padding(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = PrimaryPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thống kê hoạt động",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = "Tổng công việc",
                    value = totalEvents.toString(),
                    subtitle = "Tất cả các ngày",
                    color = PrimaryPurple,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Tỷ lệ hoàn thành",
                    value = "$completionRate%",
                    subtitle = "$completedEvents việc đã xong",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Phân bố theo danh mục", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (categories.isEmpty()) {
                        Text(
                            text = "Chưa có danh mục nào.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        categories.forEach { category ->
                            val count = events.count { it.category.id == category.id }
                            val pct = if (totalEvents > 0) count.toFloat() / totalEvents else 0f
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(category.title, fontSize = 13.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                                    Text("$count việc (${(pct * 100).toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = category.color)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = category.color,
                                    trackColor = category.bgColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}
