package com.finctrl.app

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finctrl.app.data.local.AppDatabase
import com.finctrl.app.data.local.BudgetEntity
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.FinanceDao
import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class FinanceDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: FinanceDao

    private val oct1 = LocalDate.of(2026, 10, 1).toEpochDay()
    private val oct31 = LocalDate.of(2026, 10, 31).toEpochDay()
    private val nov1 = LocalDate.of(2026, 11, 1).toEpochDay()

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.dao()
    }

    @After fun tearDown() = db.close()

    private suspend fun category(name: String, type: TxType = TxType.EXPENSE) =
        dao.upsertCategory(CategoryEntity(name = name, type = type))

    @Test fun insertedTransaction_isReturnedWithCategoryName() = runTest {
        val food = category("Jedzenie")
        dao.upsertTransaction(TransactionEntity(amount = 25_00, type = TxType.EXPENSE, categoryId = food, note = "obiad", date = oct1))

        val item = dao.observeTransactions().first().single()
        assertEquals("Jedzenie", item.categoryName)
        assertEquals(25_00L, item.transaction.amount)
        assertEquals("obiad", item.transaction.note)
    }

    @Test fun upsertWithExistingId_updatesInsteadOfDuplicating() = runTest {
        val food = category("Jedzenie")
        val id = dao.upsertTransaction(TransactionEntity(amount = 10_00, type = TxType.EXPENSE, categoryId = food, date = oct1))
        dao.upsertTransaction(TransactionEntity(id = id, amount = 99_00, type = TxType.EXPENSE, categoryId = food, date = oct1))

        val all = dao.allTransactions()
        assertEquals(1, all.size)
        assertEquals(99_00L, all.single().amount)
    }

    @Test fun deleteTransaction_removesRow() = runTest {
        val id = dao.upsertTransaction(TransactionEntity(amount = 1_00, type = TxType.INCOME, categoryId = null, date = oct1))
        dao.deleteTransaction(id)
        assertNull(dao.getTransaction(id))
    }

    @Test fun totals_includeOnlyTransactionsFromRequestedMonth() = runTest {
        val food = category("Jedzenie")
        val salary = category("Pensja", TxType.INCOME)
        dao.upsertTransaction(TransactionEntity(amount = 30_00, type = TxType.EXPENSE, categoryId = food, date = oct1))
        dao.upsertTransaction(TransactionEntity(amount = 20_00, type = TxType.EXPENSE, categoryId = food, date = oct31))
        dao.upsertTransaction(TransactionEntity(amount = 5000_00, type = TxType.INCOME, categoryId = salary, date = oct31))
        dao.upsertTransaction(TransactionEntity(amount = 999_00, type = TxType.EXPENSE, categoryId = food, date = nov1))

        val totals = dao.observeTotals(oct1, oct31).first().associate { it.type to it.total }
        assertEquals(50_00L, totals[TxType.EXPENSE])
        assertEquals(5000_00L, totals[TxType.INCOME])

        val byCategory = dao.observeExpenseByCategory(oct1, oct31).first()
        assertEquals(listOf(food to 50_00L), byCategory.map { it.categoryId to it.total })
    }

    @Test fun deletingCategory_keepsTransactions_andRemovesItsBudgets() = runTest {
        val food = category("Jedzenie")
        val txId = dao.upsertTransaction(TransactionEntity(amount = 10_00, type = TxType.EXPENSE, categoryId = food, date = oct1))
        dao.upsertBudget(BudgetEntity(categoryId = food, month = "2026-10", limitAmount = 500_00))

        dao.deleteCategory(food)

        assertNull(dao.getTransaction(txId)!!.categoryId)
        assertTrue(dao.observeBudgets("2026-10").first().isEmpty())
    }

    @Test fun budget_isUniquePerCategoryAndMonth() = runTest {
        val food = category("Jedzenie")
        dao.upsertBudget(BudgetEntity(categoryId = food, month = "2026-10", limitAmount = 100_00))
        try {
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO budgets (categoryId, month, limitAmount) VALUES ($food, '2026-10', 200)"
            )
            fail("Oczekiwano naruszenia unikalnego indeksu")
        } catch (expected: SQLiteConstraintException) {
            assertEquals(1, dao.allBudgets().size)
        }
    }

    @Test fun transactionWithUnknownCategory_isRejectedByForeignKey() = runTest {
        try {
            dao.upsertTransaction(TransactionEntity(amount = 1_00, type = TxType.EXPENSE, categoryId = 12345, date = oct1))
            fail("Oczekiwano naruszenia klucza obcego")
        } catch (expected: SQLiteConstraintException) {
            assertTrue(dao.allTransactions().isEmpty())
        }
    }
}
