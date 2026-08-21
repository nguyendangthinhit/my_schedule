package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.example.models.Event
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(onNavigateToAddEvent: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Lịch hoạt động", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                // Toggle Group
                Row(
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                        .padding(4.dp)
                ) {
                    ToggleButton(text = "Ngày", isSelected = false)
                    ToggleButton(text = "Tuần", isSelected = true)
                    ToggleButton(text = "Tháng", isSelected = false)
                }
            }
            
            // Month Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                }
                Text(text = "Tháng 5, 2025", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            
            // Weekday Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("T2" to "19", "T3" to "20", "T4" to "21", "T5" to "22", "T6" to "23", "T7" to "24", "CN" to "25")
                days.forEach { (day, date) ->
                    val isToday = date == "22"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(if (isToday) PrimaryPurple else Color.Transparent, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(text = day, fontSize = 12.sp, color = if (isToday) Color.White else Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = date, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isToday) Color.White else Color.Black)
                    }
                }
            }
            
            Divider(color = Color(0xFFEEEEEE))
            
            // Timetable Grid (Simplified for UI mockup)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(13) { index ->
                    val hour = index + 7 // 07:00 to 19:00
                    val timeString = String.format("%02d:00", hour)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        // Background grid line
                        Column {
                            Spacer(modifier = Modifier.weight(1f))
                            Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(start = 60.dp))
                        }
                        
                        // Time label
                        Text(
                            text = timeString,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .align(Alignment.CenterStart)
                        )
                        
                        // Render mock events at specific hours
                        when (hour) {
                            8 -> EventBlock(title = "Họp team", time = "08:00", category = EventCategory.WORK, offset = 1)
                            9 -> EventBlock(title = "Họp dự án UI/UX", time = "09:00 - 10:30", category = EventCategory.WORK, offset = 0, height = 90.dp)
                            10 -> EventBlock(title = "Gặp khách hàng", time = "10:00 - 12:00", category = EventCategory.WORK, offset = 2, height = 120.dp, bgColor = Color(0xFFFFF9C4), color = Color(0xFFF9A825))
                            11 -> EventBlock(title = "Học Kotlin", time = "11:30 - 12:30", category = EventCategory.STUDY, offset = 0)
                            14 -> EventBlock(title = "Đi gym", time = "14:00 - 15:00", category = EventCategory.PLAY, offset = 0)
                            16 -> EventBlock(title = "Ôn tập DS & GT", time = "16:30 - 17:30", category = EventCategory.STUDY, offset = 0)
                            18 -> EventBlock(title = "Hẹn ăn tối", time = "18:00 - 19:30", category = EventCategory.PLAY, offset = 1, bgColor = Color(0xFFEDE7F6), color = PrimaryPurple)
                        }
                        
                        // Current time indicator line
                        if (hour == 10) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 60.dp)
                                    .align(Alignment.BottomStart)
                            ) {
                                Divider(color = Color.Red, thickness = 1.dp)
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.Red, CircleShape)
                                        .align(Alignment.CenterStart)
                                        .offset(x = (-3).dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Floating Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = { /*TODO*/ },
                containerColor = Color.White,
                contentColor = Color.Red,
                shape = CircleShape,
                modifier = Modifier.border(1.dp, Color(0xFFEEEEEE), CircleShape)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Delete Event")
            }
            
            FloatingActionButton(
                onClick = onNavigateToAddEvent,
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    }
}

@Composable
fun ToggleButton(text: String, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) PrimaryPurple else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BoxScope.EventBlock(
    title: String, 
    time: String, 
    category: EventCategory, 
    offset: Int, 
    height: androidx.compose.ui.unit.Dp = 60.dp,
    bgColor: Color = category.bgColor,
    color: Color = category.color
) {
    val blockWidth = 100.dp
    val startPadding = 60.dp + (blockWidth * offset) + (8.dp * offset)
    
    Column(
        modifier = Modifier
            .padding(start = startPadding, top = 2.dp)
            .width(blockWidth)
            .height(height - 4.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(text = time, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = title, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}
