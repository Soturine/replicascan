package com.soturine.scanora

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun exibeOnboardingOuHomeNaInicializacao() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Escaneie em poucos passos").assertExists()
    }

    @Test
    fun onboardingPermiteNavegarComGestoHorizontal() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Escaneie em poucos passos").assertExists()

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Revise antes de salvar").assertExists()
    }
}
