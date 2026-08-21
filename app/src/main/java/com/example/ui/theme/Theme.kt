package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryPurpleLight,
    secondary = PrimaryPurple,
    tertiary = CategoryWork,
    background = TextPrimary,
    surface = TextPrimary,
    onPrimary = SurfaceWhite,
    onBackground = SurfaceWhite,
    onSurface = SurfaceWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    secondary = PrimaryPurpleLight,
    tertiary = CategoryWork,
    background = PurpleBackground,
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary
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
