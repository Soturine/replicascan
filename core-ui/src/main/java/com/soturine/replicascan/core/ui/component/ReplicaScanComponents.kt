package com.soturine.replicascan.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soturine.replicascan.core.ui.theme.ReplicaScanSizes
import com.soturine.replicascan.core.ui.theme.ReplicaScanSpacing

@Composable
fun ReplicaScanContent(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = if (expanded) ReplicaScanSizes.expandedContentMaxWidth else ReplicaScanSizes.compactContentMaxWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.lg),
            content = content,
        )
    }
}

@Composable
fun ReplicaScanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .985f else 1f, tween(120), label = "actionPress")
    Button(
        interactionSource = interactions,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .heightIn(min = ReplicaScanSizes.primaryActionHeight),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        )
    }
}

@Composable
fun ReplicaScanSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val buttonModifier = modifier
        .heightIn(min = 52.dp)
        .sizeIn(minWidth = ReplicaScanSizes.minimumTouchTarget)
    if (outlined) {
        OutlinedButton(modifier = buttonModifier, enabled = enabled, onClick = onClick) {
            icon?.invoke()
            Text(text = text)
        }
    } else {
        FilledTonalButton(modifier = buttonModifier, enabled = enabled, onClick = onClick) {
            icon?.invoke()
            Text(text = text)
        }
    }
}
