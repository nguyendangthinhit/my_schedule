package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(EventCategory.WORK) }
    var hasReminder by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Thêm sự kiện", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { /* TODO: Save Event */ onBack() }) {
                        Text("Lưu", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Title field
            SectionLabel("Tiêu đề sự kiện")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Nhập tiêu đề") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedBorderColor = PrimaryPurple
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Date & Time
            SectionLabel("Ngày & giờ")
            OutlinedTextField(
                value = "Thứ Năm, 22/05/2025   09:00",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedBorderColor = PrimaryPurple
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Note
            SectionLabel("Nội dung nhắc hẹn")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Nhập nội dung chi tiết...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedBorderColor = PrimaryPurple
                ),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Category
            SectionLabel("Danh mục")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategorySelectionCard(
                    category = EventCategory.WORK,
                    isSelected = selectedCategory == EventCategory.WORK,
                    onClick = { selectedCategory = EventCategory.WORK }
                )
                CategorySelectionCard(
                    category = EventCategory.STUDY,
                    isSelected = selectedCategory == EventCategory.STUDY,
                    onClick = { selectedCategory = EventCategory.STUDY }
                )
                CategorySelectionCard(
                    category = EventCategory.PLAY,
                    isSelected = selectedCategory == EventCategory.PLAY,
                    onClick = { selectedCategory = EventCategory.PLAY }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Reminder options
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Nhắc hẹn", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
                        )
                    }
                    
                    if (hasReminder) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thời gian nhắc", fontSize = 16.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("15 phút trước", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CategorySelectionCard(
    category: EventCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryPurple else Color(0xFFEEEEEE)
    val bgColor = Color.White
    
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(90.dp)
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = category.color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = category.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(24.dp)
                    .background(PrimaryPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
