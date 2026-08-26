package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFA5B4FC),
    onSecondary = Color(0xFF1E1B4B),
    tertiary = CategoryWork,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = PrimaryPurple,
    secondary = PrimaryPurpleLight,
    onSecondary = Color.White,
    tertiary = CategoryWork,
    background = Color(0xFFF9FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0)
  )

@Composable
fun MyScheduleTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun com.example.models.EventCategory.getThemedBgColor(): Color {
  val surface = MaterialTheme.colorScheme.surface
  val isDark = (0.299f * surface.red + 0.587f * surface.green + 0.114f * surface.blue) < 0.5f
  return getThemedBgColor(isDark)
}

@Composable
fun com.example.models.EventCategory.getThemedBorderColor(): Color {
  val surface = MaterialTheme.colorScheme.surface
  val isDark = (0.299f * surface.red + 0.587f * surface.green + 0.114f * surface.blue) < 0.5f
  return getThemedBorderColor(isDark)
}

fun com.example.models.EventCategory.getThemedBgColor(isDark: Boolean): Color {
  return if (isDark) {
    // Màu nền nguyên bản như cũ: pha chuẩn xác 20% màu danh mục trên nền tối #1E293B, đục 100% để che khuất hoàn toàn lớp dưới
    Color(
      red = (0.20f * color.red + 0.80f * (30f / 255f)).coerceIn(0f, 1f),
      green = (0.20f * color.green + 0.80f * (41f / 255f)).coerceIn(0f, 1f),
      blue = (0.20f * color.blue + 0.80f * (59f / 255f)).coerceIn(0f, 1f),
      alpha = 1.0f
    )
  } else {
    // Ở chế độ sáng, giữ nguyên màu pastel nguyên bản (bgColor) với độ đục 100% để che khuất tuyệt đối lớp dưới
    bgColor.copy(alpha = 1.0f)
  }
}

fun com.example.models.EventCategory.getThemedBorderColor(isDark: Boolean): Color {
  return if (isDark) color.copy(alpha = 0.6f) else color.copy(alpha = 0.35f)
}

