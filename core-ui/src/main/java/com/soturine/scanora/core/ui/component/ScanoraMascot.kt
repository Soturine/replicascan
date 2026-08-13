package com.soturine.scanora.core.ui.component

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
import com.soturine.scanora.core.ui.R

enum class ScanoraMascotState(@DrawableRes internal val drawable: Int) {
    Welcome(R.drawable.scanora_mascot_welcome),
    Processing(R.drawable.scanora_mascot_processing),
    Working(R.drawable.scanora_mascot_working),
    Success(R.drawable.scanora_mascot_success),
}

@Composable
fun ScanoraMascot(
    state: ScanoraMascotState,
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
    val animatedModifier = if (state == ScanoraMascotState.Processing || state == ScanoraMascotState.Working) {
        Modifier.graphicsLayer { translationY = (-3 + phase * 6).dp.toPx() }.scale(0.98f + phase * 0.02f)
    } else Modifier

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(state.drawable),
            contentDescription = null,
            modifier = animatedModifier.size(size),
        )
        if (showLabel) {
            Text(
                text = stringResource(if (state == ScanoraMascotState.Success) R.string.mascot_success else R.string.mascot_processing),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
