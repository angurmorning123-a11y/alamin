package com.example.expensetracker.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets") fun observeAll(): Flow<List<CategoryBudget>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: CategoryBudget)
    @Query("DELETE FROM category_budgets") suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<CategoryBudget>)
}
