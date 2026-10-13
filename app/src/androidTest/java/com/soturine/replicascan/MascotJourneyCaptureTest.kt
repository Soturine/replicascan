package com.soturine.replicascan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.ui.theme.ReplicaScanTheme
import com.soturine.replicascan.feature.export.ExportMessage
import com.soturine.replicascan.feature.export.ExportScreen
import com.soturine.replicascan.feature.export.ExportUiState
import com.soturine.replicascan.feature.home.HomeScreen
import com.soturine.replicascan.feature.home.HomeUiState
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Manual adb screenrecord target: native Compose screens with injected states and synthetic paper. */
class MascotJourneyCaptureTest {
    @get:Rule val rule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test fun recordContinuousHomeAndExportJourney() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "motion-fixture.png")
        val bitmap = Bitmap.createBitmap(720, 960, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(253, 250, 242))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(37, 59, 58)
            textSize = 36f
        }
        canvas.drawText("REPLICASCAN", 48f, 100f, paint)
        paint.textSize = 25f
        repeat(12) { index -> canvas.drawText("${index + 1}. A page kept on this device.", 48f, 180f + index * 48, paint) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val recordedAt = System.currentTimeMillis()
        val scan = ScanDocument("motion", "Caderno de ideias", emptyList(), false, recordedAt, recordedAt,
            listOf(ScanPage("motion-page", "motion", 0, file.absolutePath)), false)
        val ready = File(context.getExternalFilesDir(null), "motion-capture-ready")
        ready.delete()
        val scene = mutableStateOf("home")
        rule.scenario.onActivity { activity -> activity.setContent {
            ReplicaScanTheme {
                when (scene.value) {
                    "home" -> HomeScreen(HomeUiState(false, listOf(scan)), false,
                        {}, {}, {}, {}, { null }, {}, {}, {})
                    else -> ExportScreen(
                        state = ExportUiState(
                            scan = scan,
                            isExporting = scene.value == "working",
                            message = if (scene.value == "error") ExportMessage.WRITE_FAILED else null,
                            exportedFiles = if (scene.value == "success") {
                                listOf(ExportedFile("Caderno de ideias.pdf", "content://fixture", "application/pdf", 1024))
                            } else emptyList(),
                        ),
                        onSelectFormat = {}, onSelectQuality = {}, onSelectPageSize = {},
                        onExport = {}, onShare = {}, onOpenFile = {}, onBack = {}, onClearMessage = {},
                    )
                }
            }
        } }
        Thread.sleep(500)
        ready.writeText("home")
        Thread.sleep(8000)
        for (next in listOf("options", "working", "success", "error")) {
            rule.scenario.onActivity { scene.value = next }
            Thread.sleep(4500)
        }
        ready.delete()
    }
}
