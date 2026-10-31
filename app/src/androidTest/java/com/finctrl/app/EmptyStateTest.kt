package com.finctrl.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finctrl.app.ui.common.EmptyState
import org.junit.Rule
import org.junit.Test

class EmptyStateTest {
    @get:Rule val rule = createComposeRule()

    @Test fun showsTitleAndHint() {
        rule.setContent { EmptyState("Brak transakcji", "Dodaj pierwszą") }
        rule.onNodeWithText("Brak transakcji").assertIsDisplayed()
        rule.onNodeWithText("Dodaj pierwszą").assertIsDisplayed()
    }
}
