package com.finctrl.app.data.repo

import androidx.room.withTransaction
import com.finctrl.app.data.local.AppDatabase
import com.finctrl.app.data.local.BudgetEntity
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.CategorySum
import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TransactionWithCategory
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.local.TypeSum
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

data class Snapshot(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
)

interface FinanceRepository {
    fun observeTransactions(): Flow<List<TransactionWithCategory>>
    fun observeCategories(): Flow<List<CategoryEntity>>
    fun observeBudgets(month: YearMonth): Flow<List<BudgetEntity>>
    fun observeTotals(month: YearMonth): Flow<List<TypeSum>>
    fun observeExpenseByCategory(month: YearMonth): Flow<List<CategorySum>>

    suspend fun getTransaction(id: Long): TransactionEntity?
    suspend fun upsertTransaction(t: TransactionEntity)
    suspend fun deleteTransaction(id: Long)
    suspend fun upsertCategory(c: CategoryEntity)
    suspend fun deleteCategory(id: Long)
    suspend fun upsertBudget(b: BudgetEntity)
    suspend fun deleteBudget(id: Long)

    suspend fun seedDefaultsIfEmpty()
    suspend fun snapshot(): Snapshot
    suspend fun replaceAll(snapshot: Snapshot)
}

class RoomFinanceRepository(private val db: AppDatabase) : FinanceRepository {
    private val dao = db.dao()

    private fun YearMonth.range() = atDay(1).toEpochDay() to atEndOfMonth().toEpochDay()

    override fun observeTransactions() = dao.observeTransactions()
    override fun observeCategories() = dao.observeCategories()
    override fun observeBudgets(month: YearMonth) = dao.observeBudgets(month.toString())
    override fun observeTotals(month: YearMonth) = month.range().let { dao.observeTotals(it.first, it.second) }
    override fun observeExpenseByCategory(month: YearMonth) =
        month.range().let { dao.observeExpenseByCategory(it.first, it.second) }

    override suspend fun getTransaction(id: Long) = dao.getTransaction(id)
    override suspend fun upsertTransaction(t: TransactionEntity) { dao.upsertTransaction(t) }
    override suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)
    override suspend fun upsertCategory(c: CategoryEntity) { dao.upsertCategory(c) }
    override suspend fun deleteCategory(id: Long) = dao.deleteCategory(id)
    override suspend fun upsertBudget(b: BudgetEntity) { dao.upsertBudget(b) }
    override suspend fun deleteBudget(id: Long) = dao.deleteBudget(id)

    override suspend fun seedDefaultsIfEmpty() {
        if (dao.categoryCount() > 0) return
        dao.upsertCategories(DEFAULT_CATEGORIES)
    }

    override suspend fun snapshot() = db.withTransaction {
        Snapshot(dao.allCategories(), dao.allTransactions(), dao.allBudgets())
    }

    override suspend fun replaceAll(snapshot: Snapshot) = db.withTransaction {
        dao.clearTransactions(); dao.clearBudgets(); dao.clearCategories()
        dao.upsertCategories(snapshot.categories)
        dao.upsertTransactions(snapshot.transactions)
        dao.upsertBudgets(snapshot.budgets)
    }

    private companion object {
        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Wynagrodzenie", type = TxType.INCOME, colorHex = "#2E7D32"),
            CategoryEntity(name = "Inne przychody", type = TxType.INCOME, colorHex = "#00897B"),
            CategoryEntity(name = "Jedzenie", type = TxType.EXPENSE, colorHex = "#EF6C00"),
            CategoryEntity(name = "Transport", type = TxType.EXPENSE, colorHex = "#1565C0"),
            CategoryEntity(name = "Mieszkanie", type = TxType.EXPENSE, colorHex = "#6A1B9A"),
            CategoryEntity(name = "Rozrywka", type = TxType.EXPENSE, colorHex = "#C2185B"),
        )
    }
}
