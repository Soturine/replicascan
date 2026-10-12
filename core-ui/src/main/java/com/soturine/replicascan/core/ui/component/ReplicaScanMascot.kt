package com.soturine.replicascan.core.ui.component

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.soturine.replicascan.core.ui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

/** Domain scenes; no animation can complete an OCR/export operation. */
enum class ReplicaScanMascotState {
    Welcome, Idle, Scanning, Processing, Working, Ocr, Empty, Attention, Success;

    internal val isBusy: Boolean
        get() = this == Processing || this == Working || this == Ocr || this == Scanning
}

/** Registered original art. Decorative; the owning screen conveys its state in text. */
@Composable
fun ReplicaScanMascot(
    state: ReplicaScanMascotState,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    reducedMotion: Boolean = false,
) {
    val atlas = ImageBitmap.imageResource(R.drawable.replicascan_fox_atlas)
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val durationScale = systemAnimatorScale()
    val phase = remember { Animatable(0f) }
    val running = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !reducedMotion && durationScale > 0f
    LaunchedEffect(state, running, durationScale) {
        phase.snapTo(0f)
        if (!running) return@LaunchedEffect
        do {
            phase.animateTo(1f, tween(if (state.isBusy) 3200 else 2400, easing = LinearEasing))
            if (!state.isBusy) break
            delay(320)
            phase.snapTo(0f)
        } while (isActive)
    }
    Canvas(modifier.size(size).testTag("mascot-${state.name}")) {
        val t = if (running) phase.value else 0f
        val wave = sin(t * PI * 2).toFloat()
        val breath = sin(t * PI).toFloat()
        // Physical artwork, like documents, is intentionally not mirrored in RTL.
        scale(this.size.minDimension / 240f, pivot = Offset.Zero) {
            drawOval(Color(0x14253B3A), Offset(44f, 221f), androidx.compose.ui.geometry.Size(150f, 9f))
            part(atlas, 4, 123f, 122f, 104f, 104f, wave * 5f, Offset(140f, 213f))
            part(atlas, 3, 45f, 118f - breath, 112f, 112f)
            val head = when {
                state == ReplicaScanMascotState.Attention -> 2
                t in 0.30f..0.35f || (state == ReplicaScanMascotState.Success && t > .55f) -> 1
                else -> 0
            }
            val tilt = when (state) {
                ReplicaScanMascotState.Ocr -> -5f + wave * 3f
                ReplicaScanMascotState.Attention -> -6f
                else -> wave * 2f
            }
            part(atlas, head, 23f, 8f - breath, 156f, 156f, tilt, Offset(105f, 137f))
            val hasPaper = state.isBusy || state == ReplicaScanMascotState.Success
            if (hasPaper) {
                val filing = if (state == ReplicaScanMascotState.Working) 10f * (1f - wave) else 0f
                part(atlas, 7, 83f, 132f + filing, 66f, 66f, -5f)
                if (state == ReplicaScanMascotState.Working || state == ReplicaScanMascotState.Success) {
                    part(atlas, 8, 72f, 165f, 91f, 67f)
                }
            }
            val greeting = state == ReplicaScanMascotState.Welcome || state == ReplicaScanMascotState.Success
            part(atlas, 5, 24f, 128f, 58f, 72f,
                if (greeting) 145f + wave * 14f else if (hasPaper) -20f + wave * 4f else wave * 3f,
                Offset(68f, 143f))
            part(atlas, 6, 121f, 130f, 58f, 72f,
                if (hasPaper) 18f - wave * 5f else -wave * 3f, Offset(137f, 142f))
            if (state == ReplicaScanMascotState.Scanning || state == ReplicaScanMascotState.Processing || state == ReplicaScanMascotState.Ocr) {
                drawLine(Color(0xFF326A65), Offset(97f, 149f + 26f * t), Offset(135f, 149f + 26f * t), 2f)
            }
            if (state == ReplicaScanMascotState.Success) {
                val ink = Color(0xFF326A65)
                drawCircle(Color(0xFFD8EDE5), 17f, Offset(192f, 93f))
                drawLine(ink, Offset(184f, 93f), Offset(190f, 99f), 3f)
                drawLine(ink, Offset(190f, 99f), Offset(200f, 86f), 3f)
            }
        }
    }
}

/** Source cells and pivots in a 240-unit stage; see docs/design/mascot.md. */
private fun DrawScope.part(
    atlas: ImageBitmap, cell: Int, x: Float, y: Float, width: Float, height: Float,
    angle: Float = 0f, pivot: Offset = Offset(x + width / 2, y + height / 2),
) {
    val edge = atlas.width / 3
    rotate(angle, pivot) {
        drawImage(atlas, IntOffset(cell % 3 * edge, cell / 3 * edge), IntSize(edge, edge),
            IntOffset(x.toInt(), y.toInt()), IntSize(width.toInt(), height.toInt()))
    }
}

@Composable
private fun systemAnimatorScale(): Float {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    var scale by remember(resolver) { mutableFloatStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scale = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale
}
