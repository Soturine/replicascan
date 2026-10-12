package com.soturine.replicascan.core.ui.component

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.soturine.replicascan.core.ui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

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
}

/** Original-fox pose study. Decorative; the owning screen conveys its state in text. */
@Composable
fun ReplicaScanMascot(
    state: ReplicaScanMascotState,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    reducedMotion: Boolean = false,
) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val durationScale = systemAnimatorScale()
    val density = LocalDensity.current.density
    val phase = remember { Animatable(0f) }
    val running = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !reducedMotion && durationScale > 0f

    LaunchedEffect(state, running, durationScale) {
        phase.snapTo(0f)
        if (!running) return@LaunchedEffect
        do {
            phase.animateTo(1f, tween(if (state.isBusy) 2700 else 3200, easing = LinearEasing))
            if (state == ReplicaScanMascotState.Success) break
            delay(if (state.isBusy) 420 else 1200)
            phase.snapTo(0f)
        } while (isActive)
    }

    val t = if (running) phase.value else 0f
    val wave = sin(t * 2 * PI).toFloat()
    val breath = ((1 - kotlin.math.cos(t * 2 * PI)) / 2).toFloat()
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Box(modifier.requiredSize(size).testTag("mascot-${state.name}"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawOval(
                shadowColor,
                topLeft = Offset(this.size.width * .19f, this.size.height * .90f),
                size = Size(this.size.width * .62f, this.size.height * .045f),
            )
        }
        Image(
            painter = painterResource(state.artwork),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                // Motion stays within the illustration's transparent margins.
                translationY = -wave * (if (state.isBusy) 1.6f else 1.1f) * density
                rotationZ = wave * when (state) {
                    ReplicaScanMascotState.Welcome, ReplicaScanMascotState.Empty -> 1.1f
                    ReplicaScanMascotState.Attention -> .35f
                    else -> .65f
                }
                scaleX = 1f + breath * .005f
                scaleY = 1f + breath * .008f
            },
        )
        if (running && (state == ReplicaScanMascotState.Scanning ||
                    state == ReplicaScanMascotState.Processing ||
                    state == ReplicaScanMascotState.Working)) {
            Canvas(Modifier.matchParentSize()) {
                val y = this.size.height * (.65f + .13f * t)
                val start = Offset(this.size.width * .55f, y)
                val end = Offset(this.size.width * .73f, y)
                drawLine(Color(0xFF57B8B1).copy(alpha = .18f), start, end, strokeWidth = 5.dp.toPx())
                drawLine(Color(0xFF327B76).copy(alpha = .56f), start, end, strokeWidth = 1.5.dp.toPx())
            }
        }
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
