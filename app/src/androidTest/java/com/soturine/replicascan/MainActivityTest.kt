package com.soturine.replicascan

import androidx.core.content.FileProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsOnboardingOnFirstLaunch() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_page_one_title)).assertExists()
    }

    @Test
    fun onboardingAdvancesWithHorizontalSwipe() {
        // Assertions read the app's own resources so the test passes on any device locale.
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_page_one_title)).assertExists()

        composeRule.onRoot().performTouchInput { if (composeRule.activity.isRtl()) swipeRight() else swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_page_two_title)).assertExists()
    }

    private fun android.app.Activity.isRtl(): Boolean =
        resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL

    @Test
    fun packageAndProviderUseReplicaScanIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = "${context.packageName}.fileprovider"

        assertEquals("com.soturine.replicascan", context.packageName)
        val provider = context.packageManager.resolveContentProvider(authority, 0)
        assertEquals(ReplicaScanFileProvider::class.java.name, provider?.name)
        assertEquals(false, provider?.exported)
        assertEquals(true, provider?.grantUriPermissions)
    }

    @Test
    fun providerSharesOnlyConfiguredExportDirectories() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = "${context.packageName}.fileprovider"
        val sharedExport = File(context.cacheDir, "shared-exports/provider-probe.pdf").apply {
            parentFile?.mkdirs()
            writeText("probe")
        }
        val privateSource = File(context.filesDir, "scan-sources/private-probe.jpg").apply {
            parentFile?.mkdirs()
            writeText("private")
        }

        try {
            val uri = FileProvider.getUriForFile(context, authority, sharedExport)
            assertEquals(authority, uri.authority)
            try {
                FileProvider.getUriForFile(context, authority, privateSource)
                fail("Private scan sources must not be exposed by FileProvider")
            } catch (_: IllegalArgumentException) {
                assertTrue(privateSource.exists())
            }
        } finally {
            sharedExport.delete()
            privateSource.delete()
        }
    }
}
