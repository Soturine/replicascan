package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.AutomaticOcrScriptPlanner
import com.soturine.replicascan.core.common.model.OcrScript
import org.junit.Test

class AutomaticOcrScriptPlannerTest {
    @Test
    fun `automatic uses one hinted model then Latin`() {
        assertThat(
            AutomaticOcrScriptPlanner.candidates(OcrScript.AUTOMATIC, OcrScript.JAPANESE),
        ).containsExactly(OcrScript.JAPANESE, OcrScript.LATIN).inOrder()
    }

    @Test
    fun `automatic never runs every recognizer`() {
        OcrScript.entries.forEach { hint ->
            assertThat(AutomaticOcrScriptPlanner.candidates(OcrScript.AUTOMATIC, hint).size)
                .isAtMost(2)
        }
    }

    @Test
    fun `manual mode stays explicit`() {
        assertThat(
            AutomaticOcrScriptPlanner.candidates(OcrScript.KOREAN, OcrScript.DEVANAGARI),
        ).containsExactly(OcrScript.KOREAN)
    }
}
