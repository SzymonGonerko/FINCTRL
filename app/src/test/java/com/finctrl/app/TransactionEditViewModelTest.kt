package com.finctrl.app

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.screens.TransactionEditViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEditViewModelTest {
    private val repo = mockk<FinanceRepository>(relaxed = true)

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.observeCategories() } returns flowOf(listOf(CategoryEntity(1, "Jedzenie", TxType.EXPENSE)))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = TransactionEditViewModel(SavedStateHandle(mapOf("id" to -1L)), repo, ErrorHandler())

    @Test fun `invalid amount sets field error and does not save`() = runTest {
        val vm = vm()
        vm.setAmount("abc"); vm.setCategory(1)
        vm.save()
        assertNotNull(vm.form.value.amountError)
        coVerify(exactly = 0) { repo.upsertTransaction(any()) }
    }

    @Test fun `valid form saves minor units and emits saved`() = runTest {
        val vm = vm()
        vm.setAmount("12,50"); vm.setCategory(1)
        vm.saved.test {
            vm.save()
            awaitItem()
        }
        coVerify { repo.upsertTransaction(match<TransactionEntity> { it.amount == 1250L && it.categoryId == 1L }) }
    }

    @Test fun `repository failure is reported as retryable message`() = runTest {
        coEvery { repo.upsertTransaction(any()) } throws java.net.UnknownHostException()
        val vm = vm()
        vm.setAmount("5"); vm.setCategory(1)
        vm.events.test {
            vm.save()
            val msg = awaitItem()
            assertNotNull(msg.retry)
            assertEquals(true, msg.text.contains("połączenia"))
        }
    }
}
