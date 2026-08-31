package com.soturine.replicascan.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.ui.R

enum class ReplicaScanMascotState(@DrawableRes internal val drawable: Int) {
    Welcome(R.drawable.replicascan_mascot_welcome),
    Processing(R.drawable.replicascan_mascot_processing),
    Working(R.drawable.replicascan_mascot_working),
    Ocr(R.drawable.replicascan_mascot_ocr),
    Empty(R.drawable.replicascan_mascot_empty),
    Attention(R.drawable.replicascan_mascot_attention),
    Success(R.drawable.replicascan_mascot_success),
}

@Composable
fun ReplicaScanMascot(
    state: ReplicaScanMascotState,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    showLabel: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotPhase",
    )
    val animatedModifier = when (state) {
        ReplicaScanMascotState.Processing,
        ReplicaScanMascotState.Working,
        ReplicaScanMascotState.Ocr,
        -> Modifier
            .graphicsLayer {
                translationY = (-3 + phase * 6).dp.toPx()
                rotationZ = if (state == ReplicaScanMascotState.Ocr) -0.8f + phase * 1.6f else 0f
            }
            .scale(0.98f + phase * 0.02f)
        ReplicaScanMascotState.Attention -> Modifier.scale(0.985f + phase * 0.015f)
        else -> Modifier
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(state.drawable),
            contentDescription = null,
            modifier = animatedModifier.size(size),
        )
        if (showLabel) {
            Text(
                text = stringResource(if (state == ReplicaScanMascotState.Success) R.string.mascot_success else R.string.mascot_processing),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
