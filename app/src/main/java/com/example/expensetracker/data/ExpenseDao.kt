package com.example.expensetracker.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC") fun observeAll(): Flow<List<Expense>>
    @Insert suspend fun insert(expense: Expense): Long
    @Update suspend fun update(expense: Expense)
    @Delete suspend fun delete(expense: Expense)
    @Query("DELETE FROM expenses") suspend fun clear()
    @Insert suspend fun insertAll(items: List<Expense>)
}
