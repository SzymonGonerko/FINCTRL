package com.finctrl.app

import app.cash.turbine.test
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.data.local.BudgetEntity
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.CategorySum
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.screens.BudgetsViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {
    private val repo = mockk<FinanceRepository>(relaxed = true)
    private val now = YearMonth.now()
    private val food = CategoryEntity(3, "Jedzenie", TxType.EXPENSE)
    private val salary = CategoryEntity(1, "Wynagrodzenie", TxType.INCOME)

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.observeCategories() } returns flowOf(listOf(salary, food))
        every { repo.observeBudgets(any()) } returns flowOf(listOf(BudgetEntity(10, food.id, now.toString(), 100_00)))
        every { repo.observeExpenseByCategory(any()) } returns flowOf(listOf(CategorySum(food.id, 150_00)))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.subscribed(vm: BudgetsViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

    @Test fun `combines budget with spending and flags exceeded limit`() = runTest {
        val vm = BudgetsViewModel(repo, ErrorHandler())
        subscribed(vm)

        val item = vm.state.value.items.single()
        assertEquals("Jedzenie", item.categoryName)
        assertEquals(150_00L, item.spent)
        assertTrue(item.exceeded)
        assertEquals(1f, item.progress, 0.0001f)
        assertEquals(listOf(food), vm.state.value.expenseCategories)
    }

    @Test fun `budget without spending is not exceeded`() = runTest {
        every { repo.observeExpenseByCategory(any()) } returns flowOf(emptyList())
        val vm = BudgetsViewModel(repo, ErrorHandler())
        subscribed(vm)

        val item = vm.state.value.items.single()
        assertEquals(0L, item.spent)
        assertFalse(item.exceeded)
        assertEquals(0f, item.progress, 0.0001f)
    }

    @Test fun `switching month reloads data for that month`() = runTest {
        val vm = BudgetsViewModel(repo, ErrorHandler())
        subscribed(vm)

        vm.prev()
        assertEquals(now.minusMonths(1), vm.state.value.month)
        verify { repo.observeBudgets(now.minusMonths(1)) }

        vm.next(); vm.next()
        assertEquals(now.plusMonths(1), vm.state.value.month)
    }

    @Test fun `save without category reports validation error and does not write`() = runTest {
        val vm = BudgetsViewModel(repo, ErrorHandler())
        vm.events.test {
            vm.save(categoryId = null, limitRaw = "100")
            assertEquals("Wybierz kategorię.", awaitItem().text)
        }
        coVerify(exactly = 0) { repo.upsertBudget(any()) }
    }

    @Test fun `save with invalid limit reports validation error`() = runTest {
        val vm = BudgetsViewModel(repo, ErrorHandler())
        vm.events.test {
            vm.save(categoryId = food.id, limitRaw = "-5")
            assertEquals("Kwota musi być większa od zera.", awaitItem().text)
        }
        coVerify(exactly = 0) { repo.upsertBudget(any()) }
    }

    @Test fun `saving existing category budget updates it instead of creating duplicate`() = runTest {
        val vm = BudgetsViewModel(repo, ErrorHandler())
        subscribed(vm)

        vm.save(categoryId = food.id, limitRaw = "250,50")
        coVerify {
            repo.upsertBudget(match { it.id == 10L && it.categoryId == food.id && it.limitAmount == 250_50L && it.month == now.toString() })
        }
    }
}
