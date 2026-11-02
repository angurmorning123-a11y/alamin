package com.example.expensetracker
import com.example.expensetracker.domain.BudgetMath
import com.example.expensetracker.domain.BudgetStatus
import org.junit.Assert.*
import org.junit.Test

class BudgetMathTest {
    @Test fun budgetThresholdsAreCorrect() {
        assertEquals(BudgetStatus.OK, BudgetMath.status(79.0, 100.0))
        assertEquals(BudgetStatus.WARNING, BudgetMath.status(80.0, 100.0))
        assertEquals(BudgetStatus.EXCEEDED, BudgetMath.status(101.0, 100.0))
    }
    @Test fun monthComparisonIsCorrect() {
        assertEquals(25.0, BudgetMath.monthChange(125.0, 100.0)!!, 0.001)
        assertEquals(-20.0, BudgetMath.monthChange(80.0, 100.0)!!, 0.001)
        assertNull(BudgetMath.monthChange(10.0, 0.0))
    }
}
