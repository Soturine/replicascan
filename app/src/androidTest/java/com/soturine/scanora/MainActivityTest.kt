package com.soturine.scanora

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
}
