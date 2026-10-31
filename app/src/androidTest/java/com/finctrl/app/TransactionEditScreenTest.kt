package com.finctrl.app

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.TxType
import com.finctrl.app.ui.screens.TransactionEditScreen
import com.finctrl.app.ui.screens.TransactionEditViewModel
import com.finctrl.app.ui.screens.TxEditTags
import com.finctrl.app.ui.theme.FinctrlTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionEditScreenTest {
    @get:Rule val rule = createComposeRule()

    private val repo = FakeFinanceRepository(
        listOf(
            CategoryEntity(1, "Wynagrodzenie", TxType.INCOME),
            CategoryEntity(3, "Jedzenie", TxType.EXPENSE),
        )
    )
    @Volatile private var done = false

    private fun showNewTransactionForm() = rule.setContent {
        val vm = remember { TransactionEditViewModel(SavedStateHandle(mapOf("id" to -1L)), repo, ErrorHandler()) }
        FinctrlTheme { TransactionEditScreen(onDone = { done = true }, vm = vm) }
    }

    @Test fun invalidInput_showsFieldErrors_andDoesNotSave() {
        showNewTransactionForm()
        rule.onNodeWithTag(TxEditTags.AMOUNT).performTextInput("abc")
        rule.onNodeWithTag(TxEditTags.SAVE).performClick()

        rule.onNodeWithText("Podaj poprawną kwotę.").assertIsDisplayed()
        rule.onNodeWithText("Wybierz kategorię.").assertIsDisplayed()
        assertTrue(repo.transactions.value.isEmpty())
        assertFalse(done)
    }

    @Test fun validInput_savesInMinorUnits_andClosesScreen() {
        showNewTransactionForm()
        rule.onNodeWithTag(TxEditTags.AMOUNT).performTextInput("12,50")
        rule.onNodeWithText("Wybierz kategorię").performClick()
        rule.onNodeWithText("Jedzenie").performClick()
        rule.onNodeWithTag(TxEditTags.SAVE).performClick()

        rule.waitUntil(timeoutMillis = 5_000) { done }
        val saved = repo.transactions.value.single()
        assertEquals(1250L, saved.amount)
        assertEquals(3L, saved.categoryId)
        assertEquals(TxType.EXPENSE, saved.type)
    }

    @Test fun switchingToIncome_showsOnlyIncomeCategories() {
        showNewTransactionForm()
        rule.onNodeWithText("Przychód").performClick()
        rule.onNodeWithText("Wybierz kategorię").performClick()

        rule.onNodeWithText("Wynagrodzenie").assertIsDisplayed()
        rule.onNodeWithText("Jedzenie").assertDoesNotExist()
    }
}
