package com.example.expensetracker.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF172B4D)
private val Gold = Color(0xFFD9A441)
private val LightScheme = lightColorScheme(
    primary = Navy, secondary = Gold, background = Color(0xFFF7F8FC),
    surface = Color.White, onPrimary = Color.White, onSecondary = Navy
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB8C9EF), secondary = Color(0xFFE5B95C),
    background = Color(0xFF101722), surface = Color(0xFF192332),
    onPrimary = Color(0xFF10213D), onSecondary = Color(0xFF18243A)
)
@Composable
fun ExpenseTrackerTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}
