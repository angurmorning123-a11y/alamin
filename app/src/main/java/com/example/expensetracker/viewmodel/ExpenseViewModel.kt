package com.example.expensetracker.viewmodel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val repository = ExpenseRepository(db.expenseDao(), db.categoryBudgetDao(), PreferencesStore(application))
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categoryBudgets = repository.categoryBudgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val language = repository.preferences.language.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")
    val theme = repository.preferences.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val totalBudget = repository.preferences.totalBudget.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()
    fun setMonth(month: YearMonth) { _selectedMonth.value = month }
    fun save(expense: Expense) = viewModelScope.launch { repository.saveExpense(expense) }
    fun delete(expense: Expense) = viewModelScope.launch { repository.deleteExpense(expense) }
    fun setLanguage(value: String) = viewModelScope.launch { repository.preferences.setLanguage(value) }
    fun setTheme(value: String) = viewModelScope.launch { repository.preferences.setTheme(value) }
    fun setTotalBudget(value: Double) = viewModelScope.launch { repository.preferences.setTotalBudget(value) }
    fun setCategoryBudget(category: String, value: Double) = viewModelScope.launch { repository.setCategoryBudget(category, value) }
    fun importBackup(expenses: List<Expense>, budgets: List<CategoryBudget>, budget: Double) = viewModelScope.launch {
        repository.replaceBackup(expenses, budgets); repository.preferences.setTotalBudget(budget)
    }
    fun exportJson(): String {
        val expenseJson = expenses.value.joinToString(",") {
            """{"id":${it.id},"amount":${it.amount},"date":"${it.date.escapeJson()}","category":"${it.category.escapeJson()}","note":"${it.note.escapeJson()}"}"""
        }
        val budgetJson = categoryBudgets.value.joinToString(",") {
            """{"category":"${it.category.escapeJson()}","monthlyLimit":${it.monthlyLimit}}"""
        }
        return """{"expenses":[$expenseJson],"categoryBudgets":[$budgetJson],"totalBudget":${totalBudget.value}}"""
    }
    private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    companion object {
        val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Other")
        fun monthOf(date: String): YearMonth? = runCatching { YearMonth.from(LocalDate.parse(date)) }.getOrNull()
    }
}
