package com.finctrl.app

import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TransactionWithCategory
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.screens.TransactionsViewModel
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    private val repo = mockk<FinanceRepository>()

    private fun tx(id: Long, type: TxType, note: String, category: String?) = TransactionWithCategory(
        TransactionEntity(id, 10_00, type, categoryId = id, note = note, date = 0), category, "#000000",
    )

    private val all = listOf(
        tx(1, TxType.EXPENSE, "Obiad w barze", "Jedzenie"),
        tx(2, TxType.INCOME, "Pensja październik", "Wynagrodzenie"),
        tx(3, TxType.EXPENSE, "Bilet miesięczny", "Transport"),
        tx(4, TxType.EXPENSE, "", null),
    )

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.observeTransactions() } returns flowOf(all)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm() = TransactionsViewModel(repo, ErrorHandler()).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.items.collect {} }
    }

    private fun TransactionsViewModel.ids() = items.value.map { it.transaction.id }

    @Test fun `no filters shows everything`() = runTest {
        assertEquals(listOf(1L, 2L, 3L, 4L), vm().ids())
    }

    @Test fun `type filter keeps only matching type`() = runTest {
        val vm = vm()
        vm.typeFilter.value = TxType.INCOME
        assertEquals(listOf(2L), vm.ids())
        vm.typeFilter.value = TxType.EXPENSE
        assertEquals(listOf(1L, 3L, 4L), vm.ids())
    }

    @Test fun `search matches note and category name ignoring case`() = runTest {
        val vm = vm()
        vm.query.value = "OBIAD"
        assertEquals(listOf(1L), vm.ids())
        vm.query.value = "transport"
        assertEquals(listOf(3L), vm.ids())
    }

    @Test fun `search and type filter are combined`() = runTest {
        val vm = vm()
        vm.query.value = "e"
        vm.typeFilter.value = TxType.INCOME
        assertEquals(listOf(2L), vm.ids())
    }

    @Test fun `blank query is ignored and missing category does not crash`() = runTest {
        val vm = vm()
        vm.query.value = "   "
        assertEquals(4, vm.ids().size)
        vm.query.value = "nieistniejące"
        assertEquals(emptyList<Long>(), vm.ids())
    }
}
