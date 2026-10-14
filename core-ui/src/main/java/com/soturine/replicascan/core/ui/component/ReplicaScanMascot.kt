package com.soturine.replicascan.core.ui.component

import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.soturine.replicascan.core.ui.R

/** Domain scenes; mascot motion never completes an OCR/export operation. */
enum class ReplicaScanMascotState {
    Welcome, Idle, Scanning, Processing, Working, Ocr, Empty, Attention, Success;

    internal val isBusy: Boolean
        get() = this == Processing || this == Working || this == Ocr || this == Scanning

    @get:DrawableRes
    internal val artwork: Int
        get() = when (this) {
            Welcome -> R.drawable.replicascan_fox_greeting
            Idle -> R.drawable.replicascan_fox_idle
            Scanning, Processing, Working -> R.drawable.replicascan_fox_working
            Ocr -> R.drawable.replicascan_fox_reading
            Empty -> R.drawable.replicascan_fox_empty
            Attention -> R.drawable.replicascan_fox_attention
            Success -> R.drawable.replicascan_fox_success
        }

    @get:DrawableRes
    internal val blinkArtwork: Int
        get() = when (this) {
            Welcome -> R.drawable.replicascan_fox_blink_greeting
            Idle -> R.drawable.replicascan_fox_blink_idle
            Scanning, Processing, Working -> R.drawable.replicascan_fox_blink_working
            Ocr -> R.drawable.replicascan_fox_blink_reading
            Empty -> R.drawable.replicascan_fox_blink_empty
            Attention -> R.drawable.replicascan_fox_blink_attention
            Success -> R.drawable.replicascan_fox_success
        }
}

/** Painted fox. Whole-pose motion preserves the approved artwork until true part layers exist. */
@Composable
fun ReplicaScanMascot(
    state: ReplicaScanMascotState,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    reducedMotion: Boolean = false,
) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val durationScale = systemAnimatorScale()
    val context = LocalContext.current
    // Poses are 1254 px square while the largest in-app mascot is 240 dp.
    // Decode at half size once per state; only the eyelid overlay uses a mask.
    // Original source files remain available for later layered art edits.
    val artwork = remember(context, state) { decodeMascotPose(context, state.artwork) }
    val blinkArtwork = remember(context, state) { decodeMascotPose(context, state.blinkArtwork) }
    val running = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !reducedMotion && durationScale > 0f
    // Compose owns these three independent clocks only while the screen is resumed. Reverse
    // breathing and scan motion have no snap/delay at the loop boundary. A stopped lifecycle,
    // explicit reduced motion or system animator scale 0 disposes the clocks entirely.
    val motion = if (running && state != ReplicaScanMascotState.Success) {
        val transition = rememberInfiniteTransition(label = "fox-${state.name}")
        val breath by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breathing",
        )
        val scan by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "scan-light",
        )
        val blink by transition.animateFloat(
            initialValue = 0f, targetValue = 0f,
            animationSpec = infiniteRepeatable(keyframes {
                durationMillis = 6100
                0f at 0
                0f at 2700
                1f at 2780
                1f at 2870
                0f at 2960
                0f at 6100
            }, RepeatMode.Restart),
            label = "blink",
        )
        MascotMotion(breath, scan, blink > .5f)
    } else {
        MascotMotion(0f, 0f, false)
    }
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Box(modifier.requiredSize(size).testTag("mascot-${state.name}"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawOval(
                shadowColor,
                topLeft = Offset(this.size.width * .19f, this.size.height * .90f),
                size = Size(this.size.width * .62f, this.size.height * .045f),
            )
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer {
            translationY = motion.breath * (if (state.isBusy) 2.dp.toPx() else 1.4.dp.toPx())
            scaleY = 1f + motion.breath * .006f
        }) {
            drawFoxPose(
                pose = artwork,
                blinkPose = blinkArtwork,
                state = state,
                blink = motion.blink,
            )
        }
        if (running && (state == ReplicaScanMascotState.Scanning ||
                    state == ReplicaScanMascotState.Processing ||
                    state == ReplicaScanMascotState.Working)) {
            Canvas(Modifier.matchParentSize()) {
                val y = this.size.height * (.65f + .13f * motion.scan)
                val start = Offset(this.size.width * .55f, y)
                val end = Offset(this.size.width * .73f, y)
                drawLine(Color(0xFF57B8B1).copy(alpha = .18f), start, end, strokeWidth = 5.dp.toPx())
                drawLine(Color(0xFF327B76).copy(alpha = .56f), start, end, strokeWidth = 1.5.dp.toPx())
            }
        }
    }
}

private data class MascotMotion(val breath: Float, val scan: Float, val blink: Boolean)

private fun decodeMascotPose(context: android.content.Context, @DrawableRes resource: Int): ImageBitmap {
    val options = BitmapFactory.Options().apply {
        inSampleSize = 2
        inScaled = false
    }
    return requireNotNull(BitmapFactory.decodeResource(context.resources, resource, options)) {
        "Mascot artwork missing: $resource"
    }.asImageBitmap()
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
