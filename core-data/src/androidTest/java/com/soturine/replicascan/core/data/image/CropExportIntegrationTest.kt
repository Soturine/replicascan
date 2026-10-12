package com.soturine.replicascan.core.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.replicascan.core.common.model.*
import com.soturine.replicascan.core.data.export.DefaultExportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Exercises the production decoder, perspective warp, rotation and PNG writer, without a processing mock. */
class CropExportIntegrationTest {
    @Test fun approvedCropChangesTheExportedPixelsAndPreservesTheSource() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "crop-export-${System.nanoTime()}.png")
        val image = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        image.eraseColor(Color.RED)
        Canvas(image).drawRect(200f, 0f, 400f, 300f, Paint().apply { color = Color.BLUE })
        source.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        val original = source.readBytes()
        val geometry = DocumentQuad(PointValue(0f, 0f), PointValue(.5f, 0f), PointValue(.5f, 1f), PointValue(0f, 1f))
        val page = ScanPage("page", "scan", 0, source.absolutePath, quad = geometry, rotationDegrees = 90)
        val scan = ScanDocument("scan", "Crop integration fixture", emptyList(), false, 0, 0, listOf(page), true)
        val processing = DefaultDocumentProcessingRepository(context)
        val exports = mutableListOf<ExportedFile>()
        try {
            val reset = processing.renderBitmap(source.absolutePath, DocumentFilterType.ORIGINAL, DocumentQuad.FULL_PAGE, 0, 1000)
            assertEquals(400, reset.width)
            assertEquals(Color.BLUE, reset.getPixel(350, 150))
            reset.recycle()
            exports += DefaultExportRepository(context, processing).exportImages(scan, ExportFormat.PNG)
            val actual = context.contentResolver.openInputStream(Uri.parse(exports.single().uri))!!.use { BitmapFactory.decodeStream(it) }!!
            assertTrue(actual.width in 299..301)
            assertTrue(actual.height in 199..201)
            assertEquals(Color.RED, actual.getPixel(actual.width / 2, actual.height / 2))
            actual.recycle()
            assertArrayEquals(original, source.readBytes())
        } finally {
            exports.forEach { context.contentResolver.delete(Uri.parse(it.uri), null, null) }
            source.delete()
        }
    }
}
