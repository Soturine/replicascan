package com.soturine.replicascan.core.ui.component

import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.soturine.replicascan.core.ui.R

/** Domain scenes never complete an OCR or export operation. */
enum class ReplicaScanMascotState {
    Welcome, Idle, Scanning, Processing, Working, Ocr, Empty, Attention, Success;

    internal val isBusy: Boolean
        get() = this == Processing || this == Working || this == Ocr || this == Scanning
}

/** Compose-native animation of independently authored anatomical paintings. */
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
    val layers = remember(context) {
        FoxLayers(
            tail = decodeLayer(context, R.drawable.fox_rig_tail),
            body = decodeLayer(context, R.drawable.fox_rig_body),
            head = decodeLayer(context, R.drawable.fox_rig_head),
            scarf = decodeLayer(context, R.drawable.fox_rig_scarf),
            arm = decodeLayer(context, R.drawable.fox_rig_arm),
            wave = decodeLayer(context, R.drawable.fox_rig_wave),
            paper = decodeLayer(context, R.drawable.fox_rig_paper),
        )
    }
    val running = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !reducedMotion && durationScale > 0f
    val gesture = remember(state) { Animatable(0f) }
    LaunchedEffect(state, running) {
        if (running && (state == ReplicaScanMascotState.Welcome ||
                    state == ReplicaScanMascotState.Empty ||
                    state == ReplicaScanMascotState.Attention ||
                    state == ReplicaScanMascotState.Success)) {
            gesture.snapTo(0f)
            gesture.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            gesture.animateTo(0f, tween(750, easing = FastOutSlowInEasing))
        }
    }

    val motion = if (running) {
        val transition = rememberInfiniteTransition(label = "fox-rig")
        val tail by transition.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "tail",
        )
        val head by transition.animateFloat(
            initialValue = -.6f, targetValue = .6f,
            animationSpec = infiniteRepeatable(tween(3700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "head",
        )
        val paws by transition.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "paws-paper",
        )
        val breath by transition.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breathing",
        )
        val celebration = if (state == ReplicaScanMascotState.Success) gesture.value else 0f
        FoxMotion(
            tail = tail + celebration * .35f,
            head = head + celebration * .6f,
            paws = paws,
            gesture = gesture.value,
            breath = breath,
        )
    } else FoxMotion()

    val shadow = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)
    Box(modifier.requiredSize(size).testTag("mascot-${state.name}"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawOval(shadow, topLeft = Offset(this.size.width * .19f, this.size.height * .90f),
                size = Size(this.size.width * .62f, this.size.height * .045f))
            drawFoxRig(layers, state, motion)
            if (running && (state == ReplicaScanMascotState.Scanning ||
                        state == ReplicaScanMascotState.Processing ||
                        state == ReplicaScanMascotState.Working)) {
                val y = this.size.height * (.66f + .10f * ((motion.paws + 1f) / 2f))
                drawLine(Color(0xFF57B8B1).copy(alpha = .18f),
                    Offset(this.size.width * .42f, y), Offset(this.size.width * .60f, y),
                    strokeWidth = 5.dp.toPx())
                drawLine(Color(0xFF327B76).copy(alpha = .56f),
                    Offset(this.size.width * .42f, y), Offset(this.size.width * .60f, y),
                    strokeWidth = 1.5.dp.toPx())
            }
        }
    }
}

private fun decodeLayer(context: android.content.Context, @DrawableRes resource: Int): ImageBitmap {
    val options = BitmapFactory.Options().apply { inSampleSize = 2; inScaled = false }
    return requireNotNull(BitmapFactory.decodeResource(context.resources, resource, options)) {
        "Mascot layer missing: $resource"
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
