package com.finctrl.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query(
        "SELECT t.*, c.name AS categoryName, c.colorHex AS categoryColor FROM transactions t " +
            "LEFT JOIN categories c ON c.id = t.categoryId ORDER BY t.date DESC, t.id DESC"
    )
    fun observeTransactions(): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): TransactionEntity?

    @Upsert
    suspend fun upsertTransaction(t: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT type, SUM(amount) AS total FROM transactions WHERE date BETWEEN :from AND :to GROUP BY type")
    fun observeTotals(from: Long, to: Long): Flow<List<TypeSum>>

    @Query(
        "SELECT categoryId, SUM(amount) AS total FROM transactions " +
            "WHERE type = 'EXPENSE' AND date BETWEEN :from AND :to GROUP BY categoryId"
    )
    fun observeExpenseByCategory(from: Long, to: Long): Flow<List<CategorySum>>

    @Query("SELECT * FROM categories ORDER BY type, name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Upsert
    suspend fun upsertCategory(c: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT * FROM budgets WHERE month = :month")
    fun observeBudgets(month: String): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsertBudget(b: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    @Query("SELECT * FROM categories") suspend fun allCategories(): List<CategoryEntity>
    @Query("SELECT * FROM transactions") suspend fun allTransactions(): List<TransactionEntity>
    @Query("SELECT * FROM budgets") suspend fun allBudgets(): List<BudgetEntity>
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Upsert suspend fun upsertCategories(list: List<CategoryEntity>)
    @Upsert suspend fun upsertTransactions(list: List<TransactionEntity>)
    @Upsert suspend fun upsertBudgets(list: List<BudgetEntity>)
}
