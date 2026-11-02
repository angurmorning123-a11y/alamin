package com.example.expensetracker.data
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao, private val budgetDao: CategoryBudgetDao,
                        val preferences: PreferencesStore) {
    val expenses: Flow<List<Expense>> = expenseDao.observeAll()
    val categoryBudgets: Flow<List<CategoryBudget>> = budgetDao.observeAll()
    suspend fun saveExpense(expense: Expense) { if (expense.id == 0L) expenseDao.insert(expense) else expenseDao.update(expense) }
    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)
    suspend fun setCategoryBudget(category: String, limit: Double) { budgetDao.upsert(CategoryBudget(category, limit.coerceAtLeast(0.0))) }
    suspend fun replaceBackup(expenses: List<Expense>, budgets: List<CategoryBudget>) {
        expenseDao.clear(); budgetDao.clear()
        expenseDao.insertAll(expenses.map { it.copy(id = 0) }); budgetDao.insertAll(budgets)
    }
}
