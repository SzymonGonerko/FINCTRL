package com.finctrl.app

import com.finctrl.app.data.local.BudgetEntity
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.CategorySum
import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TransactionWithCategory
import com.finctrl.app.data.local.TypeSum
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.data.repo.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.YearMonth

class FakeFinanceRepository(categories: List<CategoryEntity>) : FinanceRepository {
    private val cats = MutableStateFlow(categories)
    val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override fun observeTransactions(): Flow<List<TransactionWithCategory>> = transactions.map { list ->
        list.map { t -> cats.value.firstOrNull { it.id == t.categoryId }.let { TransactionWithCategory(t, it?.name, it?.colorHex) } }
    }
    override fun observeCategories(): Flow<List<CategoryEntity>> = cats
    override fun observeBudgets(month: YearMonth): Flow<List<BudgetEntity>> = flowOf(emptyList())
    override fun observeTotals(month: YearMonth): Flow<List<TypeSum>> = flowOf(emptyList())
    override fun observeExpenseByCategory(month: YearMonth): Flow<List<CategorySum>> = flowOf(emptyList())

    override suspend fun getTransaction(id: Long) = transactions.value.firstOrNull { it.id == id }
    override suspend fun upsertTransaction(t: TransactionEntity) {
        val withId = if (t.id == 0L) t.copy(id = (transactions.value.maxOfOrNull { it.id } ?: 0) + 1) else t
        transactions.value = transactions.value.filterNot { it.id == withId.id } + withId
    }
    override suspend fun deleteTransaction(id: Long) { transactions.value = transactions.value.filterNot { it.id == id } }
    override suspend fun upsertCategory(c: CategoryEntity) { cats.value = cats.value.filterNot { it.id == c.id } + c }
    override suspend fun deleteCategory(id: Long) { cats.value = cats.value.filterNot { it.id == id } }
    override suspend fun upsertBudget(b: BudgetEntity) = Unit
    override suspend fun deleteBudget(id: Long) = Unit
    override suspend fun seedDefaultsIfEmpty() = Unit
    override suspend fun snapshot() = Snapshot(cats.value, transactions.value, emptyList())
    override suspend fun replaceAll(snapshot: Snapshot) {
        cats.value = snapshot.categories; transactions.value = snapshot.transactions
    }
}
