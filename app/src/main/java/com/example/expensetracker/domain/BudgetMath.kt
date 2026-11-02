package com.example.expensetracker.domain
object BudgetMath {
    fun usage(spent: Double, budget: Double): Double = if (budget <= 0.0) 0.0 else spent / budget
    fun status(spent: Double, budget: Double): BudgetStatus = when {
        budget <= 0.0 -> BudgetStatus.NONE
        spent > budget -> BudgetStatus.EXCEEDED
        spent >= budget * 0.8 -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
    fun monthChange(current: Double, previous: Double): Double? {
        if (previous == 0.0) return if (current == 0.0) 0.0 else null
        return ((current - previous) / previous) * 100.0
    }
}
enum class BudgetStatus { NONE, OK, WARNING, EXCEEDED }
