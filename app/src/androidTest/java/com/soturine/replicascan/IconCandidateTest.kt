package com.soturine.replicascan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.soturine.replicascan.core.ui.theme.ReplicaScanTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Visual qualification of unapproved foreground candidates; only the first mask is the device's real mask. */
class IconCandidateTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun candidateMasksAndSizes() {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val images = (1..3).map { index ->
            testContext.assets.open("icon-candidates/candidate-$index.png").use(BitmapFactory::decodeStream)
        }
        images.forEach { assertEquals(it.width, it.height) }
        val chosen = mutableIntStateOf(0)
        rule.setContent {
            ReplicaScanTheme {
                Surface {
                    key(chosen.intValue) { Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        Text("Icon candidate ${chosen.intValue + 1}", style = MaterialTheme.typography.headlineMedium)
                        Text("Android system mask · 24 / 48 / 72 / 108 dp")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf(24, 48, 72, 108).forEach { size ->
                                CandidateIcon(images[chosen.intValue], Mask.System, size)
                            }
                        }
                        Text("Circle · squircle · teardrop (mask previews)")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf(Mask.Circle, Mask.Squircle, Mask.Teardrop).forEach { mask ->
                                CandidateIcon(images[chosen.intValue], mask, 72)
                            }
                        }
                        Text("Themed icon draft · 48 / 72 dp")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf(48, 72).forEach { size ->
                                AndroidView(
                                    modifier = Modifier.size(size.dp),
                                    factory = { ctx -> object : View(ctx) {
                                        override fun onDraw(canvas: Canvas) {
                                            val mono = testContext.getDrawable(com.soturine.replicascan.test.R.drawable.ic_candidate_monochrome)!!
                                                .mutate().apply { setTint(android.graphics.Color.rgb(32, 66, 64)) }
                                            AdaptiveIconDrawable(ColorDrawable(0xFFDDEBE4.toInt()), mono).apply {
                                                setBounds(0, 0, width, height)
                                                draw(canvas)
                                            }
                                        }
                                    } },
                                )
                            }
                        }
                    } }
                }
            }
        }
        images.indices.forEach { index ->
            chosen.intValue = index
            rule.waitForIdle()
            val directory = File(targetContext.getExternalFilesDir(null), "rebrand-evidence").apply { mkdirs() }
            rule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(directory, "icon-candidate-${index + 1}-android.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        images.forEach(Bitmap::recycle)
    }

    private enum class Mask { System, Circle, Squircle, Teardrop }

    @Composable
    private fun CandidateIcon(bitmap: Bitmap, mask: Mask, size: Int) {
        AndroidView(
            modifier = Modifier.size(size.dp),
            factory = { ctx -> object : View(ctx) {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                override fun onDraw(canvas: Canvas) {
                    if (mask == Mask.System) {
                        AdaptiveIconDrawable(
                            ColorDrawable(0xFFFAF6EF.toInt()),
                            InsetDrawable(BitmapDrawable(resources, bitmap), (width * .22f).toInt()),
                        ).apply { setBounds(0, 0, width, height); draw(canvas) }
                        return
                    }
                    val w = width.toFloat()
                    val h = height.toFloat()
                    val path = Path()
                    when (mask) {
                        Mask.Circle -> path.addCircle(w / 2, h / 2, w / 2, Path.Direction.CW)
                        Mask.Squircle -> path.addRoundRect(RectF(0f, 0f, w, h), w * .23f, h * .23f, Path.Direction.CW)
                        Mask.Teardrop -> {
                            path.moveTo(w * .5f, 0f)
                            path.cubicTo(w * .78f, 0f, w, h * .26f, w, h * .53f)
                            path.cubicTo(w, h * .81f, w * .8f, h, w * .5f, h)
                            path.cubicTo(w * .2f, h, 0f, h * .81f, 0f, h * .53f)
                            path.cubicTo(0f, h * .26f, w * .22f, 0f, w * .5f, 0f)
                            path.close()
                        }
                        Mask.System -> Unit
                    }
                    canvas.save()
                    canvas.clipPath(path)
                    canvas.drawColor(0xFFFAF6EF.toInt())
                    // AdaptiveIconDrawable magnifies the foreground before clipping it.
                    canvas.drawBitmap(bitmap, null, RectF(w * .08f, h * .08f, w * .92f, h * .92f), paint)
                    canvas.restore()
                }
            } },
        )
    }
}
