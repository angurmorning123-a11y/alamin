package com.example.expensetracker.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")
class PreferencesStore(private val context: Context) {
    private val languageKey = stringPreferencesKey("language")
    private val themeKey = stringPreferencesKey("theme")
    private val totalBudgetKey = doublePreferencesKey("total_budget")
    val language = context.dataStore.data.map { it[languageKey] ?: "en" }
    val theme = context.dataStore.data.map { it[themeKey] ?: "system" }
    val totalBudget = context.dataStore.data.map { it[totalBudgetKey] ?: 0.0 }
    suspend fun setLanguage(value: String) { context.dataStore.edit { it[languageKey] = value } }
    suspend fun setTheme(value: String) { context.dataStore.edit { it[themeKey] = value } }
    suspend fun setTotalBudget(value: Double) { context.dataStore.edit { it[totalBudgetKey] = value.coerceAtLeast(0.0) } }
}
