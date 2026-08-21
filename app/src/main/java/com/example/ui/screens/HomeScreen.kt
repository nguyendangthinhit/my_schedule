package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen() {
    val events = listOf(
        Event(
            title = "Họp dự án UI/UX",
            startTime = LocalDateTime.now().withHour(9).withMinute(0),
            endTime = LocalDateTime.now().withHour(10).withMinute(0),
            category = EventCategory.WORK
        ),
        Event(
            title = "Học Kotlin nâng cao",
            startTime = LocalDateTime.now().withHour(11).withMinute(30),
            endTime = LocalDateTime.now().withHour(12).withMinute(30),
            category = EventCategory.STUDY
        ),
        Event(
            title = "Đi gym",
            startTime = LocalDateTime.now().withHour(14).withMinute(0),
            endTime = LocalDateTime.now().withHour(15).withMinute(0),
            category = EventCategory.PLAY
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            HeaderSection()
            Spacer(modifier = Modifier.height(24.dp))
            DateSelector()
            Spacer(modifier = Modifier.height(24.dp))
            CategoriesSection()
            Spacer(modifier = Modifier.height(32.dp))
            SectionTitle(title = "Việc trong 8 giờ tới", actionText = "Xem tất cả")
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        items(events) { event ->
            EventCard(event)
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(title = "Sự kiện đặc biệt", actionText = "Xem thêm")
            Spacer(modifier = Modifier.height(16.dp))
            SpecialEventCard()
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Xin chào, Minh! \uD83D\uDC4B",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chúc bạn một ngày hiệu quả!",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
        }
    }
}

@Composable
fun DateSelector() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thứ Năm, 22 Tháng 5, 2025",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun CategoriesSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CategoryCard(title = "Tất cả", count = "8", color = PrimaryPurple, bgColor = PrimaryPurple.copy(alpha = 0.1f), isSelected = true)
        CategoryCard(title = "Công việc", count = "4", color = EventCategory.WORK.color, bgColor = EventCategory.WORK.bgColor)
        CategoryCard(title = "Học tập", count = "2", color = EventCategory.STUDY.color, bgColor = EventCategory.STUDY.bgColor)
        CategoryCard(title = "Đi chơi", count = "2", color = EventCategory.PLAY.color, bgColor = EventCategory.PLAY.bgColor)
    }
}

@Composable
fun CategoryCard(title: String, count: String, color: Color, bgColor: Color, isSelected: Boolean = false) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .height(100.dp)
            .background(bgColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.White, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if(isSelected) PrimaryPurple else Color.DarkGray)
        Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if(isSelected) PrimaryPurple else Color.Black)
    }
}

@Composable
fun SectionTitle(title: String, actionText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = actionText, fontSize = 14.sp, color = PrimaryPurple, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EventCard(event: Event) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(start = 0.dp), // To have the color bar flush left
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(event.category.color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        
        // Time
        Text(
            text = event.startTime.format(formatter),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray,
            modifier = Modifier.width(50.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(text = event.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = event.category.color, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = event.category.title, fontSize = 12.sp, color = event.category.color, fontWeight = FontWeight.Medium)
            }
        }
        
        // Notification bell
        IconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun SpecialEventCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F5FF), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryPurple)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = "01/06/2025", fontSize = 12.sp, color = Color.Gray)
            Text(text = "Ngày Quốc tế Thiếu nhi \uD83C\uDF89", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
