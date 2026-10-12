package com.soturine.replicascan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.replicascan.core.common.model.*
import com.soturine.replicascan.core.ui.component.*
import com.soturine.replicascan.core.ui.theme.ReplicaScanTheme
import com.soturine.replicascan.feature.editor.*
import com.soturine.replicascan.feature.export.*
import com.soturine.replicascan.feature.history.*
import com.soturine.replicascan.feature.home.*
import com.soturine.replicascan.feature.ocr.*
import com.soturine.replicascan.feature.settings.*
import com.soturine.replicascan.onboarding.OnboardingScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real Compose rendering with synthetic documents/injected screen states, not an end-to-end OCR claim. */
class RebrandVisualTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun fixture(): ScanDocument {
        val file = File(context.cacheDir, "visual-fixture.png")
        val bitmap = Bitmap.createBitmap(720, 960, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(253, 250, 242))
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(37, 59, 58); textSize = 38f }
        canvas.drawText("REPLICASCAN", 56f, 100f, ink)
        ink.textSize = 25f
        canvas.drawText("Synthetic document / Documento de teste", 56f, 160f, ink)
        repeat(12) { i -> canvas.drawText("${i + 1}. A clear page, kept on this device.", 56f, 230f + i * 47, ink) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return ScanDocument("visual", "Caderno de ideias", emptyList(), false, 0, 0,
            listOf(ScanPage("page", "visual", 0, file.absolutePath)), false)
    }

    private fun capture(name: String) {
        rule.mainClock.advanceTimeBy(900)
        rule.waitForIdle()
        val dir = File(context.getExternalFilesDir(null), "rebrand-evidence").apply { mkdirs() }
        val target = if (name == "language") rule.onNode(isDialog()) else rule.onRoot()
        target.captureToImage().asAndroidBitmap().let { bitmap ->
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun renderScreenMatrix() {
        val scan = fixture()
        var screen by mutableStateOf("onboarding")
        var dark by mutableStateOf(false)
        var large by mutableStateOf(false)
        var rtl by mutableStateOf(false)
        val editor = EditorUiState(true, scan, scan.pages.first(), previewImageUri = scan.pages.first().sourceUri)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, if (large) 2f else 1f),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                ReplicaScanTheme(darkTheme = dark) {
                    key(screen) {
                        when (screen) {
                            "onboarding" -> OnboardingScreen({})
                            "home-empty", "home-recent" -> HomeScreen(HomeUiState(false, if (screen == "home-recent") listOf(scan) else emptyList()),
                                false, {}, {}, {}, {}, { null }, {}, {}, {})
                            "review" -> ReviewScreen(editor, {}, {}, { _, _ -> }, {}, { true }, {}, {}, {}, {}, {}, {}, {}, {})
                            "crop" -> CropScreen(editor, {}, {}, {})
                            "filters" -> FilterScreen(editor, { _, _ -> }, {}, {}, {})
                            "ocr-loading", "ocr-ready", "ocr-empty" -> OcrScreen(OcrUiState(isLoaded = true, page = scan.pages.first(),
                                isRecognizing = screen == "ocr-loading", text = if (screen == "ocr-ready") "Documento de teste.\n\nTexto legível para selecionar e copiar." else ""), {}, {}, {}, {})
                            "export", "export-jpg", "export-png", "export-progress", "export-error", "export-success", "export-library-warning" -> ExportScreen(ExportUiState(scan = scan, selectedFormat = when(screen) { "export-jpg" -> ExportFormat.JPG; "export-png" -> ExportFormat.PNG; else -> ExportFormat.PDF },
                                isExporting = screen == "export-progress", message = when(screen) { "export-error" -> ExportMessage.WRITE_FAILED; "export-library-warning" -> ExportMessage.LIBRARY_UPDATE_FAILED; else -> null },
                                exportedFiles = if (screen == "export-success" || screen == "export-library-warning") listOf(ExportedFile("Caderno de ideias.pdf", "content://fixture", "application/pdf", 1024)) else emptyList()),
                                {}, {}, {}, {}, {}, {}, {}, {})
                            "history", "search-empty" -> HistoryScreen(HistoryUiState(true, if (screen == "search-empty") "xyz" else "", if (screen == "history") listOf(scan) else emptyList()), {}, {}, {})
                            "detail" -> ScanDetailScreen(scan, true, {}, {}, {}, {}, {}, {})
                            "settings" -> SettingsScreen(SettingsUiState(), "", {}, {}, {}, {}, {}, {}, {})
                            "about" -> AboutScreen("candidate", {}, {})
                        }
                    }
                }
            }
        }
        val screens = listOf("onboarding", "home-empty", "home-recent", "review", "crop", "filters", "ocr-loading", "ocr-ready", "ocr-empty",
            "export", "export-jpg", "export-png", "export-progress", "export-error", "export-success", "export-library-warning", "history", "search-empty", "detail", "settings", "about")
        for (name in screens) {
            rule.runOnIdle { screen = name }
            // URI decoding runs on IO; allow it to complete before the capture.
            rule.mainClock.advanceTimeBy(32)
            Thread.sleep(600)
            capture(name)
            if (name == "onboarding") {
                rule.onNodeWithText(context.getString(R.string.onboarding_next)).performClick()
                rule.mainClock.advanceTimeBy(1200)
                capture("onboarding-two")
                rule.onNodeWithText(context.getString(R.string.onboarding_next)).performClick()
                rule.mainClock.advanceTimeBy(1200)
                capture("onboarding-three")
            }
        }
        rule.runOnIdle { screen = "settings" }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText(context.getString(com.soturine.replicascan.feature.settings.R.string.settings_language)).performClick()
        capture("language")
        rule.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        rule.runOnIdle { screen = "home-recent"; dark = true }
        capture("home-dark")
        rule.runOnIdle { large = true }
        capture("home-dark-200")
        rule.runOnIdle { screen = "export"; large = true }
        capture("export-dark-200")
        rule.runOnIdle { screen = "review"; rtl = true }
        capture("review-dark-200-rtl")
    }

    @Test fun sequentialCornerDragsPreserveTheOtherCorners() {
        var quad by mutableStateOf(DocumentQuad.FULL_PAGE)
        rule.setContent {
            ReplicaScanTheme {
                Box(Modifier.size(300.dp)) {
                    val px = LocalDensity.current.density
                    QuadEditorOverlay(quad, Rect(40 * px, 40 * px, 260 * px, 260 * px), { quad = it })
                }
            }
        }
        val first = context.getString(com.soturine.replicascan.feature.editor.R.string.editor_corner_top_left)
        val second = context.getString(com.soturine.replicascan.feature.editor.R.string.editor_corner_bottom_right)
        rule.onNodeWithContentDescription(first).performTouchInput { swipe(center, center + Offset(45f, 55f), 300) }
        val afterFirst = quad.topLeft
        assertTrue(afterFirst.x > 0f && afterFirst.y > 0f)
        rule.onNodeWithContentDescription(second).performTouchInput { swipe(center, center - Offset(55f, 45f), 300) }
        assertEquals(afterFirst, quad.topLeft)
        assertTrue(quad.bottomRight.x < 1f && quad.bottomRight.y < 1f)
    }

    @Test fun launcherMasksAndSizes() {
        rule.setContent {
            ReplicaScanTheme {
                androidx.compose.material3.Surface {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        for (rounded in listOf(false, true)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                for (size in listOf(48, 72, 108)) {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        modifier = Modifier.size(size.dp),
                                        factory = { ctx -> object : android.view.View(ctx) {
                                            override fun onDraw(canvas: Canvas) {
                                                val path = android.graphics.Path()
                                                val radius = width * if (rounded) .22f else .5f
                                                path.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, android.graphics.Path.Direction.CW)
                                                canvas.save()
                                                canvas.clipPath(path)
                                                for (id in listOf(R.drawable.ic_replicascan_badge, R.drawable.ic_launcher_foreground)) {
                                                    ctx.getDrawable(id)!!.apply { setBounds(0, 0, width, height); draw(canvas) }
                                                }
                                                canvas.restore()
                                            }
                                        } },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        capture("launcher-masks")
    }

    @Test fun holdingCompareRestoresSelectedFilterOnRelease() {
        val scan = fixture()
        var requested: DocumentFilterType? = null
        val selectedPage = scan.pages.first().copy(filterType = DocumentFilterType.GRAYSCALE)
        rule.setContent { ReplicaScanTheme {
            FilterScreen(EditorUiState(true, scan, selectedPage), { filter, _ -> requested = filter }, {}, {}, {})
        } }
        rule.waitForIdle()
        assertEquals(DocumentFilterType.GRAYSCALE, requested)
        rule.onNodeWithTag("filter-compare").performTouchInput { down(center) }
        rule.waitForIdle()
        assertEquals(DocumentFilterType.ORIGINAL, requested)
        rule.onNodeWithTag("filter-compare").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(DocumentFilterType.GRAYSCALE, requested)
    }

    @Test fun motionFramesAndReducedMotionAreDifferentContracts() {
        var reduced by mutableStateOf(false)
        var scene by mutableStateOf(ReplicaScanMascotState.Working)
        rule.mainClock.autoAdvance = false
        rule.setContent { ReplicaScanTheme { ReplicaScanMascot(scene, size = 240.dp, reducedMotion = reduced) } }
        rule.mainClock.advanceTimeBy(100)
        val first = rule.onNodeWithTag("mascot-Working").captureToImage().asAndroidBitmap()
        rule.mainClock.advanceTimeBy(750)
        val second = rule.onNodeWithTag("mascot-Working").captureToImage().asAndroidBitmap()
        assertFalse("Working pose must move", first.sameAs(second))
        val dir = File(context.getExternalFilesDir(null), "rebrand-evidence").apply { mkdirs() }
        listOf(first, second).forEachIndexed { index, bitmap ->
            File(dir, "motion-frame-$index.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        capture("mascot-working-frame")
        rule.runOnIdle { reduced = true }
        rule.mainClock.advanceTimeBy(50)
        val still = rule.onNodeWithTag("mascot-Working").captureToImage().asAndroidBitmap()
        rule.mainClock.advanceTimeBy(1000)
        assertTrue("Reduced motion must be stable", still.sameAs(rule.onNodeWithTag("mascot-Working").captureToImage().asAndroidBitmap()))
        rule.runOnIdle { scene = ReplicaScanMascotState.Attention }
        capture("mascot-error-static")
        rule.runOnIdle { scene = ReplicaScanMascotState.Success }
        capture("mascot-success-static")
        rule.runOnIdle { reduced = false }
        var frame = 0
        listOf(ReplicaScanMascotState.Idle, ReplicaScanMascotState.Working,
            ReplicaScanMascotState.Success, ReplicaScanMascotState.Attention).forEach { pose ->
            rule.runOnIdle { scene = pose }
            repeat(16) {
                rule.mainClock.advanceTimeBy(125)
                val bitmap = rule.onNodeWithTag("mascot-${pose.name}").captureToImage().asAndroidBitmap()
                File(dir, "motion-sequence-${frame++.toString().padStart(2, '0')}.png")
                    .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
