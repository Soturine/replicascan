package com.soturine.replicascan.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.ui.R

enum class ReplicaScanMascotState(@param:DrawableRes internal val drawable: Int) {
    Welcome(R.drawable.replicascan_mascot_welcome),
    Processing(R.drawable.replicascan_mascot_processing),
    Working(R.drawable.replicascan_mascot_working),
    Ocr(R.drawable.replicascan_mascot_ocr),
    Empty(R.drawable.replicascan_mascot_empty),
    Attention(R.drawable.replicascan_mascot_attention),
    Success(R.drawable.replicascan_mascot_success),
    ;

    internal val isBusy: Boolean
        get() = this == Processing || this == Working || this == Ocr
}

/** Decorative only: every screen conveys its state in text, so the mascot is hidden from TalkBack. */
@Composable
fun ReplicaScanMascot(
    state: ReplicaScanMascotState,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
) {
    val motion = if (state.isBusy) {
        // Gentle bob only while work is in progress; static poses never run an infinite animation.
        val phase by rememberInfiniteTransition(label = "mascot").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "mascotPhase",
        )
        Modifier.graphicsLayer {
            translationY = (-3 + phase * 6).dp.toPx()
            rotationZ = if (state == ReplicaScanMascotState.Ocr) -0.8f + phase * 1.6f else 0f
        }
    } else {
        Modifier
    }
    Image(
        painter = painterResource(state.drawable),
        contentDescription = null,
        modifier = modifier.then(motion).size(size),
    )
}
